# Librio — Architecture

## 1. Purpose

This document defines the target high-level architecture of Librio V2.

It explains:

- the main system components;
- component responsibilities;
- dependency direction;
- transaction boundaries;
- security enforcement;
- separation between circulation, digital access, metadata integration, and paid access;
- external-system boundaries;
- the major architectural repairs from V1.

This is a **High-Level Design** document.

Detailed API contracts and complex flows may live in `api/`, `design/`, or ADRs when necessary.

---

# 2. Architecture Context

Librio V2 remains a full-stack web application:

```text id="rl03pp"
Browser
   |
   v
React SPA
   |
   | HTTP / JSON
   | Session Cookie + CSRF
   v
Spring Boot Backend
   |
   +------ PostgreSQL
   |
   +------ External Metadata Provider
   |
   +------ Payment Provider
   |
   +------ Digital Content Storage/Delivery
```

Core stack:

```text id="sdjxhf"
Frontend
→ React + Vite

Backend
→ Spring Boot

Persistence
→ PostgreSQL

Authentication
→ Spring Security session

Deployment
→ containerized backend + deployable frontend
```

V2 does not introduce microservices.

A modular monolith remains appropriate.

---

# 3. Architecture Principles

1. The backend is the source of truth for business behavior.
2. The frontend does not decide authorization, availability, or entitlement.
3. Transaction boundaries belong to the application/service layer.
4. Repositories own persistence and queries.
5. External providers sit behind replaceable boundaries.
6. Physical Circulation does not depend on Paid Access.
7. Digital Access does not directly depend on a Payment Provider.
8. Abstractions require a clear responsibility.
9. Microservices are not introduced for demonstration purposes.
10. Modules should represent explainable business capabilities.

---

# 4. Top-level Components

```text id="azfq9l"
┌─────────────────────────┐
│       React SPA         │
└────────────┬────────────┘
             │
             v
┌─────────────────────────┐
│   Spring Web / Security │
└────────────┬────────────┘
             │
             v
┌─────────────────────────────────────┐
│         Application Services        │
│                                     │
│ Catalog                             │
│ Inventory                           │
│ Circulation                         │
│ Digital Access                      │
│ Identity                            │
│ External Metadata                   │
│ Paid Access                         │
└────────────┬────────────────────────┘
             │
             v
┌─────────────────────────┐
│   Repository / JPA      │
└────────────┬────────────┘
             │
             v
┌─────────────────────────┐
│       PostgreSQL        │
└─────────────────────────┘
```

External adapters include:

```text id="jmpzis"
External Metadata Provider
Payment Provider
Digital Content Provider
```

---

# 5. Frontend Architecture

The React SPA is responsible for:

- routing;
- page composition;
- forms;
- user interaction;
- rendering server state;
- API calls;
- presenting errors and status.

It does not own authoritative business rules.

Protected frontend routes improve UX but are not security boundaries.

The API client handles:

- base URL configuration;
- credentials;
- CSRF;
- common error parsing;
- endpoint calls.

As V2 is repaired, API code may be separated by capability when that improves ownership.

Runtime mock branches should be removed once the real backend provides the corresponding capability.

---

# 6. Backend Layering

V2 keeps a simple layering model:

```text id="qnp7cg"
Controller
    |
    v
Application / Domain Service
    |
    v
Repository
    |
    v
Database
```

Controllers handle HTTP concerns.

Services own business validation, transactions, orchestration, and state transitions.

Repositories own persistence, locks, and query behavior.

Repositories do not own business policy.

---

# 7. Identity & Security Architecture

V2 retains Spring Security session-based authentication.

```text id="mvm0dt"
login
→ Spring Security context
→ server session
→ JSESSIONID
```

State-changing requests remain protected by CSRF.

Authorization consists of:

- role-level API protection;
- ownership and business-specific validation in application services.

`readerType` remains a business classification, not an authorization role.

---

# 8. Catalog Module

Catalog owns:

```text id="mzq0ha"
Resource
metadata
browse/search/detail
ISBN
metadata provenance
archive lifecycle
```

It does not own the circulation state of individual physical copies.

---

# 9. External Metadata Boundary

Metadata lookup uses a provider boundary:

```text id="81obw4"
Resource Admin
      |
      v
Metadata Lookup Service
      |
      v
MetadataProvider
      |
      v
Google Books
```

Google Books is the current adapter.

Provider-specific response structures should not leak through the entire domain.

External lookup remains editable prefill rather than automatic persistence.

---

# 10. Physical Inventory Module

Inventory owns:

```text id="ls9qxr"
PhysicalItem
barcode
location
inventoryStatus
circulationStatus
inventory maintenance
```

It provides persistence and item-state capability to Circulation.

It does not decide reader eligibility.

---

# 11. Physical Circulation Module

Circulation owns:

```text id="21hx3x"
BorrowRequest
exact-copy reservation
Borrowing
due dates
overdue
returns
expiration
```

V1's `BorrowService` is large, but its central responsibility remains coherent: orchestration of the physical-circulation lifecycle.

V2 only splits services where a genuinely separate responsibility exists.

---

## 11.1 Borrow Eligibility

The V1 dependency:

```text id="fwepxu"
BorrowService
→ BorrowingQuotaPolicy
→ MembershipSubscription
```

must be removed.

Target:

```text id="jhdsdt"
CirculationService
        |
        v
BorrowEligibilityPolicy
        |
        +---- Account / readerType
        +---- active commitments
        +---- circulation configuration
```

Physical circulation must not depend on Paid Access.

---

## 11.2 Transaction Boundaries

Create request:

```text id="t4ncua"
validate reader
validate duplicate/limit
lock available item
reserve exact item
create request
COMMIT
```

Fulfil:

```text id="fd2z11"
lock request
validate state
validate exact item
request → FULFILLED
item → BORROWED
create Borrowing
COMMIT
```

Return:

```text id="n6ebof"
lock Borrowing
lock PhysicalItem
set returnedAt
item → AVAILABLE
COMMIT
```

Terminal request:

```text id="5e71e2"
cancel/reject/expire
→ terminal request
→ release reservation
→ COMMIT
```

---

# 12. Concurrency Strategy

V2 uses:

```text id="ognuhh"
transactions
+
pessimistic locking where necessary
+
database uniqueness constraints
```

Concurrency protection is part of the core architecture rather than an afterthought.

---

# 13. Expiration

Request expiration remains an application-scheduled process.

The scheduler calls the same circulation rules and must not bypass lifecycle invariants.

---

# 14. Digital Content Module

Digital Content owns:

```text id="6ww3pa"
DigitalItem
content references
preview/full content
content delivery
```

It does not independently decide user entitlement.

---

# 15. Digital Access Architecture

Target:

```text id="8va8p9"
Digital endpoint
      |
      v
DigitalAccessService
      |
      +---- DigitalItem
      +---- Account / readerType
      +---- AccessSubscription (future)
      |
      v
DigitalAccessResult
```

Minimal V2 resolution:

```text id="aqv3lq"
PUBLIC
→ FULL

INSTITUTIONAL + Institutional Reader
→ FULL

otherwise + preview exists
→ PREVIEW

otherwise
→ NONE
```

Future personal-paid access can be added to this resolver without modifying physical circulation.

---

# 16. Digital Content Delivery

Generated demo PDFs are not the target architecture.

Target:

```text id="w8vrmi"
DigitalAccessService
        |
        v
DigitalContentService
        |
        v
ContentProvider / Storage Adapter
```

Possible future implementations include local/object storage, S3, Drive, or another content platform.

V2 only requires one clean boundary.

---

# 17. Paid Access Module

Paid Access owns:

```text id="kpy5t1"
AccessPlan
AccessSubscription
PaymentTransaction
PaymentProvider
```

Flow:

```text id="wo79zy"
create payment attempt
→ persist PENDING
→ provider redirect
→ callback/return
→ process result
→ SUCCESS / FAILED
→ activate subscription when appropriate
```

VNPay is the current provider adapter.

Paid Access may later supply digital entitlement.

It does not supply physical borrowing eligibility.

---

# 18. Resource Administration

Resource administration may orchestrate:

```text id="zmyd6p"
catalog metadata
physical-copy reconciliation
digital-item reconciliation
```

when these operations need to remain atomic.

A service should only be split when responsibilities become independently meaningful, not merely because a class becomes long.

---

# 19. Persistence Architecture

JPA repositories connect application services to PostgreSQL.

Custom queries are appropriate for:

- locking;
- efficient allocation;
- inventory views;
- pagination;
- aggregate queries.

Business policy should remain visible at the application/domain level rather than hidden in persistence code.

---

# 20. Database Migration

V2 moves from startup `schema.sql` repair toward:

```text id="q0euy9"
Flyway
→ ordered versioned migrations
```

Demo seed data remains separate from production schema migration.

---

# 21. Error Architecture

Backend errors should follow a stable contract such as:

```text id="lx7t7g"
status
code
message
timestamp
```

Frontend behavior should depend on stable error codes rather than exception-message text.

---

# 22. Configuration Architecture

Environment-specific configuration includes:

- database;
- CORS;
- cookie/security;
- circulation policies;
- frontend origin;
- metadata provider;
- payment provider;
- digital-content storage.

Production secrets remain external to source code.

---

# 23. Testing Architecture

V2 primarily uses:

```text id="kq0s4h"
service/domain tests
        |
        v
API/integration tests
        |
        v
selective PostgreSQL integration tests
```

Browser E2E is not required.

PostgreSQL-specific tests should cover behavior such as:

- pessimistic locks;
- concurrent allocation;
- partial unique indexes;
- schema migrations.

---

# 24. Deployment Architecture

Target deployment:

```text id="dw8n42"
Frontend static hosting
        |
        v
Spring Boot container
        |
        v
Managed PostgreSQL
```

External providers may include:

```text id="ypv9tc"
Google Books
VNPay
Digital storage provider
```

Render, Neon, or other vendors are deployment choices, not architectural dependencies.

---

# 25. V2 Dependency Rules

Allowed:

```text id="lpece0"
Frontend
→ Backend API

Controller
→ Service

Service
→ Repository

Circulation
→ Inventory

Digital Access
→ Digital Content

Digital Access
→ Paid Access (future)

Catalog
→ Metadata Provider

Paid Access
→ Payment Provider
```

Avoid:

```text id="yrmybs"
Repository
→ Service

Circulation
→ Payment Provider

Physical borrowing
→ AccessSubscription

provider-specific DTOs
→ leaked throughout domain
```

---

# 26. Main V1 → V2 Repairs

```text id="zr8blt"
BorrowService
→ remove Membership/Quota dependency

BorrowingQuotaPolicy
→ remove or replace with institutional circulation policy

MembershipService
→ reframe as PaidAccessService

MembershipPlan
→ AccessPlan

MembershipSubscription
→ AccessSubscription

DigitalAccessService
→ resolve public/institutional access
   instead of membership-only access

demo PDF generation
→ replace with content-delivery boundary

frontend runtime mocks
→ remove when obsolete

schema.sql startup repair
→ Flyway
```

---

# 27. Architecture Deliberately Not Added in V2

V2 does not introduce:

- microservices;
- event bus;
- generic entitlement engine;
- multi-tenant architecture;
- Institution service;
- Elasticsearch;
- distributed cache;
- CQRS;
- message queues;
- separate payment service;
- separate digital-content microservice.

These require concrete product or operational needs.

---

# 28. Architecture Overview

```text id="uq3ifu"
                        ┌─────────────────┐
                        │    React SPA    │
                        └────────┬────────┘
                                 │
                                 v
                     ┌────────────────────┐
                     │ Spring Web/Security│
                     └─────────┬──────────┘
                               │
        ┌──────────────────────┼─────────────────────┐
        │                      │                     │
        v                      v                     v
     Catalog               Circulation          Digital Access
        │                      │                     │
        │                      v                     v
        │                  Inventory          Digital Content
        │                                            │
        v                                            │
 Metadata Provider                                  │
                                                     │
                              Paid Access ───────────+
                                  │
                                  v
                           Payment Provider

                      Persistence
                           |
                           v
                       PostgreSQL
```

---

# 29. V2 Architecture Principles

1. Prefer a modular monolith before microservices.
2. The backend is the business source of truth.
3. Services own transactions and orchestration.
4. Repositories own persistence and queries, not policy.
5. The database protects appropriate invariants.
6. Circulation is independent from Payment.
7. Digital entitlement is independent from content storage.
8. External providers sit behind adapter boundaries.
9. Security is enforced on the backend.
10. Concurrency is a core design concern.
11. Complexity is introduced only when required.
12. V2 should remain easy to explain, maintain, and extend through vertical slices.