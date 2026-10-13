# Librio — Database Design

## 1. Purpose

This document defines the target persistence model for Librio V2.

It translates the concepts in `domain.md` into:

- tables;
- columns;
- relationships;
- constraints;
- indexes;
- migration strategy;
- data-seeding lifecycle.

The database should protect important invariants without turning V2 into a generic platform for hypothetical V3+ requirements.

---

# 2. Database Platform

V2 continues to use:

```text
PostgreSQL
```

PostgreSQL is the authoritative database platform for deployed environments and database-critical verification.

H2 may remain useful for fast tests but is not treated as a complete behavioral replacement for PostgreSQL.

---

# 3. Target V2 ERD

```text
ACCOUNT
  |
  |             RESOURCE
  |            /       \
  |           v         v
  |    PHYSICAL_ITEM  DIGITAL_ITEM
  |          |
  +----< BORROW_REQUEST
  |          |
  |          v
  +------< BORROWING


ACCOUNT
  |
  +----< ACCESS_SUBSCRIPTION >---- ACCESS_PLAN
  |              |
  |              v
  +----< PAYMENT_TRANSACTION
```

Main persistence areas are:

- Catalog;
- Physical Inventory;
- Circulation;
- Identity;
- Digital Content;
- Future Paid Access.

---

# 4. `accounts`

Target:

```text
accounts
--------
id
email
password_hash
display_name
role
account_status
reader_type
created_at
updated_at
```

`role` remains:

```text
READER
LIBRARIAN
```

`reader_type` is introduced as:

```text
INSTITUTIONAL
EXTERNAL
```

For V2:

```text
INSTITUTIONAL
→ may use institutional physical circulation

EXTERNAL
→ no default physical borrowing
```

The V1 field:

```text
membership_eligible
```

is removed because paid-membership eligibility is no longer an Account responsibility.

---

# 5. `resource`

Target:

```text
resource
--------
id
title
authors
description
category
isbn
cover_image_url
metadata_source
external_source_id
resource_status
```

Authors remain stored simply in V2.

ISBN remains unique under the current canonical-ISBN policy.

Metadata provenance remains:

```text
MANUAL
GOOGLE_BOOKS
```

V2 introduces:

```text
resource_status
---------------
ACTIVE
ARCHIVED
```

to preserve circulation history without relying on hard deletion.

---

# 6. `physical_item`

Target:

```text
physical_item
-------------
id
resource_id
barcode
location
inventory_status
circulation_status
```

A Resource may own multiple PhysicalItems.

`barcode` remains unique.

Inventory states remain:

```text
ACTIVE
LOST
DAMAGED
WITHDRAWN
```

Circulation states remain:

```text
AVAILABLE
RESERVED
BORROWED
```

V2 intentionally continues to persist `circulation_status` as operational state.

It must remain consistent with active request and borrowing records.

---

# 7. `borrow_request`

Target:

```text
borrow_request
--------------
id
reader_id
resource_id
physical_item_id
status
requested_at
status_updated_at
expires_at
prepared_at
prepared_by
rejected_at
rejected_by
rejection_reason
fulfilled_at
fulfilled_by
```

Both `resource_id` and `physical_item_id` are retained.

They represent different facts:

```text
resource_id
→ what the reader requested

physical_item_id
→ the exact copy allocated by the system
```

Current request states remain:

```text
REQUESTED
READY_FOR_PICKUP
FULFILLED
CANCELLED
REJECTED
EXPIRED
```

Generic `created_at` and `updated_at` fields are candidates for removal because `requested_at` and `status_updated_at` already carry the relevant lifecycle meaning.

---

# 8. `borrowing`

Target:

```text
borrowing
---------
id
physical_item_id
reader_id
borrow_request_id
borrowed_at
due_at
returned_at
```

`borrow_request_id` remains unique.

`reader_id` is intentionally retained even though it can be reached through BorrowRequest because Borrowing is a first-class historical record and reader-centric queries are common.

Application logic must enforce consistency between the Borrowing and its source BorrowRequest.

Overdue remains derived and is not persisted as an independent boolean.

---

# 9. Circulation Constraints

Retain PostgreSQL partial unique constraints for:

```text
one active request per physical item
one active request per reader + resource
one active borrowing per physical item
```

These constraints directly protect core circulation invariants.

Some cross-record consistency rules remain application-level rather than introducing complex database triggers.

---

# 10. Circulation Indexes

Indexes should continue to support real query patterns such as:

- available-copy allocation;
- reader active requests;
- request expiration scanning;
- active borrowing lookup;
- overdue ordering.

Indexes are not added merely because a column exists.

---

# 11. `digital_item`

Target:

```text
digital_item
------------
id
resource_id
preview_content_key
full_content_key
full_access_policy
```

A Resource has at most one DigitalItem in the V2 model.

Content keys represent references to content delivery/storage and must not assume generated demo PDFs.

V2 introduces:

```text
full_access_policy
------------------
PUBLIC
INSTITUTIONAL
RESTRICTED
```

`PUBLIC` grants full access without institutional entitlement.

`INSTITUTIONAL` grants full access to institutional readers of the current deployment.

`RESTRICTED` means V2 does not provide full access through public or institutional rules; preview may still be available.

The database does not store a `user_can_access` value because effective user access is derived.

---

# 12. Paid Access Tables

V1 currently uses:

```text
membership_plan
membership_subscription
payment_transaction
```

The V2 target terminology is:

```text
access_plan
access_subscription
payment_transaction
```

This makes the boundary explicitly about paid access rather than institutional library membership.

---

# 13. `access_plan`

Target:

```text
access_plan
-----------
id
code
name
duration_months
price_amount
currency
active
```

The V1 field:

```text
monthly_borrow_quota
```

is removed.

Physical borrowing policy is not the responsibility of a paid digital-access plan.

No plan-to-resource coverage model is introduced in V2 because external paid digital access is not yet a V2 product requirement.

---

# 14. `access_subscription`

Target:

```text
access_subscription
-------------------
id
account_id
plan_id
payment_transaction_id
starts_at
expires_at
```

Keep:

```text
expires_at > starts_at
```

A subscription currently originates from one successful payment transaction.

Future grant/trial models may revisit this constraint.

---

# 15. `payment_transaction`

Target fields remain:

```text
account_id
plan_id
amount
currency
status
created_at
completed_at
provider_txn_ref
provider_transaction_no
provider_response_code
```

Payment status remains:

```text
PENDING
SUCCESS
FAILED
```

Amount and currency remain transaction snapshots so historical payments do not change when a plan price changes.

---

# 16. Physical Circulation Policy

V2 does not introduce a `borrowing_policy` table.

Current rules such as:

```text
commitment limit
loan period
request expiration
pickup expiration
```

may remain application configuration while only one institutional policy exists.

A persisted policy model becomes justified only when the product needs multiple manageable reader policies.

---

# 17. Institution Model

V2 does not introduce an `institution` table.

The current assumption is:

```text
one deployment
=
one institutional library
```

Physical items and institutional digital access implicitly belong to that institution.

Multi-institution support remains a later design problem.

---

# 18. Entitlement Model

V2 does not introduce a generic `entitlement` table.

Effective digital access is derived from explicit data such as:

```text
DigitalItem.fullAccessPolicy
+
Account.readerType
+
future paid subscription information
```

The V2 resolver only needs to support the current product behavior cleanly.

---

# 19. Resource Deletion

V2 favors archival over hard deletion.

```text
ACTIVE
ARCHIVED
```

preserves historical relationships while removing a Resource from active catalog use.

Historical circulation records should not be cascade-deleted.

Physical items that have participated in circulation should generally be withdrawn instead of deleted.

---

# 20. Referential Integrity

Core relationships must use foreign keys.

Historical domain records should not use broad cascade deletion as their default lifecycle strategy.

---

# 21. CHECK Constraints

Database CHECK constraints should protect enum-like values including:

- account role;
- account status;
- reader type;
- resource status;
- metadata source;
- inventory status;
- circulation status;
- borrow-request status;
- digital-access policy;
- payment status.

Database constraints and application enums must remain synchronized.

---

# 22. Migration Strategy

The current `schema.sql` mixes:

```text
bootstrap
migration
backfill
legacy cleanup
constraint repair
```

and is executed during application startup.

V2 replaces this model with versioned migrations.

The preferred V2 migration tool is:

```text
Flyway
```

Example structure:

```text
db/migration/
├── V1__baseline_schema.sql
├── V2__v2_account_and_resource_model.sql
├── V3__digital_access_policy.sql
└── V4__decouple_paid_access.sql
```

Applied migrations should not be rewritten casually; new schema changes receive new migrations.

---

# 23. V1-to-V2 Migration

The migration path should:

1. identify the final V1 schema;
2. establish a migration baseline;
3. add `reader_type`;
4. remove `membership_eligible`;
5. add `resource_status`;
6. add digital access policy;
7. remove `monthly_borrow_quota`;
8. rename paid-access tables if adopted;
9. remove redundant fields only after compatibility review;
10. verify existing data and constraints;
11. retire the legacy startup schema-repair script.

---

# 24. Seed Strategy

Schema evolution, required reference data, demo data, and test fixtures must be separated.

Production must not automatically receive demo books, accounts, or circulation records.

Development/demo data belongs in a dev-specific seed path.

Tests should own deterministic fixtures independently.

---

# 25. PostgreSQL-specific Verification

H2 may remain for fast tests.

Behavior that depends materially on PostgreSQL should be verified against PostgreSQL, including:

- partial unique indexes;
- pessimistic locking;
- concurrent allocation;
- migrations;
- PostgreSQL-specific constraints and queries.

Testcontainers PostgreSQL is an appropriate V2 implementation option.

---

# 26. Main Changes from V1

```text
accounts.membership_eligible
→ DROP

accounts.reader_type
→ ADD

resource.resource_status
→ ADD

digital_item.full_access_policy
→ ADD

membership_plan.monthly_borrow_quota
→ DROP

membership_plan
→ access_plan

membership_subscription
→ access_subscription

BorrowRequest created_at / updated_at
→ candidate DROP when proven redundant

schema.sql mega lifecycle
→ REPLACE with versioned migrations

global data.sql demo seed
→ SPLIT by environment/purpose
```

---

# 27. Tables Intentionally Not Added in V2

V2 does not introduce:

```text
institution
institution_license
entitlement
author
borrowing_policy
waitlist
inter_library_loan
order
shopping_cart
fine
```

These require concrete future requirements.

---

# 28. V2 Database Principles

1. The database protects important business invariants.
2. Historical circulation data is not cascade-deleted.
3. Resource, physical inventory, and digital representation remain distinct persistence concerns.
4. Physical circulation does not depend on paid subscription.
5. Effective digital access is derived.
6. Persisted circulation status remains an explicit operational state with consistency rules.
7. A single-institution product does not need multi-institution schema.
8. Generic entitlement modeling is deferred until justified.
9. Schema evolution uses versioned migrations.
10. Demo data is separated from production data.
11. Indexes exist for actual queries and invariants.
12. The database model may evolve when product requirements provide new evidence.