# Librio — Domain Model

## 1. Mục đích

Tài liệu này mô tả domain model của Librio V2.

Mục tiêu của `domain.md` là trả lời:

- Librio có những business concept nào;
- mỗi concept có ý nghĩa gì;
- chúng liên hệ với nhau như thế nào;
- lifecycle và invariant quan trọng là gì;
- state nào là persisted business state và state nào nên được derive;
- boundary nào thuộc physical circulation, digital access hoặc paid access.

Tài liệu này **không định nghĩa database schema**.

Table, column, index, foreign key và migration strategy thuộc `database.md`.

---

# 2. Bối cảnh domain

Librio V2 là:

> **Library + Digital Content Platform cho một institutional library.**

V2 giả định một deployment phục vụ một library/institution chính.

```text
Librio deployment
        |
        v
Institution / Library
        |
        +---- Physical collection
        |
        +---- Digital access
        |
        +---- Readers
        |
        +---- Librarians
```

V2 chưa phải multi-tenant platform.

Các ví dụ như FPT và Bách Khoa được dùng để reasoning về domain, nhưng một instance V2 không cần đồng thời quản lý cả hai institution.

---

# 3. Actor

## 3.1 Reader

`Reader` là user sử dụng tài nguyên thư viện.

Trong V2, physical circulation chủ yếu dành cho **Institutional Reader**.

Reader có thể:

- browse/search resource;
- xem resource detail;
- xem physical availability;
- xem digital access;
- tạo borrow request nếu đủ điều kiện;
- xem request/borrowing của mình;
- truy cập digital content nếu được cấp quyền.

---

## 3.2 Librarian

`Librarian` là operator của library.

Librarian có thể:

- maintain resource metadata;
- maintain physical inventory;
- maintain digital representation;
- xử lý circulation request;
- prepare/fulfil/reject request;
- xử lý return;
- hỗ trợ metadata lookup.

`Librarian` không mặc định có nghĩa là employee của một university cụ thể.

Nó là role vận hành library hiện tại.

---

# 4. Identity, Role và Affiliation

## 4.1 Account

`Account` đại diện cho identity được Librio nhận diện.

Account chịu trách nhiệm cho:

- login identity;
- display information;
- account lifecycle;
- authorization role.

Các role hiện tại:

```text
READER
LIBRARIAN
```

---

## 4.2 Role không phải affiliation

Ba concept sau phải được phân biệt:

```text
Role
≠
Institutional affiliation
≠
Access entitlement
```

Ví dụ:

```text
role = READER
```

chỉ nói rằng account sử dụng hệ thống với tư cách reader.

Nó không tự động cho biết:

- reader có thuộc institution hay không;
- reader được phép mượn physical item hay không;
- reader được quyền đọc digital content nào;
- reader có paid subscription hay không.

---

## 4.3 Institutional Reader

V2 cần concept:

> Reader thuộc institution/library hiện tại và đủ điều kiện sử dụng institutional services.

Ví dụ:

```text
Account
role = READER

institutional affiliation = ACTIVE
```

có thể được phép sử dụng physical circulation.

Cách persist affiliation:

- field;
- enum;
- structure riêng;

không được quyết định ở domain layer.

---

## 4.4 External Reader

External Reader là reader không có institutional affiliation hiện hành.

V2 không yêu cầu mở full external-user product.

Đặc biệt:

```text
External Reader
→ không có physical borrowing mặc định trong V2
```

Paid digital access cho external reader là direction của V3+.

---

# 5. Resource

## 5.1 Resource

`Resource` là logical/bibliographic resource mà người dùng discover.

Ví dụ:

```text
Clean Code
```

là một `Resource`.

Resource có thể chứa metadata như:

- title;
- author information;
- description;
- category;
- ISBN;
- cover;
- metadata provenance.

Resource không phải một physical copy cụ thể.

Resource cũng không phải file digital cụ thể.

---

## 5.2 Resource hiện thiên về book-like content

Concept `Resource` có thể mở rộng cho nhiều loại content.

Tuy nhiên implementation V2 hiện chủ yếu phục vụ book-like resource.

Không cần thêm hierarchy như:

```text
Book
Article
Video
Audio
Thesis
...
```

nếu chưa có requirement thực tế.

---

# 6. Physical Representation

## 6.1 PhysicalItem

`PhysicalItem` là một physical copy cụ thể của Resource.

Ví dụ:

```text
Resource: Clean Code

PhysicalItem #101
barcode = LIB-101

PhysicalItem #102
barcode = LIB-102
```

Hai item là hai asset vật lý riêng biệt.

---

## 6.2 Inventory state

Inventory state mô tả condition/lifecycle của physical asset.

Hiện tại:

```text
ACTIVE
LOST
DAMAGED
WITHDRAWN
```

Ý nghĩa:

- `ACTIVE`: item đang thuộc active inventory;
- `LOST`: item bị mất;
- `DAMAGED`: item bị hỏng;
- `WITHDRAWN`: item bị rút khỏi circulation/inventory sử dụng.

---

## 6.3 Circulation state

Circulation state mô tả item đang được sử dụng trong circulation như thế nào.

Hiện tại:

```text
AVAILABLE
RESERVED
BORROWED
```

Hai nhóm state khác nhau:

```text
Inventory state
≠
Circulation state
```

Ví dụ:

```text
inventoryStatus = ACTIVE
circulationStatus = BORROWED
```

là hợp lệ.

---

## 6.4 Borrowable item

Trong baseline hiện tại, physical item borrowable khi:

```text
inventoryStatus = ACTIVE
AND
circulationStatus = AVAILABLE
```

Availability ở reader-facing resource level được aggregate từ các physical items.

---

# 7. BorrowRequest

## 7.1 Ý nghĩa

`BorrowRequest` đại diện cho intention/workflow trước khi actual loan bắt đầu.

Reader request một `Resource`.

Server chọn một exact `PhysicalItem`.

```text
Reader
  |
  v
BorrowRequest
  |
  +---- Resource
  |
  +---- allocated PhysicalItem
```

---

## 7.2 Exact-copy reservation

Khi BorrowRequest hợp lệ, server reserve một exact physical item.

Ví dụ:

```text
Clean Code

#101 AVAILABLE
#102 AVAILABLE
```

Request có thể dẫn tới:

```text
BorrowRequest
→ #101

#101
AVAILABLE → RESERVED
```

Mục tiêu là ngăn competing request cùng claim một copy.

---

## 7.3 Exact reservation không phải waitlist

Hai concept khác nhau:

```text
Exact-copy reservation
≠
Waitlist / hold queue
```

V2 có exact-copy reservation.

V2 không yêu cầu queue cho trường hợp:

```text
all copies unavailable
→ reader joins waiting list
```

---

# 8. BorrowRequest Lifecycle

Lifecycle hiện tại:

```text
REQUESTED
      |
      v
READY_FOR_PICKUP
      |
      v
FULFILLED
```

Các terminal outcome khác:

```text
CANCELLED
REJECTED
EXPIRED
```

---

## 8.1 REQUESTED

Request đã được tạo và exact item đã được reserve.

---

## 8.2 READY_FOR_PICKUP

Librarian đã chuẩn bị physical item để giao.

State này phân biệt:

```text
request accepted
≠
item physically prepared
```

V2 tạm giữ state này.

Nếu implementation repair cho thấy preparation stage không mang đủ product value, lifecycle có thể được simplify.

---

## 8.3 FULFILLED

Physical item đã thực sự được giao cho reader.

Fulfilment chuyển domain từ:

```text
request/reservation
```

sang:

```text
actual borrowing
```

---

## 8.4 CANCELLED / REJECTED / EXPIRED

Các trạng thái này kết thúc request mà không tạo actual borrowing.

Exact reservation phải được release.

```text
PhysicalItem
RESERVED → AVAILABLE
```

nếu không có rule khác ngăn cản.

---

# 9. Borrowing

## 9.1 Ý nghĩa

`Borrowing` đại diện cho actual physical loan.

Nó chỉ tồn tại sau khi BorrowRequest được fulfil.

```text
BorrowRequest
      |
      | fulfil
      v
Borrowing
```

BorrowRequest và Borrowing không được gộp thành cùng một concept.

---

## 9.2 Borrowing data

Một Borrowing cần đủ thông tin để biết:

- reader;
- exact physical item;
- source BorrowRequest;
- borrowed time;
- due time;
- returned time nếu đã trả.

---

## 9.3 Active borrowing

Borrowing active khi:

```text
returnedAt == null
```

---

## 9.4 Overdue

Overdue là derived domain state.

```text
overdue =
returnedAt == null
AND
dueAt < current time
```

Không cần business field độc lập kiểu:

```text
isOverdue = true
```

làm source of truth.

---

## 9.5 Return

Return kết thúc active borrowing.

```text
Borrowing
returnedAt = now

PhysicalItem
BORROWED → AVAILABLE
```

nếu inventory state vẫn cho phép item quay lại circulation.

---

# 10. Physical Circulation Invariants

Các invariant quan trọng:

## 10.1 Một exact item chỉ có một active request allocation

Không hợp lệ:

```text
PhysicalItem #101

BorrowRequest A → active
BorrowRequest B → active
```

---

## 10.2 Một exact item chỉ có một active borrowing

Không hợp lệ:

```text
PhysicalItem #101

Borrowing A → active
Borrowing B → active
```

---

## 10.3 Một active reservation phải có lifecycle owner

Nếu:

```text
PhysicalItem.circulationStatus = RESERVED
```

thì phải tồn tại active request tương ứng.

---

## 10.4 Một BORROWED item phải có active borrowing

Nếu:

```text
PhysicalItem.circulationStatus = BORROWED
```

thì phải tồn tại active Borrowing tương ứng.

---

## 10.5 Terminal request không được giữ orphan reservation

```text
CANCELLED
REJECTED
EXPIRED
```

phải release reservation.

`FULFILLED` phải consume reservation thành actual borrowing.

---

## 10.6 Circulation operations phải atomic

Ví dụ fulfil:

```text
BorrowRequest → FULFILLED
PhysicalItem  → BORROWED
Borrowing     → created
```

phải được xem như một business operation nhất quán.

---

# 11. DigitalItem

## 11.1 Ý nghĩa

`DigitalItem` đại diện cho digital representation/content associated với một Resource.

Ví dụ:

```text
Resource: Clean Code
└── DigitalItem
```

DigitalItem khác PhysicalItem.

Nó không tham gia physical circulation lifecycle.

---

## 11.2 Digital existence khác digital access

Ba câu hỏi sau phải được tách biệt:

```text
1. Digital representation có tồn tại không?
2. Institution hiện tại có cấp quyền cho content này không?
3. User hiện tại có quyền full access không?
```

Không được gộp thành một boolean `digitalAvailable`.

---

# 12. Digital Access

## 12.1 Digital access là server-authoritative

Client không tự quyết định user có quyền full content.

Server resolve access capability.

---

## 12.2 Access source

V2 reasoning sử dụng các access source:

```text
PUBLIC
INSTITUTIONAL
PERSONAL_PAID
```

Trong đó:

### PUBLIC

Content được phép truy cập công khai.

---

### INSTITUTIONAL

Institution/library hiện tại có quyền cấp access cho institutional reader.

Ví dụ:

```text
FPT licenses digital Book A
Student FPT
→ full access
```

---

### PERSONAL_PAID

User có quyền access thông qua paid product cá nhân.

Đây là domain direction hợp lệ nhưng chưa phải capability bắt buộc của V2.

---

## 12.3 Effective Digital Access

Conceptually:

```text
Effective Digital Access
=
Public access
OR
Institutional access
OR
Personal paid access
```

Trong V2, implementation bắt buộc tối thiểu phải reasoning được:

```text
PUBLIC
INSTITUTIONAL
NO_FULL_ACCESS
```

`PERSONAL_PAID` có thể được hoàn thiện ở V3+.

---

## 12.4 Preview

Preview là partial content access.

Preview không đồng nghĩa full entitlement.

Ví dụ:

```text
Full access → NO
Preview     → YES
```

là trạng thái hợp lệ.

---

# 13. Physical và Digital độc lập

Physical holding và digital entitlement là hai dimension độc lập.

Ví dụ:

```text
Book D

Physical copies:
3

Institutional digital access:
NO
```

Institutional reader có thể:

```text
borrow physical → YES
read full digital → NO
```

Ngược lại:

```text
Physical copies:
0

Institutional digital access:
YES
```

thì:

```text
borrow physical → NO
read full digital → YES
```

---

# 14. Metadata và ISBN

## 14.1 ISBN

ISBN là bibliographic identifier cho book-like resource.

V2 tiếp tục sử dụng canonical ISBN policy hiện tại.

ISBN uniqueness vẫn được giữ trong baseline.

Edition semantics có thể được review nếu catalog model phát triển sâu hơn.

---

## 14.2 Metadata Source

Metadata có thể được tạo:

```text
MANUAL
GOOGLE_BOOKS
```

`metadataSource` lưu provenance của catalog metadata.

---

## 14.3 External Metadata Provider

Google Books là provider implementation hiện tại.

Domain capability là:

> Lookup bibliographic metadata từ external provider.

Không phải:

> Librio phụ thuộc vĩnh viễn vào Google Books.

---

## 14.4 Editable prefill

External metadata không tự động trở thành catalog truth.

```text
lookup
→ provider result
→ editable prefill
→ librarian review
→ persist
```

---

# 15. Paid Access Domain

## 15.1 Physical circulation không phụ thuộc paid membership

V1 từng coupling:

```text
active membership
→ borrowing eligibility
```

V2 bỏ assumption này.

Physical borrowing thuộc institutional circulation policy.

---

## 15.2 Paid Access Plan

Concept paid plan có thể tồn tại để cung cấp digital access trong tương lai.

Một plan có thể mô tả:

- duration;
- price;
- currency;
- digital access benefits.

Physical borrowing quota không phải responsibility mặc định của paid access plan.

---

## 15.3 Subscription

`Subscription` đại diện cho một paid access period của account.

Concept lifecycle:

```text
startsAt
expiresAt
```

Tên implementation hiện tại có thể vẫn là:

```text
MembershipSubscription
```

nhưng terminology này cần review vì "membership" có thể gây nhầm với institutional membership.

---

## 15.4 PaymentTransaction

`PaymentTransaction` đại diện cho payment attempt/record.

Payment có lifecycle độc lập:

```text
PENDING
SUCCESS
FAILED
```

Payment có thể dẫn tới paid subscription activation.

Payment không trực tiếp tạo physical borrowing rights.

---

# 16. Domain Boundaries

V2 nên reasoning theo các boundary sau.

## 16.1 Catalog

```text
Resource
metadata
ISBN
external metadata lookup
```

---

## 16.2 Physical Inventory

```text
PhysicalItem
barcode
location
inventory state
```

---

## 16.3 Circulation

```text
BorrowRequest
reservation
Borrowing
due date
overdue
return
```

---

## 16.4 Digital Content

```text
DigitalItem
preview/full content
content reference
digital access decision
```

---

## 16.5 Identity & Security

```text
Account
Role
AccountStatus
Institutional eligibility
```

---

## 16.6 Paid Access

```text
Access Plan
Subscription
PaymentTransaction
```

Paid Access không nằm bên trong Physical Circulation.

---

# 17. Derived State vs Stored State

Domain phân biệt các state sau.

## Stored / operational state

Ví dụ:

```text
PhysicalItem.inventoryStatus
PhysicalItem.circulationStatus
BorrowRequest.status
Account.status
Payment.status
```

---

## Derived state

Ví dụ:

```text
overdue
physical availability summary
effective digital access
```

Derived state không nên trở thành duplicate source of truth nếu có thể tính từ authoritative data.

---

# 18. Single-Institution Assumption

V2 giả định:

```text
one deployment
≈
one library / institution
```

Do đó chưa cần bắt buộc:

```text
Institution entity
institution_id trên mọi record
multi-tenant boundary
```

Nếu V3+ cần một Librio instance quản lý nhiều institution, assumption này phải được review lại.

---

# 19. Concept chưa cần entity generic

Một số từ được dùng để reasoning nhưng chưa bắt buộc phải persist thành entity.

## 19.1 Entitlement

`Entitlement` có nghĩa:

> lý do hợp lệ khiến user được phép sử dụng capability/content.

Ví dụ:

```text
institutional affiliation
public content
paid subscription
```

V2 không cần generic `Entitlement` entity nếu policy/service cụ thể đã đủ rõ.

---

## 19.2 BorrowingPolicy

Các rule như:

```text
max concurrent borrowing
loan period
```

có thể tiếp tục ở configuration/policy code nếu chỉ có một institutional policy.

Chỉ cần persisted policy model khi business thực sự có nhiều policy cần quản lý.

---

# 20. Các quyết định chưa chốt ở domain layer

Các điểm sau được cố ý để phần design xử lý:

- institutional affiliation được persist chính xác thế nào;
- `READY_FOR_PICKUP` có được giữ trong implementation V2 cuối hay simplify;
- BorrowRequest cần giữ những audit field nào;
- `circulationStatus` được duy trì như persisted operational state cụ thể ra sao;
- digital access classification được persist bằng field/enum/model nào;
- `MembershipPlan` / `MembershipSubscription` có rename hay không;
- Resource hard-delete hay archive;
- storage provider;
- database migration implementation.

---

# 21. V2 Domain Model tổng quát

```text
                         Account
                        /       \
                   READER      LIBRARIAN
                      |
            institutional eligibility
                      |
                      v
                   Resource
                 /          \
                /            \
       PhysicalItem        DigitalItem
            |                  |
            |             Digital Access
            |             Resolution
            |
       BorrowRequest
            |
         Borrowing
```

Paid access nằm cạnh digital access:

```text
Account
   |
   v
Access Subscription
   |
   +---- Access Plan
   |
   +---- PaymentTransaction
   |
   v
Digital Access Resolution
```

---

# 22. Nguyên tắc domain của V2

1. Resource không phải physical copy.
2. Physical và digital access là hai capability độc lập.
3. BorrowRequest không phải Borrowing.
4. Inventory state không phải circulation state.
5. Overdue được derive.
6. Availability do server quyết định.
7. Role không phải affiliation.
8. Affiliation không phải entitlement.
9. Payment không phải circulation.
10. Digital content existence không phải digital access.
11. Không tạo generic entity chỉ vì tương lai có thể cần.
12. V2 ưu tiên domain model nhỏ, rõ và có thể mở rộng hơn là abstraction tổng quát quá sớm.