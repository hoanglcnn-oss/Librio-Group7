# Librio — Thiết kế Database

## 1. Mục đích

Tài liệu này mô tả persistence model mục tiêu của Librio V2.

Nó chuyển các concept trong `domain.md` thành:

- table;
- column;
- relationship;
- constraint;
- index;
- migration strategy;
- seed/data lifecycle.

Nguyên tắc:

> Database phải bảo vệ các invariant quan trọng mà không biến schema thành một generic platform cho những use case V3+ chưa tồn tại.

---

# 2. Database Platform

V2 tiếp tục sử dụng:

```text
PostgreSQL
```

PostgreSQL là database chính cho:

- local integration;
- deployed environment;
- database-critical tests.

H2 có thể tiếp tục tồn tại cho fast tests nhưng không được coi là behavioral replacement hoàn toàn cho PostgreSQL.

---

# 3. ERD mục tiêu V2

```text
ACCOUNT
  |
  | 1
  |             RESOURCE
  |            /       \
  |           /         \
  |          v           v
  |    PHYSICAL_ITEM   DIGITAL_ITEM
  |          |
  |          |
  +----< BORROW_REQUEST
  |          |
  |          v
  +------< BORROWING


ACCOUNT
  |
  +----< ACCESS_SUBSCRIPTION >---- ACCESS_PLAN
  |              |
  |              v
  +----< PAYMENT_TRANSACTION
```

Các cluster chính:

```text
Catalog
→ RESOURCE

Physical Inventory
→ PHYSICAL_ITEM

Circulation
→ BORROW_REQUEST
→ BORROWING

Identity
→ ACCOUNT

Digital Content
→ DIGITAL_ITEM

Future Paid Access
→ ACCESS_PLAN
→ ACCESS_SUBSCRIPTION
→ PAYMENT_TRANSACTION
```

---

# 4. `accounts`

## 4.1 Mục đích

`accounts` lưu identity, authentication state và authorization role.

Target V2:

```text
accounts
--------
id
email
password_hash
display_name
role
account_status
reader_type
created_at
updated_at
```

---

## 4.2 Role

```text
role
----
READER
LIBRARIAN
```

DB phải có CHECK constraint.

Role chỉ đại diện authorization role.

---

## 4.3 Reader Type

V2 dùng representation tối giản:

```text
reader_type
-----------
INSTITUTIONAL
EXTERNAL
```

Ý nghĩa:

- `INSTITUTIONAL`: reader thuộc institution/library hiện tại;
- `EXTERNAL`: account reader không có institutional affiliation.

Trong V2:

```text
INSTITUTIONAL
→ có thể đủ điều kiện dùng physical circulation

EXTERNAL
→ không có physical borrowing mặc định
```

`reader_type` chỉ áp dụng cho `READER`.

Có thể enforce:

```text
role = READER
→ reader_type IS NOT NULL

role = LIBRARIAN
→ reader_type IS NULL
```

nếu implementation giữ single-role model hiện tại.

---

## 4.4 Loại bỏ `membership_eligible`

V1 có:

```text
membership_eligible BOOLEAN
```

V2 loại bỏ field này.

Lý do:

- nó trộn Account với paid membership eligibility;
- physical borrowing không còn phụ thuộc paid membership;
- future paid product có thể tự quyết định eligibility theo policy riêng.

---

# 5. `resource`

Target:

```text
resource
--------
id
title
authors
description
category
isbn
cover_image_url
metadata_source
external_source_id
resource_status
```

---

## 5.1 Authors

V2 tiếp tục giữ:

```text
authors VARCHAR
```

Không tạo `author` table nếu chưa có behavior yêu cầu author lifecycle riêng.

---

## 5.2 ISBN

ISBN tiếp tục:

```text
UNIQUE
```

khi khác `NULL`.

Policy hiện tại coi canonical ISBN là unique identifier trong catalog.

Nếu sau này Librio modeling edition sâu hơn, rule này phải được review.

---

## 5.3 Metadata source

```text
MANUAL
GOOGLE_BOOKS
```

được bảo vệ bằng CHECK constraint.

---

## 5.4 Resource lifecycle

V2 thêm:

```text
resource_status
---------------
ACTIVE
ARCHIVED
```

Mục tiêu là tránh hard-delete Resource đã có circulation history.

`ARCHIVED` có nghĩa:

- không còn được expose như active catalog item;
- historical BorrowRequest/Borrowing vẫn giữ reference;
- dữ liệu không bị mất.

Hard delete chỉ phù hợp với resource chưa tạo historical dependency và chỉ khi có use case rõ.

---

# 6. `physical_item`

Target:

```text
physical_item
-------------
id
resource_id
barcode
location
inventory_status
circulation_status
```

Relationship:

```text
Resource 1
    |
    +---- * PhysicalItem
```

---

## 6.1 Barcode

```text
barcode NOT NULL UNIQUE
```

Barcode là stable identifier của physical asset.

---

## 6.2 Inventory Status

```text
ACTIVE
LOST
DAMAGED
WITHDRAWN
```

CHECK constraint phải được giữ.

---

## 6.3 Circulation Status

```text
AVAILABLE
RESERVED
BORROWED
```

V2 **tiếp tục persist** `circulation_status`.

Lý do:

- allocation query đơn giản;
- locking candidate dễ;
- operational query nhanh;
- trạng thái item rõ cho librarian.

---

## 6.4 Circulation status là persisted operational state

`circulation_status` có thể được kiểm chứng bằng circulation records:

```text
RESERVED
↔ active BorrowRequest

BORROWED
↔ active Borrowing
```

Nó tạo duplicate representation có chủ đích.

Do đó invariant bắt buộc:

```text
RESERVED
→ phải có active BorrowRequest

BORROWED
→ phải có active Borrowing

AVAILABLE
→ không được có active request/borrowing sở hữu item
```

Consistency được bảo vệ bằng:

- transaction;
- locking;
- database constraints khi biểu diễn được;
- automated tests.

---

# 7. `borrow_request`

Target:

```text
borrow_request
--------------
id
reader_id
resource_id
physical_item_id
status

requested_at
status_updated_at
expires_at

prepared_at
prepared_by

rejected_at
rejected_by
rejection_reason

fulfilled_at
fulfilled_by
```

---

## 7.1 Resource và PhysicalItem cùng tồn tại

Cả hai field được giữ:

```text
resource_id
physical_item_id
```

Dù `PhysicalItem` đã reference Resource.

Lý do:

```text
resource_id
→ resource mà reader request

physical_item_id
→ exact copy system allocate
```

Đây là hai ý nghĩa business khác nhau.

---

## 7.2 `physical_item_id`

Trong current exact-copy reservation model, active request phải có exact item.

Có thể nullable cho historical/migration compatibility nhưng application invariant của flow mới phải đảm bảo active request đã allocate item.

---

## 7.3 Status

```text
REQUESTED
READY_FOR_PICKUP
FULFILLED
CANCELLED
REJECTED
EXPIRED
```

CHECK constraint được giữ cho đến khi V2 quyết định simplify `READY_FOR_PICKUP`.

---

## 7.4 Audit fields

V2 giữ các actor/timestamp có business meaning:

```text
requested_at
status_updated_at

prepared_at / prepared_by
rejected_at / rejected_by
rejection_reason
fulfilled_at / fulfilled_by
```

V1 có thêm:

```text
created_at
updated_at
```

trên BorrowRequest.

V2 coi chúng là redundant với:

```text
requested_at
status_updated_at
```

và ưu tiên loại bỏ nếu implementation không chứng minh cần một generic modification timestamp riêng.

---

# 8. `borrowing`

Target:

```text
borrowing
---------
id
physical_item_id
reader_id
borrow_request_id
borrowed_at
due_at
returned_at
```

---

## 8.1 Source request

```text
borrow_request_id
UNIQUE NOT NULL
```

Một BorrowRequest chỉ được fulfil thành tối đa một Borrowing.

---

## 8.2 Reader

`reader_id` được giữ dù có thể derive từ BorrowRequest.

Lý do:

- Borrowing là historical business record độc lập;
- query theo reader đơn giản;
- borrowing history không cần traverse request cho mọi use case.

Application phải bảo đảm:

```text
borrowing.reader_id
=
borrow_request.reader_id
```

---

## 8.3 Exact item

Application phải bảo đảm:

```text
borrowing.physical_item_id
=
borrow_request.physical_item_id
```

Việc này không cần trigger phức tạp trong V2.

Service transaction chịu trách nhiệm enforce.

---

## 8.4 Time constraints

Giữ:

```text
due_at > borrowed_at
```

và:

```text
returned_at IS NULL
OR
returned_at >= borrowed_at
```

---

## 8.5 Overdue

Không thêm column:

```text
is_overdue
```

Overdue được derive:

```text
returned_at IS NULL
AND due_at < now
```

---

# 9. Circulation Constraints

Các constraint hiện tại có giá trị cao và phải được giữ.

## 9.1 Một active request trên mỗi physical item

```sql
UNIQUE (physical_item_id)
WHERE status IN ('REQUESTED', 'READY_FOR_PICKUP')
```

---

## 9.2 Một active request cho cùng reader + resource

```sql
UNIQUE (reader_id, resource_id)
WHERE status IN ('REQUESTED', 'READY_FOR_PICKUP')
```

---

## 9.3 Một active borrowing trên mỗi physical item

```sql
UNIQUE (physical_item_id)
WHERE returned_at IS NULL
```

---

## 9.4 Active borrowing cùng resource

Database partial index trên exact item chưa đủ để chặn:

```text
reader borrowing copy #101 of Resource A
+
reader requests copy #102 of Resource A
```

Rule này tiếp tục được enforce ở service/query layer.

Nếu sau này cần database-level enforcement mạnh hơn, design có thể được review.

---

# 10. Circulation Indexes

Giữ index phục vụ:

### Allocation

```text
resource_id
inventory_status
circulation_status
id
```

### Reader active requests

```text
reader_id
status
requested_at
```

### Request expiration scheduler

```text
expires_at
```

partial trên active request.

### Active borrowing / overdue query

```text
reader_id
due_at
```

với:

```text
returned_at IS NULL
```

Index phải dựa trên query thực tế, không thêm index chỉ vì field tồn tại.

---

# 11. `digital_item`

Target:

```text
digital_item
------------
id
resource_id
preview_content_key
full_content_key
full_access_policy
```

Relationship:

```text
Resource 1
    |
    +---- 0..1 DigitalItem
```

---

## 11.1 Content key

```text
preview_content_key
full_content_key
```

là references tới content delivery/storage layer.

Chúng không được giả định luôn là local generated PDF.

Storage implementation có thể thay đổi.

---

## 11.2 Full Access Policy

V2 dùng policy nhỏ:

```text
full_access_policy
------------------
PUBLIC
INSTITUTIONAL
RESTRICTED
```

### PUBLIC

Full content có thể được sử dụng không cần institutional entitlement.

### INSTITUTIONAL

Full content được cấp cho Institutional Reader của deployment hiện tại.

### RESTRICTED

Digital representation có thể tồn tại nhưng baseline V2 không cấp full access thông qua public/institutional rule.

Preview vẫn có thể tồn tại.

`RESTRICTED` cũng để lại đường cho future paid access mà không buộc V2 tạo entitlement engine.

---

## 11.3 Không lưu `user_can_access`

Không có column:

```text
user_can_access
```

vì effective access phụ thuộc user + resource + policy và phải được derive.

---

# 12. Paid Access Tables

V1 có:

```text
membership_plan
membership_subscription
payment_transaction
```

V2 giữ capability nhưng sửa semantics để không trộn physical circulation.

Target naming:

```text
access_plan
access_subscription
payment_transaction
```

Việc rename thực tế phải được thực hiện bằng migration.

---

# 13. `access_plan`

Target:

```text
access_plan
-----------
id
code
name
duration_months
price_amount
currency
active
```

V1 field:

```text
monthly_borrow_quota
```

bị loại bỏ.

Lý do:

> Physical borrowing policy không phải responsibility của paid digital access plan.

V2 chưa cần table mô tả resource coverage của plan vì paid external digital product chưa thuộc scope.

---

# 14. `access_subscription`

Target:

```text
access_subscription
-------------------
id
account_id
plan_id
payment_transaction_id
starts_at
expires_at
```

Constraint:

```text
expires_at > starts_at
```

Một subscription hiện vẫn được tạo từ một payment transaction.

Nếu tương lai có:

- free trial;
- manual grant;
- institution-sponsored plan;

relationship này có thể được review ở V3+.

---

# 15. `payment_transaction`

Target:

```text
payment_transaction
-------------------
id
account_id
plan_id
amount
currency
status
created_at
completed_at
provider_txn_ref
provider_transaction_no
provider_response_code
```

Status:

```text
PENDING
SUCCESS
FAILED
```

---

## 15.1 Snapshot amount

`amount` và `currency` được giữ trực tiếp trên transaction.

Nếu AccessPlan thay đổi giá sau đó, historical payment vẫn giữ giá thực tế lúc transaction được tạo.

---

# 16. Physical Circulation Policy

V2 **không tạo `borrowing_policy` table**.

Các rule hiện chỉ có một institutional policy, ví dụ:

```text
commitment limit
loan period
request expiration
pickup expiration
```

Có thể tiếp tục nằm trong application configuration.

Chỉ tạo persisted policy model khi xuất hiện requirement như:

```text
Student → max 5
Faculty → max 10
External → max 2
```

và các policy cần được quản trị động.

---

# 17. Institution Model

V2 **không tạo `institution` table**.

Assumption:

```text
one deployment
=
one institutional library
```

Do đó:

- physical items mặc định thuộc library hiện tại;
- `INSTITUTIONAL` reader mặc định thuộc institution hiện tại;
- `INSTITUTIONAL` digital policy mặc định nói tới institution hiện tại.

Multi-institution support là V3+ design problem.

---

# 18. Entitlement Model

V2 **không tạo generic `entitlement` table**.

Effective digital access được resolve từ explicit data:

```text
DigitalItem.fullAccessPolicy
+
Account.readerType
+
future subscription data when supported
```

V2 target:

```text
PUBLIC
→ full access

INSTITUTIONAL + institutional reader
→ full access

otherwise
→ preview or no access
```

Future personal paid rule có thể được bổ sung vào access resolver sau này.

---

# 19. Resource Deletion

V2 ưu tiên archive thay vì hard delete.

```text
ACTIVE
→ normal catalog behavior

ARCHIVED
→ hidden/inactive catalog resource
   but historical references remain
```

Không sử dụng:

```text
ON DELETE CASCADE
```

như default strategy cho historical circulation data.

PhysicalItem có thể được `WITHDRAWN` thay vì delete khi đã tham gia circulation.

---

# 20. Referential Integrity

Các core relationship phải sử dụng FK:

```text
physical_item.resource_id
→ resource.id

digital_item.resource_id
→ resource.id

borrow_request.reader_id
→ accounts.id

borrow_request.resource_id
→ resource.id

borrow_request.physical_item_id
→ physical_item.id

borrowing.reader_id
→ accounts.id

borrowing.physical_item_id
→ physical_item.id

borrowing.borrow_request_id
→ borrow_request.id
```

Tương tự cho paid-access tables.

Historical core record không được cascade-delete tùy tiện.

---

# 21. CHECK Constraints

Database phải tiếp tục bảo vệ enum-like state:

```text
account role
account status
reader type

resource status
metadata source

inventory status
circulation status

borrow request status

digital full-access policy

payment status
```

Application enum và DB CHECK phải đồng bộ.

---

# 22. Migration Strategy

## 22.1 Vấn đề hiện tại

V1 `schema.sql` đang làm đồng thời:

```text
fresh schema creation
+
ALTER existing schema
+
backfill
+
constraint repair
+
legacy cleanup
```

và chạy khi application startup.

Đây không phải lifecycle phù hợp cho V2 lâu dài.

---

## 22.2 V2 sử dụng versioned migrations

V2 target sử dụng:

```text
Flyway
```

với migration có thứ tự rõ ràng.

Ví dụ:

```text
db/migration/
├── V1__baseline_schema.sql
├── V2__v2_account_and_resource_model.sql
├── V3__digital_access_policy.sql
└── V4__decouple_paid_access.sql
```

Tên/version thực tế có thể khác tùy cách triển khai.

Nguyên tắc:

> Một migration đã được áp dụng không bị sửa lại tùy tiện; thay đổi mới tạo migration mới.

---

## 22.3 Baseline từ V1

Khi migrate project:

1. Xác định schema V1 cuối cùng.
2. Baseline schema/history.
3. Tạo migration V2 để:
   - thêm `reader_type`;
   - bỏ `membership_eligible`;
   - thêm `resource_status`;
   - thêm digital access policy;
   - bỏ `monthly_borrow_quota`;
   - rename paid-access tables nếu thực hiện;
   - cleanup redundant fields khi an toàn.
4. Verify data integrity.
5. Loại bỏ startup schema repair script cũ.

---

# 23. Seed Strategy

V1 `data.sql` đang trộn:

```text
demo catalog
demo physical inventory
digital sample
paid plan configuration
```

V2 tách chúng.

---

## 23.1 Production

Production migration chỉ chứa:

- schema;
- required reference data;
- required invariant-preserving initialization.

Không tự seed:

```text
Clean Code
Refactoring
demo accounts
fake borrowings
```

---

## 23.2 Development / Demo

Demo data thuộc dev/demo profile hoặc dedicated seed mechanism.

Ví dụ:

```text
db/demo/
demo-data.sql
```

hoặc application dev seeder.

---

## 23.3 Test

Automated tests tự quản lý deterministic test fixtures.

Không phụ thuộc production/demo seed ngẫu nhiên.

---

# 24. Testing Database Behavior

Fast application tests có thể dùng H2 khi behavior không phụ thuộc PostgreSQL-specific semantics.

Các phần sau nên được verify bằng PostgreSQL:

- partial unique index;
- pessimistic locking;
- concurrent allocation;
- migration;
- PostgreSQL-specific constraints/query;
- production schema compatibility.

Có thể dùng Testcontainers PostgreSQL khi bước implementation tới phần này.

---

# 25. Những thay đổi chính từ V1

```text
accounts.membership_eligible
→ DROP

accounts.reader_type
→ ADD

resource.resource_status
→ ADD

digital_item.full_access_policy
→ ADD

membership_plan.monthly_borrow_quota
→ DROP

membership_plan
→ access_plan

membership_subscription
→ access_subscription

BorrowRequest created_at/updated_at
→ candidate DROP if redundant

schema.sql mega lifecycle
→ DROP after migration

data.sql global demo seed
→ SPLIT
```

---

# 26. Những table cố ý chưa tạo

V2 chưa tạo:

```text
institution
institution_license
entitlement
author
borrowing_policy
waitlist
inter_library_loan
order
shopping_cart
fine
```

Chúng chỉ được thêm khi có requirement thực tế.

---

# 27. V2 Database Principles

1. Database bảo vệ core invariant, không chỉ lưu object.
2. Historical circulation data không bị cascade-delete.
3. Resource, physical inventory và digital representation là các persistence concern riêng.
4. Physical circulation không phụ thuộc paid subscription.
5. Effective digital access được derive.
6. Persisted `circulation_status` được giữ như operational state có invariant rõ.
7. Không tạo multi-institution schema cho single-institution product.
8. Không tạo generic entitlement model quá sớm.
9. Schema evolution dùng versioned migrations.
10. Demo data không phải production data.
11. Index phải phục vụ query/invariant thực tế.
12. Database design được phép tiến hóa khi product requirement thay đổi.