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

### Collection Cockpit read semantics

Summary semantics:

- `totalCopies` excludes `WITHDRAWN` copies and represents the current collection.
- `availableCopies` counts only `ACTIVE + AVAILABLE` copies.
- `reservedCopies` and `borrowedCopies` count the corresponding circulation states among non-withdrawn copies.
- `attentionCopies` counts distinct non-withdrawn copies that are `LOST`, `DAMAGED`, have location `UNASSIGNED`, have an overdue active borrowing, or have a circulation-data mismatch.

Physical-copy query rules:

- `q` is trimmed; blank means no search filter. It matches barcode, location, resource title, or authors case-insensitively.
- Supplied filters are combined with `AND`.
- Pagination defaults to `page=0`, `size=20`; `page` is zero-based and `size` must be `1..100`.
- Results prioritize records requiring attention and use `physicalItem.id ASC` as the stable final tie-breaker.
- `needsAttention` uses the same derived predicate as `attentionCopies`.

```json
{
  "items": [
    {
      "id": 10003,
      "barcode": "LIB-10003",
      "location": "A-04",
      "inventoryStatus": "ACTIVE",
      "circulationStatus": "BORROWED",
      "borrowable": false,
      "needsAttention": true,
      "attentionReasons": ["BORROWING_OVERDUE"],
      "resource": {
        "id": 1000,
        "title": "Clean Code",
        "authors": ["Robert C. Martin"]
      },
      "activeOperation": {
        "type": "BORROWING",
        "id": 912,
        "borrowRequestId": 731,
        "reader": {
          "id": 45,
          "displayName": "Reader One"
        },
        "borrowedAt": "2026-08-20T09:00:00+07:00",
        "dueAt": "2026-09-03T09:00:00+07:00",
        "overdue": true
      }
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

`activeOperation` is `null` when the copy has no active circulation operation.

Otherwise:

- `BORROW_REQUEST`: `id`, `type`, `status`, `reader`, `requestedAt`, `statusUpdatedAt`, `expiresAt`.
- `BORROWING`: `id`, `type`, `borrowRequestId`, `reader`, `borrowedAt`, `dueAt`, `overdue`.

Stable attention reason codes:

- `INVENTORY_LOST`
- `INVENTORY_DAMAGED`
- `LOCATION_UNASSIGNED`
- `BORROWING_OVERDUE`
- `CIRCULATION_DATA_MISMATCH`

`borrowable`, `needsAttention`, `attentionReasons`, and `overdue` are server-derived; the client must not recompute them.

### Collection Cockpit implementation / verification note

US-15 Collection Cockpit implementation is complete through T-156.

Verified integration behavior includes:

- Cockpit filters and pagination remain server-driven;
- URL query state covers `q`, `inventoryStatus`, `circulationStatus`, `needsAttention`, `page`, and `size`;
- changing filter criteria resets pagination appropriately;
- malformed / unsupported page and page-size URL values are normalized before backend requests;
- manual refresh reloads both summary and the current physical-item page;
- active-operation display includes `statusUpdatedAt` for borrow requests and `borrowRequestId` for borrowings;
- Cockpit navigation reuses existing librarian resource / circulation workflows rather than introducing duplicate mutation APIs.

Automated verification is covered by the T-156 Cockpit regression tests. Deployed end-to-end verification is recorded separately as Sprint 4 evidence.

### Inventory implementation / verification note

US-14 inventory API implementation is complete through T-147.

Verified invariants include:

- physical-copy create defaults to `ACTIVE + AVAILABLE`;
- `circulationStatus` cannot be modified through the librarian inventory update API;
- only `ACTIVE + AVAILABLE` copies are borrowable / counted as available;
- `ACTIVE -> LOST / DAMAGED / WITHDRAWN` is rejected while circulation is `RESERVED` or `BORROWED`;
- unsupported non-ACTIVE -> non-ACTIVE transitions return `INVALID_INVENTORY_TRANSITION`;
- barcode and location remain required copy-identity fields;
- inventory mutations are followed by server-backed resource / availability refresh in the librarian workflow.

Automated verification is covered by the T-147 inventory regression suite. Deployed end-to-end verification is recorded separately as Sprint 4 evidence.

## Membership subscription

- `GET /membership/plans` — public; returns active membership plans.
- `GET /me/membership` — reader; returns effective membership derived from subscription timestamps and latest payment context.
- `POST /me/membership-payments` — reader + CSRF; performs the Sprint 4 mock payment attempt for a selected plan.

```json
{
  "planId": 1,
  "outcome": "SUCCESS"
}
```

Payment lifecycle:

```text
FAILED
→ persist failed PaymentTransaction
→ no MembershipSubscription

SUCCESS
→ persist successful PaymentTransaction
→ create MembershipSubscription
→ effective membership becomes ACTIVE
```

A subscription is never created before successful payment.

Effective membership states are:

- `NONE` — no subscription exists.
- `ACTIVE` — `startsAt <= serverNow < expiresAt`.
- `EXPIRED` — `expiresAt <= serverNow`.

`PENDING` and `FAILED` are payment lifecycle concerns and are not persisted membership-subscription states.

Requires account eligibility, a valid active plan and no existing active membership.

Membership expiry blocks new membership-gated borrowing but does not mutate an existing active borrowing.

| Status     | Code                                                          |
| ---------- | ------------------------------------------------------------- |
| `400`      | `INVALID_PAYMENT_OUTCOME`                                     |
| `403`      | `MEMBERSHIP_NOT_ELIGIBLE`                                     |
| `404`      | `MEMBERSHIP_PLAN_NOT_FOUND`                                   |
| `409`      | `ACTIVE_MEMBERSHIP_EXISTS` / `MEMBERSHIP_ACTIVATION_CONFLICT` |

### Membership implementation / verification note

US-16 membership implementation is complete through T-167.

Implemented behavior includes:

- effective membership state is derived server-side as `NONE`, `ACTIVE`, or `EXPIRED`;
- failed payment persists a failed `PaymentTransaction` and creates no subscription;
- successful payment persists the transaction and creates a `MembershipSubscription`;
- membership ownership is derived from the authenticated principal;
- membership activation is protected by a backend transactional/account-lock boundary;
- expiry does not mutate an already active borrowing;
- the reader UI refreshes authoritative membership state after payment and protects against stale async responses.

Verification coverage added in T-167 includes:

- failed-payment retry followed by successful activation;
- expired membership while an existing borrowing remains unchanged;
- competing activation using two concurrent worker threads against the same account and plan;
- persisted final-state verification that competing activation does not create multiple membership subscriptions.

Backend verification tests have been added but are not execution-proven in the current local environment because Maven and Maven Wrapper are unavailable. Deployed end-to-end verification is recorded separately.

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

ISBN is validated, normalized to ISBN-13, and enforced unique when present.

| Status | Code |
|---:|---|
| `400` | `INVALID_ISBN` |
| `404` | `BOOK_METADATA_NOT_FOUND` |
| `409` | `RESOURCE_ISBN_EXISTS` |
| `502` | `BOOK_METADATA_PROVIDER_ERROR` |
| `504` | `BOOK_METADATA_LOOKUP_TIMEOUT` |

### Google Books implementation / verification note

US-19 Google Books metadata integration is complete through T-198.

Implemented behavior includes:

- the browser calls the Librio backend and never calls Google Books directly;
- ISBN-10/13 input is normalized to canonical ISBN-13;
- provider candidates must contain the exact normalized ISBN before metadata is accepted;
- lookup returns editable prefill only and never auto-saves a Resource;
- manual Resource entry remains available when lookup fails;
- persisted Resource metadata supports `isbn`, `coverImageUrl`, `metadataSource`, and `externalSourceId`;
- duplicate canonical ISBN returns `RESOURCE_ISBN_EXISTS`.

Verification coverage added in T-198 includes:

- exact provider match;
- mismatched candidate and empty result handling;
- timeout and provider-error mapping;
- metadata mapping with optional provider fields;
- Spring Security filter-chain coverage for unauthenticated, reader, and librarian access;
- frontend metadata-prefill transformation while preserving unrelated form fields.

Backend verification tests have been added but are not execution-proven in the current local environment because Maven and Maven Wrapper are unavailable.

## Data schema updates

| Table | New Columns / Constraints |
|---|---|
| `physical_item` | `barcode` (VARCHAR, UNIQUE), `location` (VARCHAR), `inventory_status` (`ACTIVE`, `LOST`, `DAMAGED`, `WITHDRAWN`) |
| `resource` | `isbn` (VARCHAR, UNIQUE nullable), `cover_image_url`, `metadata_source` (`MANUAL` \| `GOOGLE_BOOKS`), `external_source_id` |
| `accounts` | `membership_eligible` (BOOLEAN NOT NULL DEFAULT false) |
| `membership_plan` | `code`, `name`, `duration_months`, `price_amount`, `currency`, `monthly_borrow_quota`, `active` |
| `payment_transaction` | `account_id`, `plan_id`, `amount`, `currency`, `status` (`SUCCESS` \| `FAILED`), `created_at`, `completed_at` |
| `membership_subscription` | `account_id`, `plan_id`, `payment_transaction_id` (UNIQUE), `starts_at`, `expires_at` |
