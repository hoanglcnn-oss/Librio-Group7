# Librio — High-Level Design & System Architecture

**Version:** Living HLD through Sprint 3
**Detailed design:** [Sprint 3 LLD](../lld/sprint-3-lld.md) | **Diagram:** [component-diagram.mmd](component-diagram.mmd)

## 1. Architecture

Librio is a React SPA backed by a Spring Boot modular monolith and one PostgreSQL database. The deployment keeps cross-domain changes transactional while module boundaries preserve clarity.

| Layer | Technology | Responsibility |
|---|---|---|
| Client | React, Vite, React Router | Discovery, authentication, My Library, librarian circulation/admin |
| Security | Spring Security | Session, CSRF, role authorization and JSON 401/403 |
| Application | Spring Boot controllers/services | Catalog, circulation, digital access and administration rules |
| Persistence | Spring Data JPA, PostgreSQL | Relational state, constraints, row locking and derived queries |

## 2. Module boundaries

- **Account & Access:** account identity, BCrypt credential, reader/librarian role and session principal.
- **Catalog:** resource metadata, exact physical items, digital marker and derived availability.
- **Circulation:** request lifecycle, reservation, checkout, overdue projection and atomic return.
- **Digital Access:** reader-authorized capability/content delivery. Sprint 3 content is generated demo PDF.
- **Resource Administration:** librarian aggregate metadata/access maintenance and safe physical-copy reconciliation.

The client never supplies authoritative reader identity, availability, timestamps or generated IDs.

## 3. Key workflows

1. **Discovery:** public browse/search/detail obtains backend-derived physical/digital availability.
2. **Authentication:** CSRF-protected login creates a server session; `/auth/me` restores UI state.
3. **Request and checkout:** reader reserves an exact available item; librarian prepares and fulfils it into a borrowing.
4. **Overdue and return:** backend derives overdue from due time; librarian return closes the borrowing and releases its exact item in one transaction.
5. **Digital reading:** reader obtains a capability, then the protected content endpoint re-authorizes and serves PDF.
6. **Resource administration:** librarian creates/updates an aggregate; the backend reconciles access markers and only removes available copies.

## 4. Security, transaction and concurrency

- Session principal is the only ownership source; reader/librarian routes use role checks.
- State-changing protected requests require CSRF.
- Create/reserve, release, fulfil, return and aggregate reconciliation are atomic transactions.
- Row locking and unique/partial indexes enforce one-winner behavior and prevent duplicate active commitments.
- Overdue and availability are derived rather than duplicated as mutable summary state.

## 5. Decisions

| ID | Decision |
|---|---|
| `HLD-01` | React SPA + Spring Boot modular monolith + PostgreSQL |
| `HLD-02` | REST JSON without an `/api` prefix |
| `HLD-03` | Server session + HttpOnly cookie; no JWT |
| `HLD-04` | Central Spring Security boundary with CSRF |
| `HLD-05` | A `READER` account is the Sprint 1–3 reader identity |
| `HLD-06` | Physical/digital availability is derived; no availability table |
| `HLD-07` | Circulation and copy reconciliation use atomic transactions |
| `HLD-08` | Single-site production and Vite development proxy |
| `HLD-09` | Overdue is derived from `returnedAt`, `dueAt` and server time |
| `HLD-10` | Return releases the exact item atomically |
| `HLD-11` | Sprint 3 proves protected digital access with generated PDF |
| `HLD-12` | US-13 owns aggregate copies; US-14 owns per-item inventory |

## 6. Traceability

| Area | Requirements | Detailed design |
|---|---|---|
| Discovery | Sprint 1 SRS | Sprint 1 LLD/API |
| Authentication/circulation | Sprint 2 SRS | Sprint 2 Auth/Borrow LLD and API |
| Overdue, return, digital, admin | Sprint 3 SRS | Sprint 3 LLD/API |
| Persistence | Sprint 1–3 | Database specification, SQL and ERD |
