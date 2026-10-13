# Librio — Domain Model

## 1. Purpose

This document defines the Librio V2 domain model.

Its purpose is to clarify:

- the business concepts that exist in Librio;
- the meaning of each concept;
- their relationships;
- important lifecycles and invariants;
- which states are stored and which should be derived;
- the boundaries between physical circulation, digital access, and paid access.

This document does **not** define the database schema.

Tables, columns, indexes, foreign keys, and migration strategy belong in `database.md`.

---

# 2. Domain Context

Librio V2 is:

> **A Library + Digital Content Platform for an institutional library.**

V2 assumes one deployment primarily serves one library or institution.

```text
Librio deployment
        |
        v
Institution / Library
        |
        +---- Physical collection
        |
        +---- Digital access
        |
        +---- Readers
        |
        +---- Librarians
```

V2 is not a multi-tenant platform.

Examples involving multiple universities may be used for domain reasoning, but one V2 instance does not need to manage them simultaneously.

---

# 3. Actors

## 3.1 Reader

A `Reader` uses library resources.

Physical circulation in V2 is primarily intended for an **Institutional Reader**.

A reader may:

- browse and search resources;
- inspect resource details;
- inspect physical availability;
- inspect digital access;
- create borrow requests when eligible;
- view their own requests and borrowings;
- access digital content when authorized.

---

## 3.2 Librarian

A `Librarian` operates the library.

A librarian may:

- maintain resource metadata;
- maintain physical inventory;
- maintain digital representations;
- process circulation requests;
- prepare, fulfil, or reject requests;
- process returns;
- use external metadata lookup.

`Librarian` does not imply employment by a particular university.

It represents the operator role for the current library.

---

# 4. Identity, Role, and Affiliation

## 4.1 Account

`Account` represents an identity recognized by Librio.

It is responsible for:

- login identity;
- display information;
- account lifecycle;
- authorization role.

Current roles are:

```text
READER
LIBRARIAN
```

---

## 4.2 Role Is Not Affiliation

These concepts must remain distinct:

```text
Role
≠
Institutional affiliation
≠
Access entitlement
```

For example:

```text
role = READER
```

does not by itself indicate:

- whether the reader belongs to the institution;
- whether the reader may borrow physical items;
- which digital resources the reader may access;
- whether the reader has a paid subscription.

---

## 4.3 Institutional Reader

V2 needs the concept:

> A Reader who belongs to the current institution/library and is eligible to use institutional services.

For example:

```text
Account
role = READER

institutional affiliation = ACTIVE
```

may be eligible for physical circulation.

The persistence representation of affiliation is not defined at the domain layer.

---

## 4.4 External Reader

An External Reader does not have a current institutional affiliation.

V2 does not require a complete external-user product.

In particular:

```text
External Reader
→ no default physical borrowing in V2
```

Paid digital access for external readers remains a V3+ direction.

---

# 5. Resource

## 5.1 Resource

`Resource` represents the logical or bibliographic resource discovered by users.

For example:

```text
Clean Code
```

is one `Resource`.

A Resource may contain metadata such as:

- title;
- author information;
- description;
- category;
- ISBN;
- cover;
- metadata provenance.

A Resource is not a specific physical copy.

A Resource is also not a specific digital file.

---

## 5.2 Current Resource Scope Is Book-oriented

The `Resource` concept may eventually support different content types.

The V2 implementation is currently centered on book-like resources.

No subtype hierarchy such as:

```text
Book
Article
Video
Audio
Thesis
...
```

is required without a concrete use case.

---

# 6. Physical Representation

## 6.1 PhysicalItem

`PhysicalItem` represents one specific physical copy of a Resource.

Example:

```text
Resource: Clean Code

PhysicalItem #101
barcode = LIB-101

PhysicalItem #102
barcode = LIB-102
```

These are separate physical assets.

---

## 6.2 Inventory State

Inventory state describes the condition or lifecycle of the physical asset.

Current states:

```text
ACTIVE
LOST
DAMAGED
WITHDRAWN
```

---

## 6.3 Circulation State

Circulation state describes how the item participates in circulation.

Current states:

```text
AVAILABLE
RESERVED
BORROWED
```

The two dimensions are separate:

```text
Inventory state
≠
Circulation state
```

---

## 6.4 Borrowable Item

In the current baseline, an item is borrowable when:

```text
inventoryStatus = ACTIVE
AND
circulationStatus = AVAILABLE
```

Reader-facing resource availability is aggregated from physical items.

---

# 7. BorrowRequest

## 7.1 Meaning

`BorrowRequest` represents the intention and workflow before the actual loan begins.

The reader requests a `Resource`.

The server selects an exact `PhysicalItem`.

```text
Reader
  |
  v
BorrowRequest
  |
  +---- Resource
  |
  +---- allocated PhysicalItem
```

---

## 7.2 Exact-copy Reservation

When a BorrowRequest is accepted, the server reserves one exact physical item.

```text
Clean Code

#101 AVAILABLE
#102 AVAILABLE
```

may become:

```text
BorrowRequest
→ #101

#101
AVAILABLE → RESERVED
```

The purpose is to prevent competing requests from claiming the same copy.

---

## 7.3 Exact Reservation Is Not a Waitlist

These are different concepts:

```text
Exact-copy reservation
≠
Waitlist / hold queue
```

V2 supports exact-copy reservation.

V2 does not require a queue for unavailable resources.

---

# 8. BorrowRequest Lifecycle

Current lifecycle:

```text
REQUESTED
      |
      v
READY_FOR_PICKUP
      |
      v
FULFILLED
```

Other terminal outcomes:

```text
CANCELLED
REJECTED
EXPIRED
```

---

## 8.1 REQUESTED

The request exists and an exact item has been reserved.

---

## 8.2 READY_FOR_PICKUP

A librarian has prepared the physical item for handoff.

This distinguishes:

```text
request accepted
≠
item physically prepared
```

V2 keeps this state provisionally.

It may be simplified if the preparation step no longer provides sufficient product value.

---

## 8.3 FULFILLED

The physical item has actually been handed to the reader.

Fulfilment transitions the domain from:

```text
request/reservation
```

to:

```text
actual borrowing
```

---

## 8.4 CANCELLED / REJECTED / EXPIRED

These outcomes end the request without creating an actual borrowing.

The exact reservation must be released.

---

# 9. Borrowing

## 9.1 Meaning

`Borrowing` represents an actual physical loan.

It exists only after a BorrowRequest is fulfilled.

```text
BorrowRequest
      |
      | fulfil
      v
Borrowing
```

BorrowRequest and Borrowing remain separate concepts.

---

## 9.2 Borrowing Data

A Borrowing requires enough information to identify:

- the reader;
- the exact physical item;
- the originating BorrowRequest;
- borrowing time;
- due time;
- return time when returned.

---

## 9.3 Active Borrowing

A borrowing is active when:

```text
returnedAt == null
```

---

## 9.4 Overdue

Overdue is a derived domain state.

```text
overdue =
returnedAt == null
AND
dueAt < current time
```

A separate `isOverdue` value must not become an independent source of truth.

---

## 9.5 Return

A return ends the active borrowing.

If inventory state still permits circulation:

```text
PhysicalItem
BORROWED → AVAILABLE
```

---

# 10. Physical Circulation Invariants

## 10.1 One Exact Item Has at Most One Active Request Allocation

One physical copy must not be allocated to multiple active BorrowRequests.

---

## 10.2 One Exact Item Has at Most One Active Borrowing

One physical copy must not belong to multiple active Borrowings.

---

## 10.3 Active Reservation Requires an Owning Request

If:

```text
PhysicalItem.circulationStatus = RESERVED
```

an appropriate active BorrowRequest must exist.

---

## 10.4 BORROWED Requires an Active Borrowing

If:

```text
PhysicalItem.circulationStatus = BORROWED
```

an appropriate active Borrowing must exist.

---

## 10.5 Terminal Requests Must Not Leave Orphan Reservations

```text
CANCELLED
REJECTED
EXPIRED
```

release the reservation.

`FULFILLED` consumes the reservation into an actual Borrowing.

---

## 10.6 Circulation Operations Must Be Atomic

For example:

```text
BorrowRequest → FULFILLED
PhysicalItem  → BORROWED
Borrowing     → created
```

forms one consistent business operation.

---

# 11. DigitalItem

## 11.1 Meaning

`DigitalItem` represents digital content or a digital representation associated with a Resource.

It is separate from PhysicalItem and does not participate in physical circulation.

---

## 11.2 Digital Existence Is Not Digital Access

These questions are separate:

```text
1. Does a digital representation exist?
2. Does the current institution grant access?
3. Does the current user have full access?
```

They must not be collapsed into one `digitalAvailable` boolean.

---

# 12. Digital Access

## 12.1 Server-authoritative Access

The client does not independently decide whether a user has full digital access.

The server resolves access capability.

---

## 12.2 Access Sources

The domain uses the following access-source language:

```text
PUBLIC
INSTITUTIONAL
PERSONAL_PAID
```

### PUBLIC

Content is publicly accessible.

### INSTITUTIONAL

The current institution grants access to an institutional reader.

### PERSONAL_PAID

The user owns an individual paid access right.

This is a valid future domain direction but not a mandatory V2 capability.

---

## 12.3 Effective Digital Access

Conceptually:

```text
Effective Digital Access
=
Public access
OR
Institutional access
OR
Personal paid access
```

V2 must at minimum support reasoning about:

```text
PUBLIC
INSTITUTIONAL
NO_FULL_ACCESS
```

`PERSONAL_PAID` may be completed in V3+.

---

## 12.4 Preview

Preview represents partial content access.

Preview does not imply full entitlement.

---

# 13. Physical and Digital Access Are Independent

Physical holdings and digital access rights are separate dimensions.

A resource may have:

```text
physical copies = YES
institutional digital access = NO
```

or:

```text
physical copies = NO
institutional digital access = YES
```

These are both valid states.

---

# 14. Metadata and ISBN

## 14.1 ISBN

ISBN is a bibliographic identifier for book-like resources.

V2 preserves the current canonical ISBN policy and uniqueness rule.

Edition semantics may be revisited if the catalog model becomes more detailed.

---

## 14.2 Metadata Source

Current metadata provenance includes:

```text
MANUAL
GOOGLE_BOOKS
```

---

## 14.3 External Metadata Provider

Google Books is the current implementation provider.

The domain capability is:

> Bibliographic metadata lookup through an external provider.

Google Books is not a permanent domain dependency.

---

## 14.4 Editable Prefill

External metadata does not automatically become catalog truth.

```text
lookup
→ provider result
→ editable prefill
→ librarian review
→ persist
```

---

# 15. Paid Access Domain

## 15.1 Physical Circulation Does Not Depend on Paid Membership

V1 previously coupled:

```text
active membership
→ borrowing eligibility
```

V2 removes this assumption.

Physical borrowing belongs to institutional circulation policy.

---

## 15.2 Paid Access Plan

A paid plan may exist to grant digital access in future versions.

It may describe:

- duration;
- price;
- currency;
- digital-access benefits.

Physical borrowing quota is not a default responsibility of the paid access plan.

---

## 15.3 Subscription

A `Subscription` represents a paid-access period associated with an account.

The current implementation name may remain `MembershipSubscription`, but this terminology is subject to review because "membership" may be confused with institutional membership.

---

## 15.4 PaymentTransaction

`PaymentTransaction` represents a payment attempt or record.

Its lifecycle is:

```text
PENDING
SUCCESS
FAILED
```

A successful payment may activate paid digital access.

It does not directly create physical borrowing rights.

---

# 16. Domain Boundaries

## 16.1 Catalog

```text
Resource
metadata
ISBN
external metadata lookup
```

## 16.2 Physical Inventory

```text
PhysicalItem
barcode
location
inventory state
```

## 16.3 Circulation

```text
BorrowRequest
reservation
Borrowing
due date
overdue
return
```

## 16.4 Digital Content

```text
DigitalItem
preview/full content
content references
digital-access resolution
```

## 16.5 Identity & Security

```text
Account
Role
AccountStatus
Institutional eligibility
```

## 16.6 Paid Access

```text
Access Plan
Subscription
PaymentTransaction
```

Paid Access does not belong inside Physical Circulation.

---

# 17. Derived State vs Stored State

Examples of stored or operational state:

```text
PhysicalItem.inventoryStatus
PhysicalItem.circulationStatus
BorrowRequest.status
Account.status
Payment.status
```

Examples of derived state:

```text
overdue
resource-level physical availability
effective digital access
```

Derived state should not become a duplicate source of truth when it can be calculated reliably from authoritative data.

---

# 18. Single-Institution Assumption

V2 assumes:

```text
one deployment
≈
one library / institution
```

Therefore V2 does not require:

```text
Institution entity
institution_id on every record
multi-tenant boundaries
```

This assumption must be reviewed if V3+ introduces multiple institutions in one deployment.

---

# 19. Concepts That Do Not Yet Require Generic Entities

## 19.1 Entitlement

`Entitlement` is domain language describing:

> a valid reason why a user may access a capability or content.

Examples include:

```text
institutional affiliation
public content
paid subscription
```

V2 does not require a generic `Entitlement` entity when explicit policy logic is sufficient.

---

## 19.2 BorrowingPolicy

Rules such as:

```text
maximum concurrent borrowings
loan period
```

may remain configuration or policy code when only one institutional policy exists.

A persisted BorrowingPolicy model should be added only when multiple managed policies become a real requirement.

---

# 20. Decisions Deferred to Design

The following are intentionally not fixed in the domain model:

- exact persistence representation of institutional affiliation;
- whether `READY_FOR_PICKUP` remains in final V2 implementation;
- final BorrowRequest audit fields;
- exact persistence strategy for circulation status;
- persistence representation of digital-access classification;
- final naming of membership/access plan concepts;
- Resource archive versus hard-delete behavior;
- storage provider;
- database migration implementation.

---

# 21. V2 Domain Model Overview

```text
                         Account
                        /       \
                   READER      LIBRARIAN
                      |
            institutional eligibility
                      |
                      v
                   Resource
                 /          \
                /            \
       PhysicalItem        DigitalItem
            |                  |
            |             Digital Access
            |             Resolution
            |
       BorrowRequest
            |
         Borrowing
```

Paid access sits beside the digital-access domain:

```text
Account
   |
   v
Access Subscription
   |
   +---- Access Plan
   |
   +---- PaymentTransaction
   |
   v
Digital Access Resolution
```

---

# 22. V2 Domain Principles

1. A Resource is not a physical copy.
2. Physical and digital access are independent capabilities.
3. BorrowRequest is not Borrowing.
4. Inventory state is not circulation state.
5. Overdue is derived.
6. Availability is server-authoritative.
7. Role is not affiliation.
8. Affiliation is not entitlement.
9. Payment is not circulation.
10. Digital-content existence is not digital access.
11. Generic entities are not introduced only for hypothetical future needs.
12. V2 favors a small, explicit, extensible domain model over premature generalization.