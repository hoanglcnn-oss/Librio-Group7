# Librio — Sprint 4 Physical Inventory LLD

## 1. Scope

This document defines the Sprint 4 technical design for physical-copy inventory administration under US-14. The design covers physical-copy schema changes, separation of inventory and circulation states, migration of existing physical-item data, barcode and location backfill, valid inventory state transitions, borrowability derivation, and active-circulation conflict rules. Implementation of the migration and APIs is handled by subsequent Sprint 4 tasks.

## 2. Existing Model

Before Sprint 4, `PhysicalItem` contains `id`, `resource`, and `status` (`AVAILABLE`, `RESERVED`, `BORROWED`, `OVERDUE`). This single `status` field mixes circulation state with inventory condition.

The existing database already protects important circulation invariants:
- At most one active request for a physical item.
- At most one active borrowing for a physical item.
- Overdue derived from `Borrowing.dueAt` and `returnedAt`.

Sprint 4 must preserve these existing invariants.

## 3. Target PhysicalItem Model

Sprint 4 separates physical-copy inventory condition from circulation state:

```text
PhysicalItem
├── id
├── resource
├── barcode
├── location
├── inventoryStatus (ACTIVE, LOST, DAMAGED, WITHDRAWN)
└── circulationStatus (AVAILABLE, RESERVED, BORROWED)
```

### 3.1 Barcode
`barcode` uniquely identifies one physical copy (required, unique across all physical items, server-enforced constraint, duplicates rejected). Barcode identifies the individual copy, while `resourceId` identifies the bibliographic resource.

### 3.2 Location
`location` represents the known physical location or shelving identifier (required for every item after migration, editable by librarian). Legacy copies without historical location use `UNASSIGNED` rather than fabricating shelf data.

### 3.3 Inventory Status
`inventoryStatus` represents whether the physical copy belongs to the usable collection:
- `ACTIVE`: Copy is part of the usable collection.
- `LOST`: Copy cannot currently be located.
- `DAMAGED`: Copy is not usable for borrowing due to condition.
- `WITHDRAWN`: Copy removed from active collection but retained for historical consistency.

### 3.4 Circulation Status
`circulationStatus` represents the server-managed circulation lifecycle (`AVAILABLE`, `RESERVED`, `BORROWED`). The inventory update API must not directly mutate this field.

`OVERDUE` is not retained as a status value. Instead, `overdue = borrowing.returnedAt == null AND borrowing.dueAt < currentTime`. An overdue copy remains `circulationStatus = BORROWED`, avoiding multiple sources of truth.

## 4. Borrowability

Borrowability is derived and not stored in the database. A physical copy is borrowable if and only if `inventoryStatus == ACTIVE` AND `circulationStatus == AVAILABLE`.

| Inventory | Circulation | Borrowable |
| --- | --- | ---: |
| ACTIVE | AVAILABLE | Yes |
| ACTIVE | RESERVED | No |
| ACTIVE | BORROWED | No |
| LOST | AVAILABLE | No |
| DAMAGED | AVAILABLE | No |
| WITHDRAWN | AVAILABLE | No |

## 5. Inventory State Transitions

| Current | Target | Allowed |
| --- | --- | ---: |
| ACTIVE | LOST / DAMAGED / WITHDRAWN | Yes, if no active circulation |
| LOST / DAMAGED / WITHDRAWN | ACTIVE | Yes |
| Any state | Same state | Allowed (idempotent / no-op) |

Transitions between non-ACTIVE states are not required for Sprint 4 MVP (restoring to `ACTIVE` first is required).

## 6. Active Circulation Conflict

Inventory administration must not invalidate an active circulation workflow (`RESERVED` or `BORROWED`). While active circulation exists, transitioning `ACTIVE` → `LOST / DAMAGED / WITHDRAWN` is rejected with `409 ACTIVE_CIRCULATION_CONFLICT`. The system must not automatically cancel a reservation or borrowing.

## 7. WITHDRAWN and Deletion

Sprint 4 does not hard-delete physical copies. Removal uses `inventoryStatus = WITHDRAWN`. The database row is retained to preserve referential integrity and circulation history.

## 8. Legacy Migration

### 8.1 Strategy
Expand → backfill → validate → constrain:
1. Add new columns without final `NOT NULL` constraints.
2. Backfill existing physical items.
3. Validate migrated data.
4. Apply required constraints and indexes.
5. Remove or replace the legacy status representation.

### 8.2 Backfill Rules
- **Inventory Status**: All existing items receive `inventory_status = ACTIVE`.
- **Circulation Status**: `AVAILABLE` → `AVAILABLE`, `RESERVED` → `RESERVED`, `BORROWED`/`OVERDUE` → `BORROWED`.
- **Barcode**: Deterministic `LIB-<physical_item.id>` (e.g. `id=10000` → `LIB-10000`).
- **Location**: All existing items receive `location = UNASSIGNED`.

## 9. Database Constraints

```text
barcode NOT NULL UNIQUE
location NOT NULL
inventory_status NOT NULL IN (ACTIVE, LOST, DAMAGED, WITHDRAWN)
circulation_status NOT NULL IN (AVAILABLE, RESERVED, BORROWED)
```

Existing circulation invariants remain: one active request per copy, one active borrowing per copy. Queries filter by `inventory_status = ACTIVE AND circulation_status = AVAILABLE`.

## 10. API Impact

Endpoints expose both dimensions separately:
```json
{
  "id": 10000,
  "resourceId": 1000,
  "barcode": "LIB-10000",
  "location": "A-04",
  "inventoryStatus": "ACTIVE",
  "circulationStatus": "AVAILABLE"
}
```
Librarian updates may modify `barcode`, `location`, `inventoryStatus`, but never `circulationStatus`.

## 11. Error Rules

| Condition | HTTP | Code |
| --- | ---: | --- |
| Physical copy does not exist | 404 | `PHYSICAL_ITEM_NOT_FOUND` |
| Barcode already exists | 409 | `DUPLICATE_ITEM_BARCODE` |
| Conflict with active circulation | 409 | `ACTIVE_CIRCULATION_CONFLICT` |
| Unsupported inventory transition | 409 | `INVALID_INVENTORY_TRANSITION` |

## 12. Design Decisions

- **D142-01 — Separate inventory & circulation state**: Avoids status combinatorial explosion and isolates domain concerns.
- **D142-02 — Derive overdue**: Eliminates dual sources of truth for time-dependent state.
- **D142-03 — Deterministic legacy barcode**: Uses `LIB-<id>` for safe, unique backfill.
- **D142-04 — Unknown legacy location**: Uses `UNASSIGNED` instead of fabricating data.
- **D142-05 — Safe migration sequence**: Expand → backfill → validate → constrain.
- **D142-06 — Derived borrowability**: Borrowable iff `ACTIVE + AVAILABLE`.
- **D142-07 — Reject conflicting transitions**: Rejects state changes on items with active circulation (`409 ACTIVE_CIRCULATION_CONFLICT`).
- **D142-08 — Soft withdrawal**: Uses `WITHDRAWN` instead of hard deletion to protect history.

## 13. Implementation Status & Verification

US-14 physical inventory implementation is complete through T-147.

### 13.1 Implemented

- **T-143 — Schema & migration**
  - `PhysicalItem` now separates `inventoryStatus` and `circulationStatus`.
  - `barcode` and `location` are persisted and constrained.
  - Existing rows are migrated using deterministic barcode backfill and `UNASSIGNED` location.
  - Legacy `OVERDUE` circulation state is normalized to `BORROWED`; overdue remains derived from borrowing timestamps.

- **T-144 — Physical-copy backend**
  - Librarian create/update APIs enforce copy identity and inventory transition rules.
  - Create defaults to `inventoryStatus = ACTIVE` and `circulationStatus = AVAILABLE`.
  - `circulationStatus` remains server-managed.
  - Duplicate barcode and active-circulation conflicts are normalized to stable API errors.
  - Update operations use the backend concurrency/locking boundary defined for physical-copy mutation.

- **T-145 — Physical-copy administration UI**
  - Librarians can create physical copies and edit barcode, location, and inventory status.
  - Circulation status is display-only.
  - The UI guards obvious invalid inventory changes while retaining the backend as the source of truth.
  - Physical-copy loading follows the Cockpit pagination contract (`size <= 100`) and resolves copies by persisted resource identity.

- **T-146 — Resource / availability refresh integration**
  - Successful physical-copy mutation refreshes the physical-copy list from the server.
  - Managed resource / availability data is refreshed from the backend rather than patched client-side.
  - Physical-item refresh does not overwrite unsaved Resource Admin form edits.
  - Editable resource form hydration and server-derived availability snapshot refresh are intentionally separated.

### 13.2 Automated Verification — T-147

Automated inventory verification covers:

- allowed transitions:
  - `ACTIVE -> LOST / DAMAGED / WITHDRAWN`
  - `LOST / DAMAGED / WITHDRAWN -> ACTIVE`
  - same-state idempotent transitions
- rejection of unsupported non-ACTIVE -> non-ACTIVE transitions
- `RESERVED` and `BORROWED` active-circulation conflict handling
- derived availability where only `ACTIVE + AVAILABLE` is borrowable
- required barcode and location
- server defaults `ACTIVE + AVAILABLE`
- server ownership of `circulationStatus`
- physical-copy history / circulation integrity
- managed-resource availability refresh after inventory mutation

Manual end-to-end verification remains part of the deployed Sprint 4 validation flow.

### 13.3 Completion Criteria

US-14 inventory implementation is considered complete when:

- physical copies have stable unique identity and location;
- inventory state is separated from circulation state;
- borrowability and overdue remain server-derived;
- active circulation cannot be invalidated by inventory administration;
- withdrawn copies retain historical references instead of being hard-deleted;
- librarian create/edit workflow reflects persisted server state;
- resource availability refreshes after inventory mutation without discarding unrelated unsaved form edits;
- automated verification covers the transition and conflict rules above.

Final Sprint 4 evidence / deployed E2E results may be appended after deployment.

