# Librio — Sprint 4 API Contract

Protected mutations use the existing session-cookie and CSRF contract. JSON errors retain `{ status, code, message, timestamp }`; IDs and timestamps are server-owned. Endpoints under `/librarian/**` require `ROLE_LIBRARIAN`.

## Inventory and collection cockpit

- `GET /librarian/inventory/summary` — librarian; summary metrics (`totalCopies`, `availableCopies`, `reservedCopies`, `borrowedCopies`, `attentionCopies`).
- `GET /librarian/physical-items?q=&inventoryStatus=&circulationStatus=&needsAttention=&page=&size=` — librarian; paginated physical copy search and filtering.
- `POST /librarian/resources/{resourceId}/physical-items` — librarian + CSRF; creates physical copy (`barcode`, `location`). Defaults `inventoryStatus=ACTIVE`, `circulationStatus=AVAILABLE`.
- `PUT /librarian/physical-items/{id}` — librarian + CSRF; updates `barcode`, `location`, or `inventoryStatus` (`ACTIVE`, `LOST`, `DAMAGED`, `WITHDRAWN`). `circulationStatus` is server-managed.

```json
{
  "barcode": "LIB-CC-003",
  "location": "A-04",
  "inventoryStatus": "DAMAGED"
}
```

Items with active circulation (`RESERVED` or `BORROWED`) cannot transition to `LOST`, `DAMAGED`, or `WITHDRAWN`. `WITHDRAWN` copies are non-borrowable without hard deletion.

| Status | Code |
|---:|---|
| `404` | `PHYSICAL_ITEM_NOT_FOUND` |
| `409` | `DUPLICATE_ITEM_BARCODE` |
| `409` | `ACTIVE_CIRCULATION_CONFLICT` / `INVALID_INVENTORY_TRANSITION` |

## Membership subscription

- `GET /membership/plans` — public; returns active plans (`MONTHLY`, `YEARLY`).
- `GET /me/membership` — reader; returns membership status (`NONE`, `PENDING`, `ACTIVE`, `FAILED`, `EXPIRED`).
- `POST /me/membership-subscriptions` — reader + CSRF; creates a subscription (`planId`).
- `POST /me/membership-subscriptions/{id}/mock-payment` — reader + CSRF; processes payment (`outcome`: `SUCCESS` | `FAILED`), transitioning status to `ACTIVE` or `FAILED`.

```json
{
  "outcome": "SUCCESS"
}
```

Requires account eligibility, valid active plan, and no existing active/pending subscription. `EXPIRED` status is derived server-side from time.

| Status | Code |
|---:|---|
| `403` | `MEMBERSHIP_NOT_ELIGIBLE` |
| `404` | `MEMBERSHIP_PLAN_NOT_FOUND` / `SUBSCRIPTION_NOT_FOUND` |
| `409` | `ACTIVE_MEMBERSHIP_EXISTS` / `PENDING_SUBSCRIPTION_EXISTS` / `INVALID_SUBSCRIPTION_STATE` |

## Google Books ISBN lookup & Resource metadata

- `GET /librarian/book-metadata/lookup?isbn={isbn}` — librarian; normalizes ISBN-10/13 and fetches metadata from Google Books. Read-only.
- `POST /librarian/resources`, `PUT /librarian/resources/{id}` — librarian + CSRF; existing endpoints expanded with `isbn`, `coverImageUrl`, `metadataSource` (`MANUAL` | `GOOGLE_BOOKS`), and `externalSourceId`.

```json
{
  "title": "Clean Code",
  "authors": ["Robert C. Martin"],
  "isbn": "9780132350884",
  "coverImageUrl": "https://...",
  "metadataSource": "GOOGLE_BOOKS",
  "externalSourceId": "vol-123"
}
```

ISBN is validated, normalized to ISBN-13, and enforced unique when present. Duplicate ISBN returns `409 RESOURCE_ISBN_EXISTS`; invalid ISBN returns `400 INVALID_ISBN`; lookup timeout returns `504 BOOK_METADATA_LOOKUP_TIMEOUT`.

## Data schema updates

| Table | New Columns / Constraints |
|---|---|
| `physical_item` | `barcode` (VARCHAR, UNIQUE), `location` (VARCHAR), `inventory_status` (`ACTIVE`, `LOST`, `DAMAGED`, `WITHDRAWN`) |
| `resource` | `isbn` (VARCHAR, UNIQUE nullable), `cover_image_url`, `metadata_source` (`MANUAL` \| `GOOGLE_BOOKS`), `external_source_id` |
| `accounts` | `membership_eligible` (BOOLEAN DEFAULT false) |
| `membership_plan` | `code`, `name`, `duration_months`, `price_amount`, `currency`, `monthly_borrow_quota`, `active` |
| `membership_subscription` | `account_id`, `plan_id`, `status` (`PENDING`, `ACTIVE`, `FAILED`), `starts_at`, `expires_at` |
