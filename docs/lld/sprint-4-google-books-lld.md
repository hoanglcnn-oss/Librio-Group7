# Librio — Sprint 4 Google Books ISBN Lookup LLD

## 1. Scope

This document defines the Sprint 4 design for librarian ISBN metadata lookup using Google Books under US-19. The integration covers ISBN validation and normalization, Google Books provider boundary, exact-ISBN candidate verification, metadata mapping, provider failure handling, resource metadata persistence, and manual-entry fallback. Google Books metadata is treated as editable prefill and does not automatically create a Resource.

## 2. Lookup Flow

```text
Librarian enters ISBN
  ↓
Backend validates ISBN & normalizes to ISBN-13
  ↓
Google Books API lookup
  ↓
Verify exact ISBN candidate match
  ├── Found → Return BookMetadata for editable prefill → Librarian reviews → Save Resource
  └── None  → Return 404 BOOK_METADATA_NOT_FOUND
```

The browser does not call Google Books directly; all communication stays behind the backend.

## 3. ISBN Validation and Normalization

The backend accepts valid ISBN-10 or ISBN-13:
1. Strips formatting characters (spaces, hyphens).
2. Validates syntax and checksum.
3. Converts ISBN-10 to canonical ISBN-13 for lookup and persistence.
4. Rejects invalid ISBN with `400 INVALID_ISBN`.

**Decision:** Persist canonical ISBN-13 to protect publication uniqueness in the database regardless of input formatting.

## 4. Provider Boundary

Google Books is isolated behind `GoogleBooksClient` → `GoogleBooksAdapter` → `BookMetadata` model (`title`, `authors`, `description`, `isbn`, `coverImageUrl`, `externalSourceId`). Provider structures are never exposed directly to domain objects or UI.

**Decision:** Use a backend adapter to prevent third-party provider structure changes from leaking into the core application.

## 5. Exact ISBN Candidate Selection

Google Books lookup searches candidates and verifies `industryIdentifiers` against the requested normalized ISBN-13.
- Only exact candidate matches are accepted.
- If no exact candidate is found, returns `404 BOOK_METADATA_NOT_FOUND`.

**Decision:** Verify exact identifier match rather than trusting the first provider result.

## 6. Metadata Mapping & Resource Persistence

Available provider metadata maps to `Resource` fields (`volume id` → `externalSourceId`, `title`, `authors`, `description`, `isbn`, `coverImageUrl`). Optional fields may be blank.

`Resource` schema extensions:
- `isbn` (nullable, UNIQUE when present).
- `coverImageUrl`, `externalSourceId`.
- `metadataSource`: `MANUAL` (default/existing rows) or `GOOGLE_BOOKS`.

**Decision:** Lookup produces editable prefill. Resource creation requires an explicit save action by the librarian.

## 7. Provider Failure & Error Handling

Lookup failures must not block manual resource administration:

| Condition | HTTP | Result / Code |
| --- | ---: | --- |
| Invalid ISBN | 400 | `INVALID_ISBN` |
| No exact candidate match | 404 | `BOOK_METADATA_NOT_FOUND` |
| Provider timeout | 504 | `BOOK_METADATA_LOOKUP_TIMEOUT` |
| Duplicate persisted ISBN | 409 | `RESOURCE_ISBN_EXISTS` |

Provider requests use bounded timeouts without blocking manual resource entry.

## 8. Verification & Completion Criteria

- **T-192 Spike cases**: Valid ISBN with exact match, valid ISBN with no match, malformed ISBN, provider timeout/failure. Runtime smoke test verifies real Google Books lookup.
- **T-193 Completion criteria**: Defined ISBN canonicalization, backend adapter boundary, exact candidate matching, metadata mapping, provenance tracking, error handling, and manual fallback.

