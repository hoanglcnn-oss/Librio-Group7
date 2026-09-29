# Librio — Sprint 3 API Contract

Protected mutations use the existing session-cookie and CSRF contract. JSON errors retain `{ status, code, message, timestamp }`; IDs and timestamps are server-owned.

## Borrowing and return

- `GET /me/borrowings` — reader; existing DTO plus derived `overdue`.
- `GET /librarian/borrowings?status=active` — librarian; active borrowing queue with reader/resource summary and timestamps.
- `POST /librarian/borrowings/{id}/return` — librarian + CSRF; atomically sets `returnedAt` and releases the exact item.

| Status | Code |
|---:|---|
| `404` | `BORROWING_NOT_FOUND` |
| `409` | `BORROWING_ALREADY_RETURNED` |
| `409` | `BORROWING_ITEM_CONFLICT` |

## Digital access

- `GET /resources/{id}/digital-access` — reader; returns `resourceId`, `canRead=true`, protected `contentUrl` and `temporaryUrl=false`.
- `GET /resources/{id}/digital-content` — reader; returns `200 application/pdf`.

The URL itself grants no authority; the content request rechecks the session and role.
Missing resource returns `404 RESOURCE_NOT_FOUND`; no digital marker returns `404 DIGITAL_CONTENT_NOT_FOUND`.

## Resource administration

Librarian-only endpoints:

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/librarian/resources/{id}` | Load editable aggregate |
| `POST` | `/librarian/resources` | Create metadata/access records |
| `PUT` | `/librarian/resources/{id}` | Update and reconcile access |

```json
{
  "title": "Domain-Driven Design",
  "authors": ["Eric Evans"],
  "description": "Reference book",
  "category": "Software Engineering",
  "accessTypes": ["PHYSICAL", "DIGITAL"],
  "physical": { "totalCopies": 3 }
}
```

Reducing below non-available copy count returns `409 RESOURCE_IN_USE`; missing resource returns `404 RESOURCE_NOT_FOUND`; invalid input uses the standard validation error. Per-item barcode, location, condition and CRUD belong to US-14.

Validation limits are: title required/max 200, at least one author (each max 255), description max 5000, category max 100, non-empty access types and physical total from 0 to 9999.
