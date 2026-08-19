## Resource/Item Data Model (Sprint 1 SRS)
- source: docs/srs/sprint-1-srs.md
- type: schema
- content: DATA_C4K8M2QZ_START
  Thiết lập quan hệ `1 Resource ── 0..N PhysicalItem` và `1 Resource ── 0..1 DigitalItem`. `PhysicalItem` chứa enum trạng thái (`AVAILABLE`, `BORROWED`, `OVERDUE`).
  DATA_C4K8M2QZ_END

## API Foundation (Sprint 1 SRS)
- source: docs/srs/sprint-1-srs.md
- type: api-contract
- content: DATA_P7V1H6LD_START
  Cung cấp 2 REST API endpoints chuẩn hóa: `GET /resources?q={keyword}`; `GET /resources/{id}`.
  DATA_P7V1H6LD_END

## RESOURCE
- source: docs/database/schema-spec.md
- type: schema
- content: DATA_R9N3T5WA_START
  `id` `VARCHAR(64)` / `BIGINT` PRIMARY KEY; `title` `VARCHAR(255)` NOT NULL; `authors` `VARCHAR(255)` NOT NULL; `description` `TEXT` NULLABLE.
  DATA_R9N3T5WA_END

## PHYSICAL_ITEM
- source: docs/database/schema-spec.md
- type: schema
- content: DATA_F2Q6B8XE_START
  `id` `VARCHAR(64)` / `BIGINT` PRIMARY KEY; `resource_id` `VARCHAR(64)` / `BIGINT` FOREIGN KEY (`RESOURCE.id`); `status` `VARCHAR(32)` NOT NULL, enum: `AVAILABLE`, `BORROWED`, `OVERDUE`.
  DATA_F2Q6B8XE_END

## DIGITAL_ITEM
- source: docs/database/schema-spec.md
- type: schema
- content: DATA_J5M1Y7KC_START
  `id` `VARCHAR(64)` / `BIGINT` PRIMARY KEY; `resource_id` `VARCHAR(64)` / `BIGINT` FOREIGN KEY (`RESOURCE.id`).
  DATA_J5M1Y7KC_END

## BORROWING
- source: docs/database/schema-spec.md
- type: schema
- content: DATA_D8L4S9UP_START
  `id` `VARCHAR(64)` / `BIGINT` PRIMARY KEY; `physical_item_id` FOREIGN KEY (`PHYSICAL_ITEM.id`); `user_id` FOREIGN KEY (`USER.id`); `borrowed_at` TIMESTAMP NOT NULL; `due_at` TIMESTAMP NOT NULL; `returned_at` TIMESTAMP NULLABLE.
  DATA_D8L4S9UP_END

## USER
- source: docs/database/schema-spec.md
- type: schema
- content: DATA_W3G7A1RN_START
  `id` `VARCHAR(64)` / `BIGINT` PRIMARY KEY; `username` `VARCHAR(100)` NOT NULL.
  DATA_W3G7A1RN_END

## BC-01 (One Active Borrowing)
- source: docs/database/schema-spec.md
- type: schema
- content: DATA_H6Z2E5MV_START
  Mỗi `PHYSICAL_ITEM` chỉ có tối đa 1 bản ghi `BORROWING` có `returned_at IS NULL`.
  DATA_H6Z2E5MV_END

## BC-02 (Valid Lifecycle)
- source: docs/database/schema-spec.md
- type: schema
- content: DATA_K1P8C4SX_START
  Trạng thái `PHYSICAL_ITEM` đi từ `AVAILABLE ➔ BORROWED ➔ OVERDUE ➔ AVAILABLE`.
  DATA_K1P8C4SX_END

## BC-03 (Derived Availability)
- source: docs/database/schema-spec.md
- type: schema
- content: DATA_V7D3N9QL_START
  Không tạo bảng `Availability`. Availability được đếm động bằng SQL Query.
  DATA_V7D3N9QL_END

## BC-04 (Zero-Item Resource)
- source: docs/database/schema-spec.md
- type: schema
- content: DATA_B4X6J2TF_START
  Cho phép `RESOURCE` có `0` physical item hoặc `0` digital item mà vẫn tìm kiếm được.
  DATA_B4X6J2TF_END

## Search / Browse API
- source: docs/lld/api-contracts/sprint-1-api.md
- type: api-contract
- content: DATA_Q9R5L3HD_START
  `GET /resources?q={keyword}`. Parameter `q` is `string`, not required; nếu trống, trả danh sách mặc định. `200 OK` returns `{ "items": [...] }` or `{ "items": [] }`.
  DATA_Q9R5L3HD_END

## Resource Detail & Availability API
- source: docs/lld/api-contracts/sprint-1-api.md
- type: api-contract
- content: DATA_M2W8K6PA_START
  `GET /resources/{id}` requires `id` as a path parameter. `200 OK` returns resource metadata, `accessTypes`, and applicable `physical` and `digital` blocks; a physical-only response omits the `digital` block. `404 Not Found` returns `{ "message": "Resource not found" }`.
  DATA_M2W8K6PA_END

## API Response UI State Rules
- source: docs/lld/frontend-lld.md
- type: api-contract
- content: DATA_T6C1V9ZG_START
  Loading: Fetching API ➔ Hiển thị Skeleton/Spinner, không render Empty/Not Found. Empty Search: API trả `200` với `items: []` ➔ Render Empty State. `404` Detail ➔ Render màn hình Not Found. API/network lỗi ➔ Render Error State kèm nút Retry.
  DATA_T6C1V9ZG_END

## Availability Aggregation Model
- source: docs/lld/sprint-1-lld.md
- type: schema
- content: DATA_N4F7R1BX_START
  `totalCopies = COUNT(PhysicalItem WHERE resource_id = :id)`; `availableCopies = COUNT(PhysicalItem WHERE resource_id = :id AND status = 'AVAILABLE')`. Nếu tồn tại `DigitalItem` tương ứng ➔ `digital.available = true`; nếu không có `DigitalItem` ➔ bỏ hẳn block `digital` khỏi JSON response.
  DATA_N4F7R1BX_END

## Search / Browse API (LLD)
- source: docs/lld/sprint-1-lld.md
- type: api-contract
- content: DATA_E8Y2H5KM_START
  `GET /resources?q={keyword}` returns a success response with `{ "items": [...] }` and an empty `200 OK` response with `{ "items": [] }`.
  DATA_E8Y2H5KM_END

## Resource Detail API (LLD)
- source: docs/lld/sprint-1-lld.md
- type: api-contract
- content: DATA_S3Q6L8VD_START
  `GET /resources/{id}` returns `accessTypes`, `physical.totalCopies`, `physical.availableCopies`, and `digital.available` when applicable; the physical-only response omits `digital`. `404 Not Found` returns `{ "message": "Resource not found" }`.
  DATA_S3Q6L8VD_END

## Frontend UI State Machine
- source: docs/lld/sprint-1-lld.md
- type: api-contract
- content: DATA_A5J9W2PC_START
  Loading shows Skeleton/Spinner and does not show Empty/Not Found. `200` with `items: []` shows Empty State. Detail `404` shows Not Found. Network/API error shows an error message with Retry. `PHYSICAL` renders `availableCopies / totalCopies`; `DIGITAL` renders Available.
  DATA_A5J9W2PC_END

## Communication Protocol
- source: docs/hld/architecture.md
- type: protocol
- content: DATA_L7T4M1QH_START
  Protocol: HTTP/RESTful APIs (JSON Payload).
  DATA_L7T4M1QH_END

## Derived Availability Capability
- source: docs/hld/architecture.md
- type: schema
- content: DATA_X2K6D9RV_START
  Trạng thái khả dụng được tính toán động (derived) từ `PhysicalItem.status` và sự tồn tại của `DigitalItem`, không lưu cứng thành Table trong DB.
  DATA_X2K6D9RV_END

## Detail Availability Response
- source: docs/hld/architecture.md
- type: api-contract
- content: DATA_G8P3Y5NL_START
  Availability được gộp trực tiếp vào Response payload của `GET /resources/{id}`, không tách thành micro-service hay API riêng lẻ.
  DATA_G8P3Y5NL_END

## Backend Layering Boundary
- source: docs/hld/architecture.md
- type: nfr
- content: DATA_U1C7F4ZK_START
  Cấu trúc thực tế trong từng Module Backend phải tuân thủ nghiêm ngặt mô hình 3 lớp: `Controller (HTTP Boundary) ➔ Service (Business Boundary) ➔ Repository (Persistence Boundary) ➔ DB`.
  DATA_U1C7F4ZK_END

## Borrow Transaction Boundary
- source: docs/hld/architecture.md
- type: nfr
- content: DATA_R6V2A8MX_START
  Thao tác mượn sách (`Borrow`) bắt buộc phải bọc trong một Server-Side Atomic Transaction: tạo bản ghi `Borrowing` mới và cập nhật `PhysicalItem.status = 'BORROWED'`. Nếu một hành động thất bại, toàn bộ giao dịch phải `ROLLBACK`.
  DATA_R6V2A8MX_END

## Database Engine
- source: docs/hld/architecture.md
- type: schema
- content: DATA_C9H5Q1WB_START
  Database: Relational Database (PostgreSQL).
  DATA_C9H5Q1WB_END
