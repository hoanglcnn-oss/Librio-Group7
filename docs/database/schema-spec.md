# Database Schema Specification & Constraints

**Project:** Librio  
**Document:** Logical Database Schema Specification  
**Scope:** Sprint 1 discovery and Sprint 2 authentication, borrow requests, checkout and My Library

---

## 1. Purpose and Sources

Tài liệu này mô tả logical database schema, relationship, database constraint và transactional invariant của Librio.

Nguồn thiết kế:

1. `docs/srs/sprint-1-srs.md`
2. `docs/srs/sprint-2-srs.md`
3. `docs/lld/api-contracts/sprint-1-api.md`
4. `docs/lld/api-contracts/sprint-2-api.md`
5. `docs/lld/sprint-2-auth-lld.md`
6. `docs/lld/sprint-2-borrow-lld.md`

Database không chứa bảng `availability`. Physical availability được tính từ trạng thái của `physical_item`; digital availability được xác định từ sự tồn tại của `digital_item`.

---

## 2. Entity Specifications

### 2.1 `resource`

Lưu bibliographic information dùng chung cho physical và digital resources.

| Column | PostgreSQL Type | Null | Constraint | Description |
|---|---|---:|---|---|
| `id` | `BIGINT` | No | Primary Key | Định danh resource |
| `title` | `VARCHAR(255)` | No |  | Tiêu đề tài liệu |
| `authors` | `VARCHAR(255)` | No |  | Danh sách tác giả dạng chuỗi |
| `description` | `TEXT` | Yes |  | Mô tả tài liệu |

Một resource có thể:
- Không có physical item.
- Có nhiều physical items.
- Không có hoặc có đúng một digital item.

### 2.2 `physical_item`

Đại diện cho một exact physical copy có thể reserve và borrow.

| Column | PostgreSQL Type | Null | Constraint | Description |
|---|---|---:|---|---|
| `id` | `BIGINT` | No | Primary Key | Định danh/mã bản sách |
| `resource_id` | `BIGINT` | No | FK → `resource.id` | Resource mà item thuộc về |
| `status` | `VARCHAR(32)` | No | Check Constraint | Trạng thái hiện tại |

Allowed values:
- `AVAILABLE`
- `RESERVED`
- `BORROWED`
- `OVERDUE`

Trong Sprint 2, borrow-request flow sử dụng chủ yếu:
`AVAILABLE` → `RESERVED` → `BORROWED`

Cancel, reject hoặc expire chuyển:
`RESERVED` → `AVAILABLE`

### 2.3 `digital_item`

Đánh dấu resource có digital representation.

| Column | PostgreSQL Type | Null | Constraint | Description |
|---|---|---:|---|---|
| `id` | `BIGINT` | No | Primary Key | Định danh digital item |
| `resource_id` | `BIGINT` | No | FK → `resource.id`, Unique | Resource được cung cấp dạng digital |

Unique constraint trên `resource_id` bảo đảm mỗi resource có tối đa một digital item trong phạm vi hiện tại.

### 2.4 `accounts`

Lưu account dùng cho authentication và reader/librarian identity.

| Column | PostgreSQL Type | Null | Constraint | Description |
|---|---|---:|---|---|
| `id` | `BIGINT` | No | Primary Key, Identity | Định danh account |
| `email` | `VARCHAR(255)` | No | Unique | Email đăng nhập đã normalize |
| `password_hash` | `VARCHAR(255)` | No |  | BCrypt password hash |
| `display_name` | `VARCHAR(255)` | Yes |  | Tên hiển thị |
| `role` | `VARCHAR(32)` | No | Check Constraint | Role của account |
| `account_status` | `VARCHAR(32)` | No | Check Constraint | Trạng thái account |
| `created_at` | `TIMESTAMP` | No |  | Thời điểm tạo |
| `updated_at` | `TIMESTAMP` | No |  | Thời điểm cập nhật |

Allowed roles:
- `READER`
- `LIBRARIAN`

Allowed account statuses:
- `ACTIVE`
- `DISABLED`

Email được trim và chuyển lowercase trong application trước khi lưu hoặc tìm kiếm. Plaintext password không được persist.

### 2.5 `borrow_request`

Lưu request lifecycle và exact physical item được reserve.

| Column | PostgreSQL Type | Null | Constraint | Description |
|---|---|---:|---|---|
| `id` | `BIGINT` | No | Primary Key, Identity | Định danh request |
| `reader_id` | `BIGINT` | No | FK → `accounts.id` | Reader gửi request |
| `resource_id` | `BIGINT` | No | FK → `resource.id` | Resource được request |
| `physical_item_id` | `BIGINT` | No | FK → `physical_item.id` | Exact reserved copy |
| `status` | `VARCHAR(32)` | No | Check Constraint | Trạng thái request |
| `requested_at` | `TIMESTAMP` | No |  | Thời điểm tạo request |
| `status_updated_at` | `TIMESTAMP` | No |  | Thời điểm status thay đổi gần nhất |
| `expires_at` | `TIMESTAMP` | No |  | Deadline hiện tại |
| `prepared_at` | `TIMESTAMP` | Yes |  | Thời điểm librarian prepare |
| `prepared_by` | `BIGINT` | Yes | FK → `accounts.id` | Librarian thực hiện prepare |
| `rejected_at` | `TIMESTAMP` | Yes |  | Thời điểm reject |
| `rejected_by` | `BIGINT` | Yes | FK → `accounts.id` | Librarian thực hiện reject |
| `fulfilled_at` | `TIMESTAMP` | Yes |  | Thời điểm fulfil |
| `fulfilled_by` | `BIGINT` | Yes | FK → `accounts.id` | Librarian thực hiện fulfil |
| `created_at` | `TIMESTAMP` | No |  | Thời điểm tạo record |
| `updated_at` | `TIMESTAMP` | No |  | Thời điểm cập nhật record |

Allowed statuses:
- `REQUESTED`
- `READY_FOR_PICKUP`
- `FULFILLED`
- `CANCELLED`
- `REJECTED`
- `EXPIRED`

Active statuses:
- `REQUESTED`
- `READY_FOR_PICKUP`

Terminal statuses:
- `FULFILLED`
- `CANCELLED`
- `REJECTED`
- `EXPIRED`

Request record không bị hard-delete khi trở thành terminal.

### 2.6 `borrowing`

Đại diện cho checkout đã hoàn thành.

| Column | PostgreSQL Type | Null | Constraint | Description |
|---|---|---:|---|---|
| `id` | `BIGINT` | No | Primary Key, Identity | Định danh borrowing |
| `physical_item_id` | `BIGINT` | No | FK → `physical_item.id` | Exact borrowed copy |
| `reader_id` | `BIGINT` | No | FK → `accounts.id` | Reader đang borrow item |
| `borrow_request_id` | `BIGINT` | No | FK → `borrow_request.id`, Unique | Source request |
| `borrowed_at` | `TIMESTAMP` | No |  | Thời điểm checkout |
| `due_at` | `TIMESTAMP` | No | Check: sau `borrowed_at` | Persisted due date |
| `returned_at` | `TIMESTAMP` | Yes | Check: không trước `borrowed_at` | NULL nghĩa là active borrowing |

Database dùng tên `due_at`; public API sử dụng field `dueDate`.
Một borrow request chỉ tạo tối đa một borrowing.

---

## 3. Relationship Cardinalities

| Parent | Child | Cardinality | Meaning |
|---|---|---|---|
| `resource` | `physical_item` | One-to-zero-or-many | Resource có thể có nhiều physical copies |
| `resource` | `digital_item` | One-to-zero-or-one | Resource có thể có digital item |
| `accounts` | `borrow_request` | One-to-zero-or-many | Reader có thể gửi nhiều requests |
| `resource` | `borrow_request` | One-to-zero-or-many | Resource có request history |
| `physical_item` | `borrow_request` | One-to-zero-or-many | Item có request history, nhưng tối đa một active request |
| `borrow_request` | `borrowing` | One-to-zero-or-one | Request có thể tạo một borrowing |
| `accounts` | `borrowing` | One-to-zero-or-many | Reader có borrowing history |
| `physical_item` | `borrowing` | One-to-zero-or-many | Item có loan history, nhưng tối đa một active borrowing |

`prepared_by`, `rejected_by` và `fulfilled_by` là optional actor relationships từ `borrow_request` đến librarian account.

---

## 4. Database Constraints

### DB-C01 — Exact active reservation
Một physical item chỉ được liên kết với tối đa một active request.
Enforced bằng partial unique index trên:
`borrow_request(physical_item_id)`
`WHERE status IN ('REQUESTED', 'READY_FOR_PICKUP')`

### DB-C02 — Duplicate active request
Một reader chỉ có tối đa một active request cho cùng resource.
Enforced bằng partial unique index trên:
`borrow_request(reader_id, resource_id)`
`WHERE status IN ('REQUESTED', 'READY_FOR_PICKUP')`

### DB-C03 — Request-to-borrowing uniqueness
`borrowing.borrow_request_id` là unique. Một request không thể tạo hai borrowings.

### DB-C04 — Active borrowing per item
Một physical item chỉ có tối đa một borrowing có:
`returned_at IS NULL`
Enforced bằng partial unique index trên `borrowing.physical_item_id`.

### DB-C05 — Valid borrowing dates
- `due_at > borrowed_at`
- `returned_at IS NULL OR returned_at >= borrowed_at`

### DB-C06 — Valid enum values
Database check constraints giới hạn:
- Physical item status.
- Borrow request status.
- Account role.
- Account status.

---

## 5. Transactional Business Invariants

Các invariant sau được Service transaction và row locking enforce vì không thể biểu diễn đầy đủ bằng row-level check constraint:

### DB-I01 — Resource/item consistency
`borrow_request.physical_item_id` phải thuộc cùng `resource_id` với request.

### DB-I02 — Request creation
Create request commit atomically:
1. Lock reader để bảo vệ commitment limit.
2. Lock một `AVAILABLE` physical item.
3. Chuyển item sang `RESERVED`.
4. Insert `REQUESTED` borrow request với exact item và expiration deadline.
5. Nếu một bước thất bại, toàn bộ operation rollback.

### DB-I03 — Release operation
Cancel, reject hoặc expire commit atomically:
1. Request chuyển sang terminal status tương ứng.
2. Exact reserved item chuyển `RESERVED` → `AVAILABLE`.
3. Request record không bị xóa.

### DB-I04 — Fulfil operation
Fulfil commit atomically:
1. Request phải là `READY_FOR_PICKUP`.
2. Reader vẫn eligible.
3. Exact item vẫn `RESERVED` cho request.
4. Request chuyển sang `FULFILLED`.
5. Item chuyển `RESERVED` → `BORROWED`.
6. Một borrowing được insert với persisted `borrowed_at` và `due_at`.

### DB-I05 — Borrowing consistency
Borrowing phải dùng cùng reader và physical item với source borrow request.

### DB-I06 — One-winner concurrency
Competing create, cancel, expire hoặc fulfil operations sử dụng database lock và constraint để chỉ một conflicting transition được commit.

---

## 6. Index Catalogue

| Index | Columns/Predicate | Purpose |
|---|---|---|
| `uq_active_request_physical_item` | `physical_item_id`, active status | Ngăn hai active requests giữ cùng item |
| `uq_active_request_reader_resource` | `reader_id`, `resource_id`, active status | Ngăn duplicate active request |
| `uq_active_borrowing_physical_item` | `physical_item_id`, `returned_at IS NULL` | Ngăn hai active borrowings cho cùng item |
| `idx_physical_item_allocation` | `resource_id`, `status`, `id` | Chọn available copy |
| `idx_borrow_request_reader_active` | Reader, status và ordering fields | My Requests |
| `idx_borrow_request_reader_outcomes` | Reader, `status_updated_at`, `id` | Recent Outcomes |
| `idx_borrow_request_expiration` | `expires_at`, `id` trên active requests | Expiration scheduler |
| `idx_borrowing_reader_active_due` | Reader, due/borrow timestamps | My Borrowings |

---

## 7. Derived Availability

Không tạo bảng hoặc persisted field availability.

**Physical availability:**
- `availableCopies = COUNT(physical_item WHERE resource_id = ? AND status = 'AVAILABLE')`
- `totalCopies = COUNT(physical_item WHERE resource_id = ?)`
*Lưu ý: Items ở trạng thái `RESERVED`, `BORROWED` hoặc `OVERDUE` không được tính vào `availableCopies`.*

**Digital availability:**
- `digitalAvailable = EXISTS(digital_item WHERE resource_id = ?)`

Client không tự tính lại availability.

---

## 8. Initialization and Persistence

Database initialization phải bảo toàn circulation state qua backend restart.

**Yêu cầu:**
- Không drop circulation tables khi application khởi động bình thường.
- Schema initialization phải idempotent hoặc được quản lý bằng migration.
- Demo seed không được overwrite request/borrowing state.
- Production có thể tắt demo seed.
- Password chỉ được lưu dưới dạng BCrypt hash.
- `schema.sql` và `data.sql` trong runtime resources phải được đồng bộ với specification này trước khi hoàn thành Sprint 2.

---

## 9. Sprint 2 Out of Scope

- Return processing.
- Fine và payment.
- Renewal.
- Completed borrowing-history UI.
- Waitlist.
- Multiple account roles.
- Inventory/warehouse management.
- Resource deletion lifecycle.
- Realtime polling, WebSocket hoặc event streaming.