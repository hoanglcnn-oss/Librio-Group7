# Librio Sprint 3 Low-Level Design

This LLD closes the Sprint 3 boundaries for overdue presentation, return processing, protected digital access and aggregate resource administration. The implementation-oriented baseline remains linked in [sprint-3-implementation-design.md](sprint-3-implementation-design.md).

## US-13 / US-14 Boundary

US-13 manages resource metadata and aggregate access composition:

- Resource metadata: title, authors, description and category.
- Whether the resource has physical access.
- Desired total number of physical copies.
- Whether the resource has a digital-access marker.
- Increasing or decreasing total copies is reconciled through exact `physical_item` rows.
- When decreasing copies, only `AVAILABLE` items may be deleted.
- Items in `RESERVED` or `BORROWED` state must not be deleted because they represent active circulation commitments.

US-13 does not provide individual-item administration.

US-14 owns item-level inventory management:

- Barcode or accession number.
- Shelf/location.
- Inventory status such as `ACTIVE`, `LOST`, `DAMAGED` or `WITHDRAWN`.
- Changing the state of each individual physical item.
- Validation that inventory operations do not break an active request or borrowing.
- More detailed digital-item management if Sprint 4 expands beyond the current aggregate marker.

US-13 answers: "Which access forms does this resource have, and how many physical copies exist in total?"

US-14 answers: "Which exact copy is this, where is it located, and what inventory status does it have?"

Current Sprint 3 implementation follows this distinction: `ResourceAdminService` accepts aggregate `accessTypes`, `physical.totalCopies` and a digital marker, but it does not expose barcode, shelf/location, inventory status or per-item CRUD.

## Implemented Sprint 3 Entry Points

- Overdue and return: `BorrowService`, `ReaderBorrowingController`, `LibrarianBorrowingController`.
- Protected digital access: `DigitalAccessController`, `DigitalAccessService`, `SecurityConfig`.
- Aggregate resource administration: `LibrarianResourceController`, `ResourceAdminService`.
- Frontend pages/services: `MyLibraryPage.jsx`, `LibrarianRequestsPage.jsx`, `ResourceAdminPage.jsx`, `DemoActions.jsx`, `authApi.js`.

## Test Evidence

Sprint 3 E2E evidence is produced by Playwright under `frontend/test-results` and `frontend/playwright-report`. The runbook is [Sprint 3 E2E Runbook](../testing/sprint-3-e2e-runbook.md).
