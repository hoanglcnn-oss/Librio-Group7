# Librio Code Walkthrough

Tài liệu này tóm tắt các vertical slice đang có trong source code để chuẩn bị walkthrough kỹ thuật. Nội dung chỉ mô tả behavior đã thấy trong code hiện tại; chi tiết yêu cầu và thiết kế gốc nằm ở các tài liệu nguồn:

- [Documentation index](README.md)
- [High-level architecture](hld/architecture.md)
- [Sprint 2 borrow LLD](lld/sprint-2-borrow-lld.md)
- [Sprint 3 implementation design](lld/sprint-3-implementation-design.md)
- [Database schema spec](database/schema-spec.md)

## 1. Project Scope And Implemented Vertical Slices

Librio hiện là React SPA + Spring Boot modular monolith + relational database schema. Các slice đã implement trong source:

- Public discovery: browse/search resource và resource detail.
- Authentication/authorization: session cookie, CSRF, role-based reader/librarian access.
- Physical circulation: reader request, reserve exact physical item, librarian prepare, fulfil checkout, reader cancel, librarian reject, scheduler expiration.
- My Library: reader active requests, recent outcomes và active borrowings.
- Sprint 3 circulation extension: derived overdue display và librarian return.
- Sprint 3 resource administration: create/update resource metadata, physical copy reconciliation và digital marker.
- Sprint 3 protected digital access: reader-only capability endpoint và protected demo PDF response.

Deferred/out of scope trong source hiện tại: waitlist, renewal, fines/payments, multi-branch inventory, production object storage, signed URLs, DRM, distributed locking, complete borrowing history UI.

Status đọc nhanh cho pass này:

- Implemented and verified in this pass: backend test suite via mapped drive workaround, frontend install, lint, unit tests and production build.
- Implemented but not verified in this pass: none identified for the documented slices.
- Deferred/out of scope: các production concerns liệt kê ở phần 9.

## 2. Sprint 1-3 Evolution

Sprint 1 tập trung vào catalog discovery: `Resource`, `PhysicalItem`, `DigitalItem`, `/resources`, `/resources/{id}` và availability derive từ item state.

Sprint 2 thêm identity và circulation: `Account`, login/logout/CSRF, borrow request lifecycle, reservation, fulfil thành `Borrowing`, My Library và constraints bảo vệ active request/borrowing.

Sprint 3 mở rộng đúng trên lifecycle đó: return physical item, derive overdue từ `dueAt`, protected digital read capability và librarian resource administration. Backend hiện đã có service/controller/test cho các slice Sprint 3, không chỉ frontend mock.

## 3. Demo Flows

Browse -> Detail -> Request:
Reader/guest mở `/resources`, xem detail `/resources/:id`. Reader đăng nhập có thể gửi `POST /borrow-requests`. Backend reserve một `AVAILABLE` physical item và tạo `BorrowRequest REQUESTED` trong một transaction.

Librarian Prepare -> Fulfil -> Return/Overdue:
Librarian mở `/librarian/borrow-requests`, prepare request đang `REQUESTED`, fulfil request `READY_FOR_PICKUP` thành `Borrowing`, rồi return ở `/librarian/borrowings/{id}/return`. Overdue hiển thị khi borrowing active có `dueAt` trước server time.

Resource Administration:
Librarian dùng `/librarian/resources/new` hoặc `/librarian/resources/:id/edit`. Backend tạo/cập nhật metadata, reconcile physical copy rows và thêm/xóa digital marker trong transaction.

Protected Digital Access:
Reader mở digital action từ detail page. Frontend gọi `/resources/{id}/digital-access`, nhận `contentUrl`, rồi mở `/resources/{id}/digital-content`. Cả hai route yêu cầu `ROLE_READER`.

## 4. Code Entry Points

Browse -> Detail -> Request:

- Frontend: `frontend/src/pages/ResourceListPage.jsx`, `frontend/src/pages/ResourceDetailPage.jsx`, `frontend/src/components/DemoActions.jsx`, `frontend/src/services/resourceApi.js`, `frontend/src/services/authApi.js`.
- Controller: `ResourceController`, `BorrowRequestController`.
- Service: `ResourceService`, `BorrowService#createRequest`.
- Repository/domain/database: `ResourceRepository`, `PhysicalItemRepository#findForUpdate`, `BorrowRequestRepository`, `Resource`, `PhysicalItem`, `BorrowRequest`, `schema.sql`.

Librarian Prepare -> Fulfil -> Return/Overdue:

- Frontend: `frontend/src/pages/LibrarianRequestsPage.jsx`, `frontend/src/pages/MyLibraryPage.jsx`, `frontend/src/utils/borrowingStatus.js`, `frontend/src/services/authApi.js`.
- Controller: `LibrarianBorrowController`, `LibrarianBorrowingController`, `ReaderBorrowingController`.
- Service: `BorrowService#prepare`, `BorrowService#fulfil`, `BorrowService#returnBorrowing`, `BorrowService#getReaderBorrowings`, `BorrowService#getActiveBorrowingsForLibrarian`.
- Repository/domain/database: `BorrowRequestRepository`, `BorrowingRepository`, `PhysicalItemRepository`, `Borrowing`, `borrow_request`, `borrowing`, partial unique indexes in `schema.sql`.

Resource Administration:

- Frontend: `frontend/src/pages/ResourceAdminPage.jsx`, `frontend/src/utils/resourceForm.js`, `frontend/src/services/authApi.js`.
- Controller: `LibrarianResourceController`.
- Service: `ResourceAdminService`.
- Repository/domain/database: `ResourceRepository`, `PhysicalItemRepository`, `DigitalItemRepository`, `resource`, `physical_item`, `digital_item`.

Protected Digital Access:

- Frontend: `frontend/src/components/DemoActions.jsx`, `frontend/src/services/authApi.js`, `frontend/src/config/runtime.js`.
- Controller: `DigitalAccessController`.
- Service: `DigitalAccessService`.
- Security/database: `SecurityConfig`, `DigitalItemRepository`, `digital_item`.

## 5. Business Invariants

- One physical item can belong to at most one active request or active borrowing.
- A reader can have at most one active request for the same resource.
- A reader cannot create a request for a resource they are already actively borrowing.
- Active commitment is active borrowings plus `REQUESTED`/`READY_FOR_PICKUP` requests.
- Request creation must reserve an exact `AVAILABLE` physical item immediately.
- Cancel, reject and expire release the exact reserved item.
- Fulfil creates at most one borrowing for a request and moves the exact item from `RESERVED` to `BORROWED`.
- Return sets `returnedAt` and moves the exact item from `BORROWED` to `AVAILABLE`.
- Overdue is derived from `returnedAt == null && dueAt < serverNow`, not persisted as a borrowing status.
- Resource administration can delete only `AVAILABLE` physical items.

## 6. Security Model

The backend uses server-side session authentication with CSRF for state-changing requests. Public discovery remains open through `/resources/**`, while reader actions, My Library, digital content and librarian operations are role-protected.

`SecurityConfig` intentionally matches `/resources/*/digital-access` and `/resources/*/digital-content` before public `/resources/**`; changing that order would make protected digital routes public.

Frontend API calls include credentials through `authApi.js` for authenticated routes. Mock adapters are guarded by `import.meta.env.DEV` and production `.env.production` sets all mock flags to false.

## 7. Transaction And Concurrency Decisions

Borrow lifecycle logic lives in `BorrowService` and uses service-level transactions. The current lock order is:

- Create request: lock reader account, then lock candidate available physical item.
- Prepare/cancel/reject/expire: lock borrow request, then use the request's exact physical item.
- Fulfil: lock borrow request, lock reader, then use the exact physical item.
- Return: lock borrowing, then lock exact physical item.

`RequestExpiredTransitionException` is configured with `noRollbackFor` on request-mutating methods that can detect expiration. This lets the system persist `EXPIRED` and release the reserved item while returning `409 REQUEST_EXPIRED` to the API caller.

Database partial unique indexes are the final protection against active request and active borrowing races. The application still performs explicit checks to return stable business error codes.

## 8. Three Hard Problems

Competing circulation transitions:
State-changing methods lock the request or reader before mutating state, and terminal transitions are guarded by allowed transition checks. If two actions compete, only one committed state should survive; the loser receives a stable conflict response.

Expired transition must commit while returning a conflict:
When an active request is already expired, the service transitions it to `EXPIRED`, releases the reserved item and throws `RequestExpiredTransitionException`. `noRollbackFor` preserves that cleanup even though the API returns `409 REQUEST_EXPIRED`.

Keeping availability consistent with physical item state:
Availability is derived from `physical_item.status`. Create request changes `AVAILABLE -> RESERVED`, fulfil changes `RESERVED -> BORROWED` without decrementing again, and cancel/reject/expire/return move the exact item back to `AVAILABLE` when appropriate.

## 9. Sprint 3 Simplifications And Deferred Production Concerns

Implemented simplifications:

- Digital content is a server-generated demo PDF, not persistent file storage.
- Digital access returns a direct protected backend URL, not a signed object-storage URL.
- Authors are accepted as JSON array at the API boundary but persisted as comma-separated text.
- Resource administration supports create/update and copy reconciliation, not a full inventory audit trail.
- Librarian borrowing list supports `status=active` only.

Deferred production concerns:

- Object storage, signed URL expiry and DRM for digital assets.
- Database migrations beyond idempotent `schema.sql` initialization.
- Multi-instance/distributed lock strategy and production load testing.
- Fine/payment/renewal policies.
- End-user borrowing history beyond current request outcomes and active borrowings.

## 10. Tests/Evidence And Commands

Existing backend tests include Sprint 2 circulation service/controller coverage and Sprint 3 service/controller coverage:

- `BorrowServiceTest`
- `Sprint3ServiceTest`
- `BorrowRequestControllerTest`
- `Sprint3ControllerTest`
- `AuthSecurityTest`
- `ResourceControllerTest`
- `ResourceRepositoryTest`

Existing frontend tests cover runtime config, mock digital access data, borrowing presentation and resource form mapping/validation.

Verification commands for this pass:

```powershell
cd backend
mvn test
cd ..\frontend
npm ci
npm run lint
npm test
npm run build:production
cd ..
git diff --check
```

Latest local verification in this pass:

- `cd backend; mvn test`: initial direct command failed because `mvn` is not on PATH.
- `cd backend; & 'C:\Users\Admin\apache-maven-3.9.8\bin\mvn.cmd' test`: failed on the Unicode workspace path with Maven compile status error `Input length = 1`.
- `subst X: 'D:\FMềm\Librio-Group7 (2)\Librio-Group7'; cd X:\backend; & 'C:\Users\Admin\apache-maven-3.9.8\bin\mvn.cmd' test`: passed, 34 tests.
- `cd frontend; npm ci`: passed, 139 packages installed/audited and 0 vulnerabilities reported.
- `cd frontend; npm run lint`: passed.
- `cd frontend; npm test`: passed, 8 tests.
- `cd frontend; npm run build:production`: passed, Vite production build and bundle check completed.
- `git diff --check`: passed, with only Git CRLF conversion warnings on Windows.

## 11. Known Limitations/Open Risks

- `docs/database/schema-spec.md` is still scoped as Sprint 1-2 and does not fully describe Sprint 3 return/digital/admin additions.
- `docs/hld/architecture.md` is marked Sprint 2 and does not fully reflect Sprint 3 resource administration or digital access.
- `docs/lld/frontend-lld.md` describes the earlier Sprint 1 frontend structure and does not list current auth, My Library, librarian or admin pages.
- Frontend request creation records the returned request but does not refetch resource detail availability in `DemoActions.jsx`.
- In `LibrarianRequestsPage.jsx`, fulfil triggers request queue refetch; active borrowing list is refreshed by its own loader or return flow.
- `physical_item.status` includes `OVERDUE`, but overdue is currently derived on `Borrowing`; the service does not persist item status as `OVERDUE`.
