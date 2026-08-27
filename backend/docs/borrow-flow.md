# Borrow flow API

## State flow

```text
AVAILABLE
  -> Reader creates request
RESERVED + REQUESTED
  -> Librarian prepares item
RESERVED + READY_FOR_PICKUP
  -> Librarian fulfils request
BORROWED + FULFILLED + Borrowing(dueAt)
```

Current business defaults:

- Pickup window: 3 days from `prepare`.
- Loan period: 14 days from `fulfil`.
- The lowest-ID available physical item is selected.
- One reader cannot have two active requests for the same resource.

## Authentication

The API uses a session cookie and CSRF protection.

1. `GET /auth/csrf` and retain its cookies.
2. Send the returned token using the returned `headerName`.
3. `POST /auth/login` using the same cookie session.
4. Repeat CSRF retrieval before state-changing requests.

Frontend requests must include credentials, for example `credentials: "include"` with `fetch`.

## Endpoints

### Reader creates a request

```http
POST /me/borrow-requests
Content-Type: application/json

{"resourceId": 1}
```

Returns HTTP `201` and a borrow request with status `REQUESTED`.

### Librarian prepares a request

```http
POST /librarian/borrow-requests/{requestId}/prepare
```

Returns HTTP `200` and status `READY_FOR_PICKUP` with `readyAt` and `expiresAt`.

### Librarian fulfils a request

```http
POST /librarian/borrow-requests/{requestId}/fulfil
```

Returns HTTP `200` with the newly created borrowing and its `dueAt`.

## Error behavior

- `400`: missing `resourceId`.
- `401`: not authenticated.
- `403`: wrong role or missing/invalid CSRF token.
- `404`: reader, librarian, resource, or request not found.
- `409`: no available physical item, duplicate active request, invalid state transition, expired request, or duplicate borrowing.

## Repeatable local demo

Start the backend with the `test` profile and seed passwords:

```powershell
$env:LIBRIO_SEED_READER_PASSWORD = 'Reader123!'
$env:LIBRIO_SEED_LIBRARIAN_PASSWORD = 'Librarian123!'
mvn spring-boot:run "-Dspring-boot.run.profiles=test" "-Dspring-boot.run.arguments=--server.port=18080"
```

In another PowerShell terminal:

```powershell
.\scripts\demo-borrow-flow.ps1 `
  -ReaderPassword 'Reader123!' `
  -LibrarianPassword 'Librarian123!'
```

The H2 test database is recreated whenever the application restarts.
