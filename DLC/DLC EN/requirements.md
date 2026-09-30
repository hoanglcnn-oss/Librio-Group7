# Librio — System Requirements

## 1. Purpose

This document defines the requirements that Librio V2 must preserve or clarify while transitioning from the Mock Project into a personal baseline.

V2 is primarily a repair and refactoring phase, not a major feature-expansion release.

Requirements are divided into:

- **Functional Requirements:** observable behavior performed by an actor or the system.
- **Business Rules:** rules that system behavior must obey.
- **Non-functional Requirements:** quality attributes or passive behavior the system must maintain.
- **Out of V2 Scope:** capabilities that are not part of the baseline repair.

### Status

- **[PRESERVE]** — clearly established V1 behavior that V2 must preserve.
- **[CLARIFY]** — the intent exists, but current specification is insufficient.
- **[V2 REPAIR]** — a requirement introduced because V2 aims to become a maintainable baseline.
- **[OUT OF V2]** — not a V2 requirement.

---

# 2. Functional Requirements

## 2.1 Resource Discovery

### FR-DIS-01 — Browse Resources
**[PRESERVE]**

Readers must be able to browse library resources.

**Expected outcome**
- A resource list is displayed.
- Each resource provides enough summary information for a reader to decide whether to open its details.

---

### FR-DIS-02 — Search Resources
**[PRESERVE / CLARIFY]**

Readers must be able to search for resources.

V2 must preserve useful search behavior that already exists in V1.

**To clarify from V1**
- searchable fields;
- filters;
- sorting;
- pagination;
- exact search behavior currently implemented.

---

### FR-DIS-03 — View Resource Details
**[PRESERVE]**

Readers must be able to open a resource and view enough information to:

- understand the resource;
- identify available physical or digital access forms;
- understand how the resource can be used.

---

### FR-DIS-04 — View Access Forms
**[PRESERVE]**

Readers must be able to identify whether a resource supports:

- physical access;
- digital access;
- or both.

---

### FR-DIS-05 — View Availability
**[PRESERVE]**

Readers must be able to determine whether a resource or related item is currently available.

When a resource contains multiple items, availability must be presented in a reader-understandable form.

---

## 2.2 Physical Circulation

### FR-CIR-01 — Borrow a Physical Item
**[PRESERVE]**

Readers must be able to submit a request to borrow a physical item.

The server is responsible for deciding whether the request is valid.

**Expected outcome**
- If eligible, a borrowing is created.
- If not eligible, the operation is rejected with an appropriate error.

---

### FR-CIR-02 — Assign a Due Date
**[PRESERVE]**

When a physical borrowing is successfully created, the system must provide enough information to determine its due date.

---

### FR-CIR-03 — Determine Overdue Status
**[PRESERVE / CLARIFY]**

The system must be able to determine whether an active borrowing is overdue.

The exact overdue representation will be defined in `domain.md` and `database.md`.

---

### FR-CIR-04 — Return a Physical Item
**[PRESERVE]**

The system must support returning a borrowed physical item.

**Expected outcome**
- the borrowing is completed;
- circulation state is updated;
- the item may become borrowable again unless another rule prevents it.

---

### FR-CIR-05 — View Borrowing Status
**[PRESERVE / CLARIFY]**

Readers or librarians must be able to view enough information to understand the current state of a relevant borrowing.

Exact fields must be recovered from V1.

---

## 2.3 Digital Access

### FR-DIG-01 — Identify Digital Access
**[PRESERVE]**

When a resource has a digital form, readers must be able to identify that digital access exists.

---

### FR-DIG-02 — Access Digital Resources
**[PRESERVE / CLARIFY]**

Readers must be able to access digital resources according to the behavior currently supported by V1.

**To clarify from V1**
- link, viewer, or other access mechanism;
- download support;
- current permission model;
- digital content types actually supported.

---

## 2.4 User Account

### FR-USR-01 — Authenticate and Identify Users
**[PRESERVE / CLARIFY]**

The system must be able to identify users so account-related and circulation operations can behave correctly.

Registration, login, and profile behavior must be recovered from V1.

---

### FR-USR-02 — Associate Borrowings with Readers
**[PRESERVE]**

Every borrowing must be associated with its corresponding reader.

---

### FR-USR-03 — View Account-related Information
**[PRESERVE / CLARIFY]**

Users must be able to view account-related information currently supported by V1 and still required by the core workflow.

The exact scope will be verified during the salvage pass.

---

## 2.5 Library Administration

### FR-ADM-01 — Maintain Resource Data
**[PRESERVE]**

Librarians must be able to create or maintain resource data required by the core workflow.

---

### FR-ADM-02 — Maintain Items
**[PRESERVE]**

Librarians must be able to maintain physical and digital items required for correct resource access.

---

### FR-ADM-03 — Support Circulation
**[PRESERVE]**

Librarians must be able to perform or support the circulation operations currently provided by V1.

Exact operations must be inventoried from the implementation.

---

# 3. Business Rules

## BR-01 — Resource and Access Form Are Different Concepts
**[PRESERVE]**

A `Resource` represents a logical or bibliographic resource.

Physical and digital access forms may have separate lifecycles.

**Example**

```text
Resource: Clean Code

├── Physical Item #A
├── Physical Item #B
└── Digital PDF
```

`Clean Code` is one resource.

The two physical copies and the PDF are not three independent resources.

---

## BR-02 — One Physical Item Has at Most One Active Borrowing
**[PRESERVE]**

A physical item must not belong to multiple active borrowings at the same time.

**Example**

```text
Physical Item #12
→ currently borrowed by Hoang
```

Linh attempts to borrow `Item #12`.

Valid result:
- Linh's request is rejected.

Invalid result:
- both Hoang and Linh have active borrowings for `Item #12`.

---

## BR-03 — A Physical Item May Have Multiple Historical Borrowings
**[PRESERVE]**

BR-02 only limits simultaneous active borrowings.

**Example**

```text
Item #12
→ Hoang borrows → returns
→ Linh borrows  → returns
→ Nam borrows   → active
```

All three records are valid because only Nam's borrowing is active.

---

## BR-04 — Physical Borrowings Require a Due Date
**[PRESERVE]**

Every physical borrowing must contain enough information to determine its due date.

**Example**

```text
Borrowed at: 2026-09-01
Due at:      2026-09-15
```

Without `dueAt`, overdue status cannot be determined reliably.

---

## BR-05 — Overdue Applies Only to an Active Borrowing
**[CLARIFY]**

A borrowing is overdue when:

- it remains active;
- its `dueAt` has passed.

**Example**

```text
dueAt = 2026-09-15
today = 2026-09-20
returnedAt = null
```

→ overdue.

If:

```text
returnedAt = 2026-09-18
```

→ no longer an active overdue borrowing.

Whether this state is persisted or derived is not decided at the requirements level.

---

## BR-06 — Borrow and Return Decisions Are Server-side
**[PRESERVE]**

The client must not independently decide that a borrow or return operation succeeded.

**Example**

Incorrect:

```text
set item = BORROWED
→ then call API
```

Correct:

```text
call API
→ server validates
→ server returns success
→ frontend updates UI
```

---

## BR-07 — Availability Must Reflect System State
**[PRESERVE]**

Availability must not be independently inferred by the client.

**Example**

If both physical copies are borrowed:

```text
Copy A = unavailable
Copy B = unavailable
```

the frontend must not display:

```text
Physical: Available
```

because of stale local state.

---

## BR-08 — Availability May Aggregate Multiple Items
**[PRESERVE]**

A resource may contain multiple items while the reader sees a summarized availability state.

**Example**

```text
Resource: Clean Code

Physical Copy A → Borrowed
Physical Copy B → Available
Digital PDF     → Available
```

A reader-facing summary may be:

```text
Physical: 1 copy available
Digital: Available
```

---

## BR-09 — Digital Access Does Not Expire in the Baseline
**[PRESERVE]**

Digital access in the V1/V2 baseline does not automatically expire after a borrowing period.

**Example**

If the user is allowed to access a PDF:

```text
Monday  → accessible
Friday  → still accessible
```

There is no baseline rule such as:

```text
access expires after 7 days
```

---

## BR-10 — Overdue Does Not Imply Fine or Payment
**[PRESERVE]**

An overdue borrowing does not automatically create a fine, payment, or billing obligation in V2.

**Example**

```text
Borrowing = overdue
```

may result in a warning or status change.

It does not automatically result in:

```text
Fine = 50,000 VND
```

---

## BR-11 — Reservation Is Not Part of the Baseline
**[PRESERVE]**

If an item is currently borrowed, V2 is not required to provide a reservation queue.

**Example**

```text
Item #12 = borrowed
```

Another reader may simply see it as unavailable.

A:

```text
Reserve this item
```

operation is not required.

---

# 4. Non-functional Requirements

NFRs describe qualities the system must maintain while performing its functions.

They can be treated as passive system behavior: users do not invoke them directly, but they affect correctness, safety, and maintainability.

---

## 4.1 Data Integrity & Consistency

### NFR-CON-01 — Preserve Circulation Invariants
**[PRESERVE]**

Borrow and return operations must not violate circulation invariants.

**Example**

Two borrow requests for `Item #12` arrive almost simultaneously.

Valid:

```text
Request A → success
Request B → rejected
```

Invalid:

```text
Request A → success
Request B → success
→ 2 active borrowings
```

---

### NFR-CON-02 — Avoid Partial Updates
**[PRESERVE / CLARIFY]**

When one business operation requires multiple related persistence changes, the system must avoid inconsistent partial updates.

**Example**

A borrow flow requires:

```text
create Borrowing
+
update circulation state
```

If the second step fails, the system must not remain in a state such as:

```text
Borrowing created
but Item still Available
```

---

## 4.2 Security

### NFR-SEC-01 — Enforce Authorization on the Server
**[CLARIFY]**

Operations requiring a role or ownership must not rely only on frontend restrictions.

**Example**

Hiding an Admin button from a reader is not enough.

The reader must also be unable to directly call:

```http
DELETE /admin/resources/12
```

successfully.

---

### NFR-SEC-02 — Do Not Hard-code Secrets
**[V2 REPAIR]**

Credentials, tokens, and production secrets must remain outside source code.

**Example**

Invalid:

```java
String password = "postgres123";
```

Preferred:

```text
DB_PASSWORD=<environment variable>
```

---

## 4.3 Maintainability

### NFR-MNT-01 — Responsibilities Must Be Understandable
**[V2 REPAIR]**

Modules and layers must have responsibilities clear enough for the maintainer to understand the main flow.

**Example**

A borrowing business rule should not be scattered across:

```text
Controller
React component
Repository
SQL script
```

without a clear owner.

---

### NFR-MNT-02 — Do Not Preserve Academic Structure Without Value
**[V2 REPAIR]**

Code and documentation should not preserve structure only because the Mock Project once required it.

**Example**

If a WBS spreadsheet no longer helps development:

```text
→ it does not need to be maintained just to make the project appear complete
```

---

### NFR-MNT-03 — Avoid Premature Abstraction
**[V2 REPAIR]**

V2 should not create abstractions only for hypothetical future use cases.

**Example**

If only one search implementation exists, there is no need to immediately introduce:

```text
SearchProviderFactory
SearchProviderStrategy
SearchProviderAdapter
SearchProviderRegistry
```

only because Elasticsearch might be used later.

---

## 4.4 Testability

### NFR-TST-01 — Core Business Rules Must Be Automatically Testable
**[V2 REPAIR]**

Rules capable of breaking core behavior must have appropriate automated tests.

**Example**

At minimum:

```text
given Item #12 is already borrowed
when another borrow is attempted
then the operation must fail
```

The goal is behavior protection, not coverage percentage alone.

---

## 4.5 Configuration & Deployment

### NFR-DEP-01 — Configuration Must Be Externalized
**[V2 REPAIR]**

Environment-specific configuration should not require manual source-code edits.

**Example**

The same build should be able to target:

```text
local DB
staging DB
production DB
```

through configuration.

---

### NFR-DEP-02 — Deployment Must Be Reproducible
**[V2 REPAIR]**

V2 must have a deployment path that a future maintainer can reproduce from the available documentation and configuration.

**Example**

The project should not depend on:

```text
"it works on my machine because I clicked some setting months ago and no longer remember which one"
```

---

## 4.6 Observability & Error Handling

### NFR-OBS-01 — Important Failures Must Be Diagnosable
**[V2 REPAIR]**

The backend must provide enough error and log context to diagnose important server-side failures.

**Example**

Poor:

```text
500 Internal Server Error
```

with no useful server-side context.

Better:

```text
operation = borrow
itemId = 12
exception = ...
```

without logging secrets or sensitive information.

---

### NFR-OBS-02 — Client-facing Errors Should Be Consistent
**[V2 REPAIR / CLARIFY]**

Common failures should use a sufficiently stable response format for the frontend to handle.

**Example**

Avoid unrelated endpoints returning:

```text
"error"
"failed"
null
HTML error page
```

for similar failures.

A consistent error contract should be defined during API/design work.

---

# 5. V2 Scope

## 5.1 V2 Must Preserve

The current baseline includes:

- resource discovery;
- resource detail;
- availability;
- physical borrowing;
- due date and overdue;
- returns;
- basic digital access;
- user behavior required by the core workflow;
- minimal librarian administration.

---

## 5.2 V2 Is Not Required to Expand

V2 is not required to add:

- reservation system;
- fine/payment;
- digital expiry or licensing;
- advanced librarian administration;
- advanced statistics/reporting;
- social login;
- Google Books / ISBN integration;
- new cloud-storage integrations;
- AI recommendation;
- metadata extraction;
- summarization;
- plagiarism detection;
- IoT or hardware integration.

These capabilities may become requirements in a later Analyze → Design cycle.

---

# 6. V2 Clarification Backlog

## 6.1 Search
- current searchable fields;
- existing filters/sorting/pagination;
- behavior actually consumed by the frontend.

## 6.2 User / Security
- registration;
- login;
- profile;
- roles;
- authorization;
- ownership behavior.

## 6.3 Borrowing / Overdue
- how active borrowing is represented;
- whether overdue is persisted or derived;
- how return state is represented.

## 6.4 Availability
- whether item availability is stored or derived;
- where resource-level availability is aggregated;
- which fields the frontend depends on.

## 6.5 Digital Access
- existing digital item types;
- access mechanism;
- download/viewer behavior;
- current permissions.

## 6.6 Librarian Administration
- operations that actually exist in source;
- operations that exist only in proposal/docs;
- operations exposed by the frontend.

---

# 7. Requirements Maintenance Rules

- FRs describe behavior that an actor or the system can perform or observe.
- Scope exclusions are not written as FRs.
- Business policies that are not user actions belong under Business Rules.
- Business Rules should include concrete examples so they can be understood independently.
- NFRs should be grouped by quality category.
- NFRs should include examples showing how the requirement affects runtime or design.
- Any requirement not supported by the current source must be marked **[CLARIFY]** rather than assumed.
- Requirements may evolve when the salvage pass or implementation provides new evidence.