# Librio — System Requirements

## 1. Purpose

This document defines the requirements for Librio V2 while transitioning from the Mock Project into a maintainable personal baseline.

V2 is primarily a repair and refactoring release. Its goals are to:

- preserve useful V1 product behavior and domain decisions;
- remove obsolete assumptions;
- separate responsibilities that are incorrectly coupled;
- establish a baseline that can be maintained, tested, and deployed over time.

Requirements are grouped into:

- **Functional Requirements:** behavior observable by an actor or the system.
- **Business Rules:** policies and invariants that system behavior must obey.
- **Non-functional Requirements:** consistency, security, maintainability, testability, and deployment qualities.
- **V2 Scope:** capabilities that must be preserved, repaired, or intentionally deferred.

### Status

- **[PRESERVE]** — useful V1 behavior or rules that remain valid.
- **[V2 REPAIR]** — behavior or concepts that must change to fit the new baseline.
- **[CLARIFY]** — intent is known, while detailed design is intentionally deferred.
- **[OUT OF V2]** — not a required V2 deliverable.

---

# 2. Functional Requirements

## 2.1 Resource Discovery

### FR-DIS-01 — Browse Resources
**[PRESERVE]**

Users must be able to browse library resources.

Each resource must provide enough summary information for the user to decide whether to open its details.

---

### FR-DIS-02 — Search Resources
**[PRESERVE]**

Users must be able to search resources using metadata supported by the current system.

V2 does not require a dedicated search engine or advanced search architecture when the current approach remains sufficient.

---

### FR-DIS-03 — View Resource Details
**[PRESERVE]**

Users must be able to inspect a resource to:

- understand what it is;
- identify physical and digital forms;
- inspect physical availability;
- understand whether digital access is currently usable.

---

### FR-DIS-04 — View Physical Availability
**[PRESERVE]**

When a resource has physical copies, the system must provide availability derived from the actual state of those copies.

The client must not independently create a separate availability source of truth.

---

## 2.2 Identity & Access

### FR-ID-01 — Login
**[PRESERVE]**

A user must be able to log in using a valid account.

The system must maintain an authenticated session and identify the current account.

Registration is not a mandatory V2 requirement.

---

### FR-ID-02 — Reader and Librarian Roles
**[PRESERVE]**

The system must distinguish at least:

```text
READER
LIBRARIAN
```

These roles represent authorization responsibilities.

A role must not be overloaded to represent institutional affiliation or paid entitlement.

---

### FR-ID-03 — Institutional Reader
**[V2 REPAIR]**

V2 must contain enough information to determine whether a reader belongs to the group eligible to use the circulation services of the current institution/library.

V2 assumes:

> One Librio deployment primarily represents one library/institution.

Generic multi-institution support is not required.

The persistence representation is defined later in `domain.md` and `database.md`.

---

## 2.3 Physical Circulation

### FR-CIR-01 — Request a Physical Resource
**[PRESERVE]**

An eligible Institutional Reader must be able to request a resource that has a borrowable physical copy.

The reader requests the resource; the server selects an exact physical copy.

---

### FR-CIR-02 — Reserve an Exact Physical Copy
**[PRESERVE]**

When a request is accepted, the system must reserve one exact physical item so competing requests cannot claim the same copy.

This is an internal circulation reservation.

It is not a waitlist or hold queue for unavailable resources.

---

### FR-CIR-03 — Prepare an Item for Pickup
**[PRESERVE / CLARIFY]**

A librarian may move a valid request into a prepared-for-pickup state.

V1 currently uses:

```text
REQUESTED
→ READY_FOR_PICKUP
→ FULFILLED
```

V2 keeps this behavior provisionally. It may be simplified if the preparation step no longer provides product value.

---

### FR-CIR-04 — Fulfil a Request and Create a Borrowing
**[PRESERVE]**

When a physical item is actually handed to the reader:

- the BorrowRequest must be fulfilled;
- a Borrowing must be created;
- the exact physical item must become borrowed;
- a due date must be assigned.

---

### FR-CIR-05 — Cancel, Reject, or Expire a Request
**[PRESERVE]**

An unfulfilled request may end through valid terminal outcomes such as:

```text
CANCELLED
REJECTED
EXPIRED
```

Any reserved physical item must be released appropriately.

---

### FR-CIR-06 — View Reader Borrowings
**[PRESERVE]**

Readers must be able to view relevant borrowing information associated with their accounts.

---

### FR-CIR-07 — Librarian Circulation Operations
**[PRESERVE]**

Librarians must be able to inspect and process requests and borrowings required by the circulation workflow.

---

### FR-CIR-08 — Return a Physical Item
**[PRESERVE]**

Librarians must be able to complete a return.

A successful return must:

- record the borrowing as returned;
- release the exact physical item from borrowed state;
- make the item borrowable again when inventory state allows it.

---

## 2.4 Digital Access

### FR-DIG-01 — Identify a Digital Form
**[PRESERVE]**

When a resource has a digital representation, users must be able to identify its existence.

The existence of digital content does not automatically imply full access.

---

### FR-DIG-02 — Preview Digital Content
**[PRESERVE / V2 REPAIR]**

When preview content exists, the system may expose it without granting full digital access.

Preview and full content must remain distinguishable by the server.

---

### FR-DIG-03 — Access Full Digital Content
**[V2 REPAIR]**

The server must decide whether a user can access full digital content.

V2 must be able to distinguish at least:

```text
public/free access
institutional access
no full access
```

V2 is not required to productize paid external access.

The design must not assume that paid membership is the only possible source of digital entitlement.

---

### FR-DIG-04 — Digital Content Delivery
**[V2 REPAIR]**

V2 must remove long-term dependency on demo-generated content.

Digital content must use a replaceable content reference or delivery mechanism.

A specific production cloud-storage provider is not required in V2.

---

## 2.5 Resource & Inventory Administration

### FR-ADM-01 — Create and Update Resources
**[PRESERVE]**

Librarians must be able to create and maintain catalog resource metadata.

---

### FR-ADM-02 — Manage Physical Items
**[PRESERVE]**

Librarians must be able to maintain physical-copy information such as:

- barcode;
- location;
- inventory status;
- circulation-related state.

The system must reject changes that would violate active circulation.

---

### FR-ADM-03 — Manage Digital Representations
**[PRESERVE / V2 REPAIR]**

Librarians must be able to maintain the digital representation of a resource.

Digital content storage and access policy must remain separate concerns when they represent different responsibilities.

---

### FR-ADM-04 — Safely Reconcile Physical Copies
**[PRESERVE]**

Resource administration must not remove or withdraw physical copies involved in active requests or borrowings in a way that breaks circulation integrity.

---

## 2.6 ISBN & External Metadata

### FR-META-01 — ISBN
**[PRESERVE]**

Book-like resources may contain an ISBN.

The system must normalize ISBN into the canonical form supported by the implementation and enforce the current uniqueness policy.

---

### FR-META-02 — External Metadata Lookup
**[PRESERVE]**

A librarian may use an ISBN to retrieve bibliographic metadata from an external provider.

Google Books is the current provider but must not become a fixed domain dependency.

---

### FR-META-03 — External Metadata Is Editable Prefill
**[PRESERVE]**

External metadata must not automatically become catalog truth.

Expected flow:

```text
ISBN lookup
→ provider result
→ editable prefill
→ librarian review
→ persist
```

---

## 2.7 Paid Access Capability

### FR-PAY-01 — Paid Access Does Not Control Physical Borrowing
**[V2 REPAIR]**

Physical circulation in V2 must not require an active paid membership by default.

Institutional borrowing eligibility must remain independent from payment or subscription.

---

### FR-PAY-02 — Payment and Subscription Are Separate from Circulation
**[V2 REPAIR]**

If the V1 payment and subscription implementation is retained, it must exist as a separate capability intended for future paid digital access.

A successful payment may create or activate a subscription/access record.

It must not directly grant physical borrowing privileges.

---

### FR-PAY-03 — External Paid Digital Access
**[OUT OF V2]**

V2 is not required to expose paid digital subscriptions to external users.

This remains a V3+ candidate.

V2 only needs to avoid domain decisions that would make such a capability unnecessarily difficult to introduce later.

---

# 3. Business Rules

## BR-01 — Resource, Physical Item, and Digital Representation Are Different Concepts
**[PRESERVE]**

A `Resource` represents the logical or bibliographic resource.

Physical copies and digital forms represent ways in which that resource exists or can be accessed.

---

## BR-02 — Physical Copies Must Belong to the Managed Collection
**[V2 REPAIR]**

The current library can only lend physical items that it actually manages.

A paid subscription cannot create physical inventory.

---

## BR-03 — Borrowability Depends on Inventory and Circulation State
**[PRESERVE]**

A physical item is borrowable only when its inventory and circulation states allow it.

---

## BR-04 — One Exact Item Has at Most One Active Request Allocation
**[PRESERVE]**

A physical copy must not be reserved by multiple active BorrowRequests simultaneously.

---

## BR-05 — One Exact Item Has at Most One Active Borrowing
**[PRESERVE]**

A physical copy must not belong to multiple active borrowings simultaneously.

---

## BR-06 — Duplicate Active Circulation for the Same Reader and Resource Is Not Allowed
**[PRESERVE]**

The current policy must prevent a reader from creating duplicate active circulation for the same resource.

---

## BR-07 — BorrowRequest and Borrowing Are Different Concepts
**[PRESERVE]**

A `BorrowRequest` represents the pre-checkout request/reservation workflow.

A `Borrowing` represents the actual loan after the item is handed to the reader.

---

## BR-08 — Terminal Requests Must Release or Consume Their Reservation
**[PRESERVE]**

```text
CANCELLED / REJECTED / EXPIRED
→ release reservation

FULFILLED
→ consume reservation into Borrowing
```

---

## BR-09 — Physical Borrowings Require a Due Date
**[PRESERVE]**

Every borrowing must contain enough information to determine `dueAt`.

---

## BR-10 — Overdue Is Derived
**[PRESERVE]**

A borrowing is overdue when:

```text
returnedAt == null
AND dueAt < current time
```

A separate `isOverdue` field must not become an independent source of truth.

---

## BR-11 — Overdue Does Not Automatically Create Fines or Payments
**[PRESERVE]**

V2 does not automatically create billing obligations from overdue borrowings.

---

## BR-12 — Inventory State and Circulation State Are Different Dimensions
**[PRESERVE]**

States such as:

```text
LOST
DAMAGED
WITHDRAWN
```

represent a different concern from:

```text
AVAILABLE
RESERVED
BORROWED
```

---

## BR-13 — Availability Is Server-authoritative
**[PRESERVE]**

The client must not maintain an independent source of truth for resource availability.

---

## BR-14 — Digital Existence and Digital Entitlement Are Different
**[V2 REPAIR]**

These are separate questions:

```text
Does digital content exist?
Does the institution grant access?
Does this user currently have full access?
```

They must not be collapsed into one boolean.

---

## BR-15 — Digital Access May Have Different Sources
**[V2 REPAIR]**

The domain must support reasoning about different access sources:

```text
public/free
institutional
personal paid access (future)
```

V2 does not require full implementation of personal paid access.

---

## BR-16 — Role, Affiliation, and Entitlement Are Different Concepts
**[V2 REPAIR]**

For example:

```text
role = READER
```

does not by itself determine institutional affiliation or digital entitlement.

---

## BR-17 — External Metadata Does Not Automatically Replace Catalog Data
**[PRESERVE]**

External providers assist metadata acquisition.

Librio remains the source of truth after librarian review and persistence.

---

# 4. Non-functional Requirements

## 4.1 Data Integrity & Concurrency

### NFR-CON-01 — Preserve Circulation Invariants
**[PRESERVE]**

Concurrent operations must not allow the same physical item to be reserved or borrowed by multiple users.

---

### NFR-CON-02 — Business Operations Must Be Atomic
**[PRESERVE]**

Related persistence changes must commit or roll back consistently.

---

### NFR-CON-03 — Use Database Constraints Where Appropriate
**[PRESERVE]**

Persistence invariants that can be represented reasonably by FK, UNIQUE, CHECK, or similar constraints should not rely only on frontend validation.

---

## 4.2 Security

### NFR-SEC-01 — Enforce Authentication and Authorization on the Server
**[PRESERVE]**

Frontend routing or hidden controls are not security boundaries.

---

### NFR-SEC-02 — Enforce Ownership
**[PRESERVE]**

Readers must only access or modify account and circulation data they are authorized to use.

---

### NFR-SEC-03 — Externalize Secrets
**[PRESERVE / V2 REPAIR]**

Database credentials, payment secrets, and production secrets must remain outside source code.

---

## 4.3 Maintainability

### NFR-MNT-01 — Responsibilities Must Be Clear
**[V2 REPAIR]**

Important business rules must have understandable ownership instead of being scattered unnecessarily across controllers, frontend components, repositories, and SQL.

---

### NFR-MNT-02 — Do Not Preserve Mock Artifacts Without Value
**[V2 REPAIR]**

Code, configuration, and documentation that exist only because of sprint, deadline, or submission constraints may be removed.

---

### NFR-MNT-03 — Avoid Premature Generalization
**[V2 REPAIR]**

V2 must not introduce generic abstractions or entities only for hypothetical future requirements.

V2 does not automatically require:

```text
Institution table
generic Entitlement table
BorrowingPolicy table
```

---

## 4.4 Testability

### NFR-TST-01 — Core Business Invariants Require Automated Tests
**[PRESERVE / V2 REPAIR]**

Important behaviors including exact-copy reservation, lifecycle transitions, returns, inventory protection, authorization, and external metadata verification must have appropriate automated tests.

---

### NFR-TST-02 — Browser E2E Is Not Required
**[V2 REPAIR]**

V2 does not require Playwright.

Critical behavior may be protected through service, API, and integration tests when those tests provide sufficient confidence.

---

### NFR-TST-03 — Database-critical Behavior Must Be Verified Appropriately
**[V2 REPAIR]**

H2 may remain useful for fast tests.

Behavior depending on PostgreSQL-specific locking, constraints, indexes, or migration semantics should be verified against PostgreSQL where necessary.

---

## 4.5 Configuration & Deployment

### NFR-DEP-01 — Externalize Environment Configuration
**[PRESERVE]**

Environment-specific configuration must not require source-code modification.

---

### NFR-DEP-02 — Deployment Must Be Reproducible
**[V2 REPAIR]**

The application must have a deployment path reproducible from repository content and documented configuration.

---

### NFR-DEP-03 — Database Schema Requires an Explicit Lifecycle
**[V2 REPAIR]**

V2 should not continue relying long-term on one `schema.sql` file that simultaneously performs bootstrap, backfill, and migration.

Database evolution should move toward versioned migrations.

---

### NFR-DEP-04 — Separate Demo Data from Production Data
**[V2 REPAIR]**

Demo resources, accounts, and sample circulation state must not automatically populate every environment.

---

# 5. V2 Scope

## 5.1 V2 Must Preserve or Repair

- resource browse, search, and detail;
- physical and digital access-form discovery;
- server-authoritative availability;
- login/session/security;
- Reader/Librarian authorization;
- institutional physical circulation;
- exact-copy reservation;
- request lifecycle;
- borrowing, due date, overdue, and return;
- physical inventory management;
- resource administration;
- digital representation and server-side access decisions;
- ISBN normalization;
- Google Books/external metadata lookup;
- retained payment/subscription code must be decoupled from physical borrowing;
- automated tests for core business behavior;
- reproducible configuration and deployment.

---

## 5.2 Not Required in V2

V2 is not required to add:

- external physical borrowing;
- inter-library loan;
- physical-book commerce;
- external paid digital-product UX;
- multi-institution or multi-tenant support;
- a generic entitlement engine;
- waitlists for resources with no available copy;
- overdue fines or billing;
- advanced reporting/statistics;
- social login;
- a new production-grade object-storage integration;
- reminder channels;
- AI recommendation;
- AI metadata extraction;
- summarization;
- plagiarism detection.

These capabilities remain candidates for V3+.

---

# 6. Decisions Deferred to Domain and Design

The requirements layer does not prescribe the exact representation of:

- institutional-reader persistence;
- whether `READY_FOR_PICKUP` remains in the final V2 lifecycle;
- the final set of BorrowRequest audit fields;
- digital access classification;
- final naming/modeling of paid subscriptions;
- whether physical circulation policy needs its own persisted model;
- Resource archival/soft-delete behavior;
- the database migration tool;
- the digital storage provider.

These decisions belong in `domain.md`, `database.md`, `architecture.md`, or an ADR when necessary.

---

# 7. Requirements Maintenance Rules

- Functional requirements describe observable behavior.
- Business Rules describe policy and invariants.
- NFRs describe quality requirements.
- Implementation details should not become product requirements unless behavior depends on them.
- Future possibilities must not silently become V2 requirements.
- V1 behavior is preserved only when the salvage review finds continuing value.
- Requirements may evolve as implementation and product learning provide new evidence.