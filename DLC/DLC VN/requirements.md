# Librio — Yêu cầu hệ thống

## 1. Mục đích

Tài liệu này mô tả các yêu cầu của Librio V2 trong quá trình chuyển từ Mock Project thành một baseline cá nhân có thể tiếp tục phát triển.

V2 chủ yếu là giai đoạn repair/refactor. Mục tiêu không phải mở rộng mạnh feature set mà là:

- bảo toàn các behavior và domain decision còn giá trị từ V1;
- sửa các assumption không còn phù hợp;
- tách các responsibility đang bị coupling sai;
- tạo một baseline có thể maintain, test và deploy lâu dài.

Các yêu cầu được chia thành:

- **Yêu cầu chức năng (Functional Requirements):** behavior mà actor hoặc system có thể thực hiện hoặc quan sát.
- **Business Rules:** các luật mà behavior của hệ thống phải tuân theo.
- **Yêu cầu phi chức năng (Non-functional Requirements):** các thuộc tính về consistency, security, maintainability, testability và deployment.
- **Phạm vi V2:** capability phải được bảo toàn, repair hoặc chưa thuộc V2.

### Trạng thái

- **[GIỮ]** — behavior hoặc rule của V1 vẫn phù hợp và phải được preserve.
- **[V2 REPAIR]** — behavior/concept cần thay đổi để phù hợp với baseline mới.
- **[LÀM RÕ]** — intent đã có nhưng quyết định chi tiết chưa cần chốt ở requirements layer.
- **[NGOÀI V2]** — không phải deliverable bắt buộc của V2.

---

# 2. Yêu cầu chức năng

## 2.1 Khám phá tài nguyên

### FR-DIS-01 — Duyệt tài nguyên
**[GIỮ]**

Người dùng phải có khả năng duyệt danh sách tài nguyên của thư viện.

Mỗi resource phải hiển thị đủ thông tin tóm tắt để người dùng quyết định có mở chi tiết hay không.

---

### FR-DIS-02 — Tìm kiếm tài nguyên
**[GIỮ]**

Người dùng phải có khả năng tìm kiếm tài nguyên dựa trên metadata mà hệ thống hiện hỗ trợ.

V2 không yêu cầu dedicated search engine hoặc advanced search architecture nếu search hiện tại vẫn đáp ứng product need.

---

### FR-DIS-03 — Xem chi tiết tài nguyên
**[GIỮ]**

Người dùng phải có khả năng xem chi tiết một resource để:

- hiểu tài nguyên đó là gì;
- biết tài nguyên có physical form, digital form hoặc cả hai;
- xem physical availability;
- biết digital access hiện có thể được sử dụng hay không.

---

### FR-DIS-04 — Xem physical availability
**[GIỮ]**

Nếu resource có physical copies, hệ thống phải cung cấp availability được derive từ trạng thái thực tế của các copy.

Ví dụ:

```text
Clean Code

5 physical copies
2 currently borrowable
```

Client không được tự suy đoán availability độc lập với server.

---

## 2.2 Identity & Access

### FR-ID-01 — Đăng nhập
**[GIỮ]**

User phải có khả năng đăng nhập bằng account hợp lệ.

Hệ thống phải duy trì authenticated session và xác định được account hiện tại.

Registration không phải requirement bắt buộc của V2.

---

### FR-ID-02 — Phân biệt Reader và Librarian
**[GIỮ]**

Hệ thống phải phân biệt ít nhất hai authorization role:

```text
READER
LIBRARIAN
```

Role được dùng để quyết định actor có thể thực hiện operation nào.

Role không được dùng để thay thế các concept khác như institutional affiliation hoặc paid entitlement.

---

### FR-ID-03 — Institutional Reader
**[V2 REPAIR]**

V2 phải có đủ thông tin để xác định reader có thuộc nhóm được phép sử dụng circulation của institution/library hiện tại hay không.

V2 giả định:

> Một Librio deployment phục vụ một library/institution chính.

V2 không yêu cầu generic multi-institution model.

Cách persist institutional status được quyết định trong `domain.md` và `database.md`.

---

## 2.3 Physical Circulation

### FR-CIR-01 — Gửi yêu cầu mượn resource vật lý
**[GIỮ]**

Institutional Reader đủ điều kiện phải có khả năng gửi yêu cầu mượn một resource có physical copy khả dụng.

Reader yêu cầu **resource**, còn server chịu trách nhiệm chọn một exact physical copy phù hợp.

---

### FR-CIR-02 — Reserve exact physical copy
**[GIỮ]**

Khi yêu cầu mượn được chấp nhận, hệ thống phải reserve một exact physical item để tránh cùng một copy bị cấp cho nhiều request cạnh tranh.

Ví dụ:

```text
Resource: Clean Code

#101 AVAILABLE
#102 AVAILABLE
```

Request của Hoàng có thể được allocate:

```text
BorrowRequest → PhysicalItem #101
#101 → RESERVED
```

Đây là exact-copy reservation phục vụ circulation workflow.

Nó không đồng nghĩa với waitlist/hold queue cho resource đang hết sách.

---

### FR-CIR-03 — Chuẩn bị sách để nhận
**[GIỮ / LÀM RÕ]**

Librarian có thể chuyển request đã hợp lệ sang trạng thái sẵn sàng giao cho reader.

V1 sử dụng:

```text
REQUESTED
→ READY_FOR_PICKUP
→ FULFILLED
```

V2 tạm giữ behavior này. Nếu implementation review cho thấy bước preparation không còn mang product value, lifecycle có thể được đơn giản hóa mà không thay đổi intent của circulation.

---

### FR-CIR-04 — Fulfil request và tạo borrowing
**[GIỮ]**

Khi physical item thực sự được giao cho reader:

- BorrowRequest phải được hoàn tất;
- Borrowing phải được tạo;
- exact physical item phải chuyển sang trạng thái borrowed;
- due date phải được gán.

---

### FR-CIR-05 — Hủy, từ chối hoặc expire request
**[GIỮ]**

Request chưa được fulfil có thể kết thúc bằng các outcome hợp lệ như:

```text
CANCELLED
REJECTED
EXPIRED
```

Nếu request kết thúc mà chưa tạo borrowing, exact physical item đã reserve phải được release phù hợp.

---

### FR-CIR-06 — Xem borrowing của reader
**[GIỮ]**

Reader phải có khả năng xem các borrowing liên quan tới account của mình, gồm tối thiểu:

- resource;
- physical item khi cần;
- borrowed time;
- due date;
- trạng thái return/overdue liên quan.

---

### FR-CIR-07 — Librarian xử lý circulation
**[GIỮ]**

Librarian phải có khả năng xem và xử lý các request/borrowing cần thiết cho circulation workflow.

---

### FR-CIR-08 — Trả physical item
**[GIỮ]**

Librarian phải có khả năng hoàn tất việc trả sách.

Khi return thành công:

- borrowing được ghi nhận là returned;
- exact physical item được release khỏi trạng thái borrowed;
- item có thể trở lại borrowable nếu inventory state cho phép.

---

## 2.4 Digital Access

### FR-DIG-01 — Nhận biết digital form
**[GIỮ]**

Nếu một resource có digital representation, người dùng phải có khả năng nhận biết điều đó.

Sự tồn tại của digital form không tự động đồng nghĩa user có full access.

---

### FR-DIG-02 — Preview digital content
**[GIỮ / V2 REPAIR]**

Nếu resource cung cấp preview, hệ thống có thể cho phép user truy cập preview mà không cần full digital entitlement.

Preview và full content phải được server phân biệt.

---

### FR-DIG-03 — Truy cập full digital content
**[V2 REPAIR]**

Server phải là nơi quyết định user có quyền truy cập full digital content hay không.

Trong V2, digital access phải có khả năng phân biệt tối thiểu:

```text
public/free access
institutional access
no full access
```

V2 không bắt buộc phải productize paid external access.

Tuy nhiên design không được giả định rằng paid membership là nguồn entitlement duy nhất.

---

### FR-DIG-04 — Digital content delivery
**[V2 REPAIR]**

V2 phải loại bỏ sự phụ thuộc vào demo-generated content như implementation lâu dài.

Digital content phải có một content reference/delivery mechanism có thể thay thế được.

Cloud storage hoặc provider cụ thể không bắt buộc trong V2.

---

## 2.5 Resource & Inventory Administration

### FR-ADM-01 — Tạo và cập nhật resource
**[GIỮ]**

Librarian phải có khả năng tạo và cập nhật resource metadata cần thiết cho catalog.

---

### FR-ADM-02 — Quản lý physical item
**[GIỮ]**

Librarian phải có khả năng quản lý physical copies, gồm các thông tin cần thiết như:

- barcode;
- location;
- inventory status;
- circulation-related state.

System phải ngăn các transition có thể phá active circulation.

---

### FR-ADM-03 — Quản lý digital representation
**[GIỮ / V2 REPAIR]**

Librarian phải có khả năng duy trì digital representation cần thiết cho resource.

Digital content storage và access policy không được trộn thành cùng một concept nếu chúng có responsibility khác nhau.

---

### FR-ADM-04 — Điều chỉnh số lượng physical copy an toàn
**[GIỮ]**

Nếu resource administration thay đổi số lượng physical copies, system không được xóa hoặc withdraw copy đang thuộc active request/borrowing theo cách làm mất circulation integrity.

---

## 2.6 ISBN & External Metadata

### FR-META-01 — ISBN
**[GIỮ]**

Resource dạng sách có thể có ISBN.

System phải normalize ISBN về canonical format mà implementation hỗ trợ và ngăn duplicate ISBN theo policy hiện tại.

---

### FR-META-02 — Lookup metadata từ external provider
**[GIỮ]**

Librarian có thể sử dụng ISBN để lookup bibliographic metadata từ provider ngoài.

Google Books là provider hiện tại nhưng không phải domain dependency cố định.

---

### FR-META-03 — External metadata chỉ là editable prefill
**[GIỮ]**

Metadata lấy từ provider ngoài không được tự động trở thành source of truth mà không qua user action.

Flow mong muốn:

```text
ISBN lookup
→ provider result
→ prefill form
→ librarian review/edit
→ persist
```

---

## 2.7 Paid Access Capability

### FR-PAY-01 — Paid access không quyết định physical borrowing
**[V2 REPAIR]**

Physical circulation V2 không được yêu cầu active paid membership như điều kiện mặc định.

Institutional borrowing eligibility phải độc lập với payment/subscription.

---

### FR-PAY-02 — Payment/subscription được tách khỏi circulation
**[V2 REPAIR]**

Nếu V1 payment và subscription implementation được giữ lại, chúng phải tồn tại như một capability riêng phục vụ future paid digital access.

Một payment thành công có thể tạo hoặc activate subscription/access record.

Nó không được trực tiếp tạo physical borrowing privilege.

---

### FR-PAY-03 — External paid digital access
**[NGOÀI V2]**

V2 chưa bắt buộc mở paid digital subscription cho external users.

Đây là candidate của V3+.

V2 chỉ cần tránh thiết kế domain theo cách khiến capability này không thể bổ sung hợp lý sau này.

---

# 3. Business Rules

## BR-01 — Resource khác với physical và digital representation
**[GIỮ]**

`Resource` mô tả tài nguyên logical/bibliographic.

Physical và digital form là các representation/access form liên quan tới resource.

```text
Resource: Clean Code
├── PhysicalItem #101
├── PhysicalItem #102
└── DigitalItem
```

---

## BR-02 — Physical copy phải thuộc inventory của library
**[V2 REPAIR]**

Library chỉ có thể cho mượn physical item mà deployment hiện tại thực sự quản lý.

Paid subscription không tạo ra physical inventory.

---

## BR-03 — Physical item borrowable khi inventory và circulation đều hợp lệ
**[GIỮ]**

Một item chỉ được coi là borrowable khi thỏa mãn các state phù hợp, hiện tại tương ứng với:

```text
inventoryStatus = ACTIVE
circulationStatus = AVAILABLE
```

---

## BR-04 — Một physical item chỉ có tối đa một active request allocation
**[GIỮ]**

Một exact physical copy không được reserve đồng thời cho nhiều active BorrowRequest.

---

## BR-05 — Một physical item chỉ có tối đa một active borrowing
**[GIỮ]**

Một exact copy không được thuộc nhiều active borrowing cùng lúc.

---

## BR-06 — Một reader không được có duplicate active circulation cho cùng resource
**[GIỮ]**

Reader không được tạo thêm active request cho resource mà họ đã có active request hoặc active borrowing tương ứng theo policy hiện tại.

---

## BR-07 — BorrowRequest khác Borrowing
**[GIỮ]**

`BorrowRequest` mô tả request/reservation workflow trước checkout.

`Borrowing` mô tả actual loan sau khi item thực sự được giao.

---

## BR-08 — Terminal request phải release hoặc consume reservation đúng cách
**[GIỮ]**

```text
CANCELLED / REJECTED / EXPIRED
→ release reserved item

FULFILLED
→ reservation được consume thành borrowing
```

System không được để orphan reservation.

---

## BR-09 — Physical borrowing phải có due date
**[GIỮ]**

Mọi active borrowing phải có `dueAt`.

---

## BR-10 — Overdue là derived state
**[GIỮ]**

Borrowing được coi là overdue khi:

```text
returnedAt == null
AND dueAt < current time
```

Không cần lưu một `isOverdue` riêng làm source of truth.

---

## BR-11 — Overdue không tự tạo fine/payment
**[GIỮ]**

V2 không tự động tạo billing obligation khi borrowing quá hạn.

---

## BR-12 — Inventory state khác circulation state
**[GIỮ]**

Các state như:

```text
LOST
DAMAGED
WITHDRAWN
```

không cùng dimension với:

```text
AVAILABLE
RESERVED
BORROWED
```

Hai nhóm state phải được xử lý độc lập nhưng consistent.

---

## BR-13 — Availability do server quyết định
**[GIỮ]**

Client không được tự tạo source of truth riêng về availability.

Resource-level availability có thể được aggregate từ nhiều physical items.

---

## BR-14 — Digital existence khác digital entitlement
**[V2 REPAIR]**

Các câu hỏi sau là khác nhau:

```text
Digital edition/content có tồn tại không?
Institution hiện tại có cấp quyền không?
User hiện tại có quyền full access không?
```

Không được gộp chúng thành một boolean duy nhất.

---

## BR-15 — Digital access có thể có nhiều nguồn
**[V2 REPAIR]**

V2 phải cho phép reasoning theo các nguồn quyền khác nhau:

```text
public/free
institutional
personal paid access (future)
```

V2 chưa bắt buộc triển khai đầy đủ nguồn `personal paid`.

---

## BR-16 — Role khác affiliation và entitlement
**[V2 REPAIR]**

Ví dụ:

```text
role = READER
```

không tự động cho biết:

- reader thuộc institution hay không;
- resource nào họ được access;
- họ có paid subscription hay không.

---

## BR-17 — ISBN external lookup không tự ghi đè catalog
**[GIỮ]**

External provider chỉ hỗ trợ metadata acquisition.

Catalog của Librio vẫn là source of truth sau khi librarian review và persist.

---

# 4. Yêu cầu phi chức năng

## 4.1 Data Integrity & Concurrency

### NFR-CON-01 — Circulation invariant phải được bảo vệ
**[GIỮ]**

Concurrent request không được dẫn tới cùng một physical copy được reserve hoặc borrowed cho nhiều user.

---

### NFR-CON-02 — Business operation phải atomic
**[GIỮ]**

Operation gồm nhiều thay đổi liên quan phải commit hoặc rollback nhất quán.

Ví dụ fulfil:

```text
BorrowRequest → FULFILLED
PhysicalItem  → BORROWED
Borrowing     → created
```

không được chỉ thành công một phần.

---

### NFR-CON-03 — Database phải bảo vệ invariant phù hợp
**[GIỮ]**

Các invariant có thể biểu diễn hợp lý bằng FK, UNIQUE, CHECK hoặc database constraint nên được bảo vệ ở persistence layer thay vì chỉ dựa vào UI.

---

## 4.2 Security

### NFR-SEC-01 — Authentication và authorization phải được enforce ở server
**[GIỮ]**

Frontend routing hoặc ẩn button không được dùng làm security boundary.

---

### NFR-SEC-02 — Ownership phải được enforce
**[GIỮ]**

Reader chỉ được thao tác trên dữ liệu cá nhân/circulation mà họ có quyền thao tác.

---

### NFR-SEC-03 — Secret phải được externalize
**[GIỮ / V2 REPAIR]**

Database credential, payment secret và production secret không được hard-code trong source.

---

## 4.3 Maintainability

### NFR-MNT-01 — Responsibility phải rõ
**[V2 REPAIR]**

Business rule quan trọng phải có responsibility rõ ràng và không bị phân tán vô lý giữa controller, frontend, repository và SQL.

---

### NFR-MNT-02 — Không giữ Mock artifact chỉ vì đã tồn tại
**[V2 REPAIR]**

Code, config hoặc documentation chỉ phục vụ sprint/deadline/submission cũ có thể bị loại bỏ nếu không còn giá trị.

---

### NFR-MNT-03 — Không over-engineer future
**[V2 REPAIR]**

V2 không tạo abstraction hoặc entity generic chỉ vì feature tương lai có thể cần.

Ví dụ V2 chưa mặc định cần:

```text
Institution table
generic Entitlement table
BorrowingPolicy table
```

nếu use case hiện tại chưa chứng minh cần chúng.

---

## 4.4 Testability

### NFR-TST-01 — Core business invariant phải có automated tests
**[GIỮ / V2 REPAIR]**

Các behavior như:

- exact-copy reservation;
- duplicate prevention;
- fulfil;
- cancel/reject/expire release;
- return;
- inventory transition;
- authorization;
- external metadata verification;

phải có test phù hợp.

---

### NFR-TST-02 — Browser E2E không bắt buộc
**[V2 REPAIR]**

V2 không yêu cầu duy trì Playwright.

Critical behavior có thể được bảo vệ bằng service/API/integration tests nếu chúng cung cấp confidence phù hợp.

---

### NFR-TST-03 — Database-critical behavior phải được verify phù hợp
**[V2 REPAIR]**

H2 có thể tiếp tục phục vụ fast tests.

Các behavior phụ thuộc PostgreSQL semantics như locking, partial indexes, constraint hoặc migration nên có verification trên PostgreSQL khi cần.

---

## 4.5 Configuration & Deployment

### NFR-DEP-01 — Configuration phải externalized
**[GIỮ]**

Environment-specific configuration phải được truyền qua config/environment thay vì sửa source code.

---

### NFR-DEP-02 — Deployment phải reproducible
**[V2 REPAIR]**

Project phải có deployment path có thể thực hiện lại từ repository và documented configuration.

---

### NFR-DEP-03 — Database schema phải có lifecycle rõ
**[V2 REPAIR]**

V2 không nên tiếp tục phụ thuộc lâu dài vào một `schema.sql` vừa bootstrap vừa backfill vừa migrate schema.

Database changes phải chuyển dần sang versioned migration strategy.

---

### NFR-DEP-04 — Demo data phải tách khỏi production data lifecycle
**[V2 REPAIR]**

Demo resources/account/sample state không được mặc định seed vào mọi environment.

---

# 5. Phạm vi V2

## 5.1 V2 phải giữ hoặc repair

- resource browse/search/detail;
- physical/digital access-form discovery;
- server-authoritative availability;
- login/session/security;
- Reader/Librarian authorization;
- institutional physical circulation;
- exact-copy reservation;
- request lifecycle;
- borrowing/due date/overdue/return;
- physical inventory management;
- resource administration;
- digital representation và server-side access decision;
- ISBN normalization;
- Google Books/external metadata lookup;
- payment/subscription implementation nếu được giữ phải tách khỏi physical borrowing;
- automated tests bảo vệ core business rule;
- reproducible configuration/deployment.

---

## 5.2 Không bắt buộc trong V2

V2 không bắt buộc bổ sung:

- external physical borrowing;
- inter-library loan;
- physical book commerce;
- paid external digital product UX;
- multi-institution / multi-tenant support;
- generic entitlement engine;
- waitlist/hold queue khi resource hết copy;
- fine/payment do overdue;
- advanced reporting/statistics;
- social login;
- production-grade object storage mới;
- email/Zalo reminders;
- AI recommendation;
- metadata extraction bằng AI;
- summarization;
- plagiarism detection.

Các capability này có thể trở thành candidate của V3+.

---

# 6. Những quyết định để domain/design layer xử lý

Requirements không chốt representation cụ thể cho các điểm sau:

- institutional reader được persist bằng field, enum hay structure khác;
- `READY_FOR_PICKUP` có tiếp tục giữ trong implementation cuối V2 hay được simplify;
- exact set của BorrowRequest audit fields;
- digital access classification được persist thế nào;
- paid subscription naming/model cuối cùng;
- physical circulation policy có cần entity riêng hay config hiện tại đã đủ;
- Resource lifecycle có cần `ARCHIVED`/soft-delete;
- migration tool cụ thể;
- digital storage provider cụ thể.

Các quyết định này thuộc `domain.md`, `database.md`, `architecture.md` hoặc ADR khi cần.

---

# 7. Nguyên tắc duy trì requirements

- FR mô tả behavior observable.
- Business Rule mô tả policy/invariant.
- NFR mô tả quality requirement.
- Không biến implementation detail thành requirement nếu product không phụ thuộc vào detail đó.
- Không biến future possibility thành V2 requirement.
- V1 behavior chỉ được preserve khi salvage review xác định nó còn giá trị.
- Requirements được phép thay đổi khi implementation hoặc product learning tạo evidence mới.