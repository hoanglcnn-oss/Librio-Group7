# Librio — Sprint 3 Low-Level Design

**Scope:** US-10 overdue visibility, US-11 physical return, US-12 protected digital access and US-13 aggregate resource administration. US-14 is a boundary only and remains deferred.

This is the single canonical Sprint 3 LLD, consolidating the former implementation-design file.

## 1. Design principles

- Backend time, principal, identifiers and availability are authoritative.
- State-changing circulation operations are transactional and lock affected rows.
- Sprint 1 public discovery and Sprint 2 circulation contracts remain compatible.
- Reader DTOs exclude internal item and ownership identifiers.

## 2. Overdue presentation (US-10)

A borrowing is active while `returned_at IS NULL`. Overdue is derived at response time:

```text
overdue = returnedAt == null && dueAt < serverNow
```

`dueAt == serverNow` is not overdue. Reader and librarian DTOs receive the additive `overdue` boolean. The legacy `OVERDUE` physical-item value remains schema-compatible, but Sprint 3 does not write it.

## 3. Physical return (US-11)

`POST /librarian/borrowings/{borrowingId}/return` requires `ROLE_LIBRARIAN` and CSRF. In one transaction, `BorrowService` locks the borrowing, rejects a prior return, locks its exact item, verifies `BORROWED`, persists server `returned_at`, then changes the item to `AVAILABLE`.

| Condition | Result |
|---|---|
| Missing borrowing | `404 BORROWING_NOT_FOUND` |
| Already returned | `409 BORROWING_ALREADY_RETURNED` |
| Item inconsistent | `409 BORROWING_ITEM_CONFLICT` |

Locking plus persisted `returned_at` gives one-winner semantics. E2E proves successful and repeated return; a simultaneous two-client return remains a test refinement.

## 4. Protected digital access (US-12)

A `digital_item` row marks digital availability. Both endpoints require an active reader session:

- `GET /resources/{id}/digital-access` returns a backend content URL.
- `GET /resources/{id}/digital-content` re-authorizes and returns `application/pdf`.

Sprint 3 generates a demo PDF to prove access control. Durable storage, upload, signed URLs, DRM and streaming are deferred.

## 5. Aggregate resource administration (US-13)

`ROLE_LIBRARIAN` may create/read/update a resource aggregate: metadata, access types, desired physical total and digital marker. IDs are server-generated. Authors are persisted comma-separated but exposed as a JSON array.

- Increasing `totalCopies` creates `AVAILABLE` item rows.
- Decreasing it deletes only `AVAILABLE` rows.
- If insufficient available rows exist, `409 RESOURCE_IN_USE` rolls back the operation.
- Removing physical access reconciles the desired total to zero.
- Digital access creates/removes the one-to-one `digital_item` marker.

## 6. US-13 / US-14 boundary

US-13 answers: **what is the resource, which access forms exist, and how many physical copies exist?**

US-14 answers: **which exact copy is this, where is it, and what inventory condition does it have?** Barcode/accession number, shelf/location, `LOST`/`DAMAGED`/`WITHDRAWN`, per-item CRUD and inventory-safe transitions are deferred to US-14.

## 7. Entry points and references

| Slice | Backend | Frontend |
|---|---|---|
| Overdue/return | `BorrowService`, borrowing controllers | My Library, librarian circulation |
| Digital | `DigitalAccessController/Service`, security | Resource detail/demo action |
| Administration | `LibrarianResourceController`, `ResourceAdminService` | Resource Admin |

Mocks are for isolated UI work only and must be disabled for real-stack E2E and production. See the [Sprint 3 API Contract](api-contracts/sprint-3-api.md) and [E2E Runbook](../testing/sprint-3-e2e-runbook.md).
