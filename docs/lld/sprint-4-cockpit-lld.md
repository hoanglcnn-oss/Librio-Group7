# Librio — Sprint 4 Collection Cockpit Low-Level Design

**Scope:** US-15 Collection Cockpit, specifically T-152 inventory summary, paginated physical-copy query and active-operation read model.
**Source:** T-151, T-152, Sprint 4 API contract.
**Related:** `sprint-4-inventory-lld.md`, `api-contracts/sprint-4-api.md`.

## 1. Design goals

The Collection Cockpit gives librarians one read model for inventory state, circulation context and records requiring attention. The backend is authoritative for summary counts, borrowability, active operation state, attention detection and overdue state. The frontend renders these values and must not recompute them. Sprint 4 does not add arbitrary sorting, analytics, reporting or bulk inventory operations.

## 2. Cockpit read model

### 2.1 Summary
`GET /librarian/inventory/summary` returns:
- `totalCopies`: all non-`WITHDRAWN` physical copies.
- `availableCopies`: `ACTIVE + AVAILABLE`.
- `reservedCopies`: non-withdrawn copies with `circulationStatus = RESERVED`.
- `borrowedCopies`: non-withdrawn copies with `circulationStatus = BORROWED`.
- `attentionCopies`: distinct non-withdrawn copies matching the attention predicate.

### 2.2 Physical-copy result
Each physical-copy item exposes:
```json
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
```
`borrowable`, `needsAttention`, `attentionReasons` and `overdue` are server-derived.

## 3. Core rules

### 3.1 Search and filtering
- `q` is trimmed; blank input means no search filter. Case-insensitive search matches barcode, location, resource title, or resource authors.
- Supplied filters are combined with `AND`: `inventoryStatus`, `circulationStatus`, `needsAttention`.
- Pagination defaults: `page = 0`, `size = 20` (where `1 <= size <= 100`). `page` is zero-based.

### 3.2 Active operation
A physical copy exposes at most one normal active operation:
1. active unreturned `BORROWING`;
2. otherwise active `BORROW_REQUEST` in `REQUESTED` or `READY_FOR_PICKUP`;
3. otherwise `null`.

`BORROW_REQUEST` projection includes: `id`, `type`, `status`, `reader`, `requestedAt`, `statusUpdatedAt`, `expiresAt`.
`BORROWING` projection includes: `id`, `type`, `borrowRequestId`, `reader`, `borrowedAt`, `dueAt`, `overdue`.

If both an active borrowing and active borrow request exist for the same copy, the copy is marked with `CIRCULATION_DATA_MISMATCH` rather than being treated as healthy data.

### 3.3 Attention predicate
Stable attention reason codes: `INVENTORY_LOST`, `INVENTORY_DAMAGED`, `LOCATION_UNASSIGNED`, `BORROWING_OVERDUE`, `CIRCULATION_DATA_MISMATCH`.
A non-withdrawn copy needs attention when at least one reason applies. `needsAttention=true` filtering and `attentionCopies` summary must use the same predicate.

### 3.4 Default ordering
Default result ordering prioritizes operational attention:
1. `needsAttention = true`;
2. overdue borrowing;
3. inventory `LOST` or `DAMAGED`;
4. `LOCATION_UNASSIGNED`;
5. remaining records;
6. `physicalItem.id ASC` as stable tie-breaker.

Explicit client-defined sorting is outside Sprint 4 scope.

## 4. Persistence and query design

T-152 introduces no new persisted cockpit state. Summary values, borrowability, attention state and overdue state are derived from `physical_item`, active `borrow_request`, active `borrowing`, associated `resource`, and associated reader account.

The implementation must avoid one request/borrowing lookup per physical item. The backend should use joined projection queries or bounded bulk queries for the current physical-item page, then assemble the cockpit DTO in memory. Existing database invariants remain authoritative.

## 5. API impact

Endpoints: `GET /librarian/inventory/summary`, `GET /librarian/physical-items?q=&inventoryStatus=&circulationStatus=&needsAttention=&page=&size=`.
Paginated response shape:
```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```
No new mutation endpoint is introduced by T-152.

## 6. Design decisions

- **D152-01 — One operational cockpit read model**: Return inventory, resource, attention and active-operation context in one librarian DTO to prevent extra N+1 requests.
- **D152-02 — Server-derived attention**: Backend derives attention reasons to maintain consistency across summary, filtering, and UI.
- **D152-03 — Stable operational ordering**: Attention records appear before healthy records with `physicalItem.id ASC` as the final tie-breaker for deterministic pagination.

## 7. Implementation boundary and completion criteria

- **T-153**: implements inventory summary query, physical-copy paginated search/filter query, active operation projection, attention derivation, and backend tests.
- **T-154**: implements the responsive UI using this read model.
- **T-152**: complete when summary semantics, filters, pagination, ordering, active-operation projection, attention derivation and N+1 avoidance strategy are defined.
