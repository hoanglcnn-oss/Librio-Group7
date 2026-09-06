# Librio — Sprint 3 Software Requirements Specification

**Goal:** extend the working discovery and circulation system with overdue visibility, physical return, protected digital reading and aggregate resource administration.

## 1. Functional requirements

### US-10 — Overdue visibility

- `S3-OVD-01`: derive overdue when an unreturned borrowing is strictly before server time.
- `S3-OVD-02`: do not require persisted borrowing status for overdue.
- `S3-OVD-03`: show the server-derived value to reader and librarian.

### US-11 — Physical return

- `S3-RET-01`: only an authenticated librarian may return an item.
- `S3-RET-02`: atomically set `returnedAt` and move the exact item from `BORROWED` to `AVAILABLE`.
- `S3-RET-03`: repeated/competing returns must not release twice.
- `S3-RET-04`: missing, returned and inconsistent-item cases use stable codes.

### US-12 — Protected digital access

- `S3-DIG-01`: a digital marker indicates resource digital availability.
- `S3-DIG-02`: capability and content retrieval both require reader authorization.
- `S3-DIG-03`: generated demo PDF content is acceptable; durable storage and DRM are not required.

### US-13 — Aggregate resource administration

- `S3-ADM-01`: manage title, authors, description and optional category.
- `S3-ADM-02`: generate resource/item identifiers server-side.
- `S3-ADM-03`: increasing physical total creates available copies.
- `S3-ADM-04`: decreasing total removes only available copies.
- `S3-ADM-05`: unsafe reduction fails atomically.
- `S3-ADM-06`: at most one digital marker exists per resource.
- `S3-ADM-07`: barcode, location, inventory condition and item CRUD belong to US-14.

## 2. Quality and compatibility

- Backend enforces authorization; UI hiding is insufficient.
- Mutations return stable JSON errors and no partial state.
- Sprint 1 discovery and Sprint 2 authentication/circulation remain compatible.
- Real-stack E2E uses the frontend, Spring backend and a dedicated test database with mocks disabled.
- Demo state is resettable for repeatable review.

## 3. Out of scope

US-14 item inventory, fines/payment, renewal, waitlist, durable digital storage/upload, signed URLs, DRM, streaming and resource deletion lifecycle.

## 4. Traceability

| Requirement | Design | Verification |
|---|---|---|
| `S3-OVD-*`, `S3-RET-*` | Sprint 3 LLD §§2–3 | Unit/controller tests and Playwright return flow |
| `S3-DIG-*` | Sprint 3 LLD §4 | Security tests and Playwright digital flow |
| `S3-ADM-*` | Sprint 3 LLD §§5–6 | Unit/controller tests and Playwright admin flow |

API details are canonical in the [Sprint 3 API Contract](../lld/api-contracts/sprint-3-api.md).
