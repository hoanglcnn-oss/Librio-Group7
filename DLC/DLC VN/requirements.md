# Librio — Yêu cầu hệ thống

## 1. Mục đích

Tài liệu này mô tả các yêu cầu mà Librio V2 phải bảo toàn hoặc làm rõ trong quá trình chuyển từ Mock Project sang baseline cá nhân.

V2 chủ yếu là giai đoạn repair/refactor, không phải feature expansion lớn.

Các yêu cầu được chia thành:

- **Yêu cầu chức năng (Functional Requirements):** actor hoặc system có thể làm gì.
- **Business Rules:** các luật mà behavior của hệ thống phải tuân theo.
- **Yêu cầu phi chức năng (Non-functional Requirements):** các thuộc tính chất lượng hoặc “passive behavior” mà hệ thống phải duy trì.
- **Ngoài phạm vi V2:** các capability chưa phải mục tiêu của baseline repair.

### Trạng thái

- **[GIỮ]** — behavior V1 đã xác định rõ, V2 phải preserve.
- **[LÀM RÕ]** — intent đã có nhưng specification hiện chưa đủ chính xác.
- **[V2 REPAIR]** — requirement được thêm vì mục tiêu của V2 là tạo một baseline maintainable.
- **[NGOÀI V2]** — không phải requirement của V2.

---

# 2. Yêu cầu chức năng

## 2.1 Khám phá tài nguyên

### FR-DIS-01 — Duyệt tài nguyên
**[GIỮ]**

Người đọc phải có khả năng duyệt danh sách tài nguyên trong thư viện.

**Kết quả mong đợi**
- Hiển thị danh sách resource.
- Mỗi resource hiển thị đủ thông tin tóm tắt để người đọc quyết định có mở chi tiết hay không.

---

### FR-DIS-02 — Tìm kiếm tài nguyên
**[GIỮ / LÀM RÕ]**

Người đọc phải có khả năng tìm kiếm tài nguyên.

V2 phải bảo toàn các search behavior hữu ích đang tồn tại trong V1.

**Cần làm rõ từ V1**
- searchable fields;
- filter;
- sort;
- pagination;
- exact search behavior hiện tại.

---

### FR-DIS-03 — Xem chi tiết tài nguyên
**[GIỮ]**

Người đọc phải có khả năng mở một resource và xem thông tin chi tiết cần thiết để:

- hiểu resource đó là gì;
- biết resource có physical/digital access hay không;
- biết cách tiếp tục sử dụng resource.

---

### FR-DIS-04 — Xem hình thức truy cập
**[GIỮ]**

Người đọc phải có khả năng nhận biết resource có thể được truy cập dưới dạng:

- physical;
- digital;
- hoặc cả hai.

---

### FR-DIS-05 — Xem availability
**[GIỮ]**

Người đọc phải có khả năng biết resource hoặc item liên quan hiện có khả dụng hay không.

Nếu resource có nhiều item, hệ thống phải trình bày availability ở mức đủ dễ hiểu cho người đọc.

---

## 2.2 Physical Circulation

### FR-CIR-01 — Mượn physical item
**[GIỮ]**

Người đọc phải có khả năng gửi yêu cầu mượn một physical item.

Server phải quyết định request đó có hợp lệ hay không.

**Kết quả mong đợi**
- Nếu item đủ điều kiện: borrowing được tạo.
- Nếu item không đủ điều kiện: operation bị từ chối với lỗi phù hợp.

---

### FR-CIR-02 — Gán hạn trả
**[GIỮ]**

Khi một physical borrowing được tạo thành công, hệ thống phải gán thông tin đủ để xác định due date.

---

### FR-CIR-03 — Xác định borrowing quá hạn
**[GIỮ / LÀM RÕ]**

Hệ thống phải có khả năng xác định một active borrowing đã quá hạn hay chưa.

Cách biểu diễn trạng thái overdue sẽ được quyết định ở `domain.md` và `database.md`.

---

### FR-CIR-04 — Trả physical item
**[GIỮ]**

Hệ thống phải hỗ trợ việc trả một physical item đã được mượn.

**Kết quả mong đợi**
- borrowing được kết thúc hợp lệ;
- circulation state được cập nhật;
- item có thể trở lại trạng thái borrowable nếu không có rule khác ngăn cản.

---

### FR-CIR-05 — Xem trạng thái borrowing
**[GIỮ / LÀM RÕ]**

Reader hoặc librarian phải có khả năng xem đủ thông tin để hiểu trạng thái hiện tại của một borrowing liên quan.

Các field cụ thể cần inventory từ V1.

---

## 2.3 Digital Access

### FR-DIG-01 — Nhận biết digital access
**[GIỮ]**

Nếu một resource có digital form, người đọc phải có khả năng nhận biết digital access đang tồn tại.

---

### FR-DIG-02 — Truy cập digital resource
**[GIỮ / LÀM RÕ]**

Người đọc phải có khả năng truy cập digital resource theo behavior mà V1 hiện hỗ trợ.

**Cần làm rõ từ V1**
- access bằng link, viewer hay mechanism khác;
- có download hay không;
- permission/access control hiện tại;
- loại digital content thực sự đang được hỗ trợ.

---

## 2.4 User Account

### FR-USR-01 — Đăng nhập và nhận diện người dùng
**[GIỮ / LÀM RÕ]**

Hệ thống phải có khả năng nhận diện người dùng để các operation liên quan tới account và circulation hoạt động đúng.

Chi tiết registration/login/profile cần inventory từ V1.

---

### FR-USR-02 — Liên kết borrowing với reader
**[GIỮ]**

Mỗi borrowing phải được liên kết với reader tương ứng.

---

### FR-USR-03 — Xem thông tin liên quan tới account
**[GIỮ / LÀM RÕ]**

Người dùng phải có khả năng xem các thông tin account-related mà V1 hiện hỗ trợ và còn cần thiết cho core workflow.

Phạm vi cụ thể sẽ được xác minh trong salvage pass.

---

## 2.5 Library Administration

### FR-ADM-01 — Quản lý resource data
**[GIỮ]**

Librarian phải có khả năng tạo hoặc duy trì resource data ở mức cần thiết cho core workflow.

---

### FR-ADM-02 — Quản lý item
**[GIỮ]**

Librarian phải có khả năng duy trì physical/digital item cần thiết để resource có thể được truy cập đúng.

---

### FR-ADM-03 — Hỗ trợ circulation
**[GIỮ]**

Librarian phải có khả năng thực hiện hoặc hỗ trợ các operation circulation mà V1 hiện cung cấp.

Chi tiết operation cụ thể cần inventory từ implementation.

---

# 3. Business Rules

## BR-01 — Resource và access form là các concept khác nhau
**[GIỮ]**

Một `Resource` đại diện cho tài nguyên logical/bibliographic.

Physical và digital là các hình thức truy cập có lifecycle riêng.

**Ví dụ**

```text
Resource: Clean Code

├── Physical Item #A
├── Physical Item #B
└── Digital PDF
```

`Clean Code` là một resource.

Hai bản sách vật lý và file PDF không phải ba resource độc lập.

---

## BR-02 — Một physical item chỉ có tối đa một active borrowing
**[GIỮ]**

Một physical item không được đồng thời thuộc nhiều active borrowing.

**Ví dụ**

```text
Physical Item #12
→ Hoàng đang mượn
```

Linh gửi request mượn `Item #12`.

Kết quả hợp lệ:
- request của Linh bị từ chối.

Kết quả không hợp lệ:
- Hoàng và Linh đều có active borrowing cho cùng `Item #12`.

---

## BR-03 — Physical item có thể có nhiều borrowing trong lịch sử
**[GIỮ]**

BR-02 chỉ giới hạn borrowing đang active cùng lúc.

**Ví dụ**

```text
Item #12
→ Hoàng mượn → trả
→ Linh mượn  → trả
→ Nam mượn   → đang active
```

Ba borrowing record là hợp lệ vì chỉ có borrowing của Nam đang active.

---

## BR-04 — Physical borrowing phải có due date
**[GIỮ]**

Mỗi physical borrowing phải có thông tin đủ để biết hạn trả.

**Ví dụ**

```text
Borrowed at: 2026-09-01
Due at:      2026-09-15
```

Nếu thiếu `dueAt`, hệ thống không thể xác định overdue một cách đáng tin cậy.

---

## BR-05 — Overdue chỉ áp dụng cho borrowing còn active
**[LÀM RÕ]**

Một borrowing được coi là overdue khi:

- borrowing vẫn active;
- và `dueAt` đã qua thời điểm hiện tại.

**Ví dụ**

```text
dueAt = 2026-09-15
today = 2026-09-20
returnedAt = null
```

→ overdue.

Nếu:

```text
returnedAt = 2026-09-18
```

→ không còn là active overdue.

Cách lưu hoặc derive trạng thái này chưa được chốt ở requirements layer.

---

## BR-06 — Borrow/return được quyết định ở server
**[GIỮ]**

Client không được tự quyết định operation đã thành công.

**Ví dụ**

Frontend bấm `Borrow`.

Sai:

```text
set item = BORROWED
→ sau đó mới gọi API
```

Đúng:

```text
gọi API
→ server validate
→ server trả success
→ frontend cập nhật UI
```

---

## BR-07 — Availability phải phản ánh trạng thái hệ thống
**[GIỮ]**

Availability không được client tự suy đoán.

**Ví dụ**

Nếu DB cho biết cả hai physical copy đều đang được mượn:

```text
Copy A = unavailable
Copy B = unavailable
```

frontend không được hiển thị:

```text
Physical: Available
```

chỉ vì state cũ trên UI chưa refresh.

---

## BR-08 — Availability có thể aggregate nhiều item
**[GIỮ]**

Một resource có thể có nhiều item nhưng reader cần một availability summary dễ hiểu.

**Ví dụ**

```text
Resource: Clean Code

Physical Copy A → Borrowed
Physical Copy B → Available
Digital PDF     → Available
```

Có thể hiển thị:

```text
Physical: 1 copy available
Digital: Available
```

---

## BR-09 — Digital access trong baseline không có expiry
**[GIỮ]**

Digital access của baseline V1/V2 không tự hết hạn theo borrowing period.

**Ví dụ**

Nếu user có quyền truy cập một PDF theo baseline hiện tại:

```text
Monday  → accessible
Friday  → vẫn accessible
```

Không tồn tại rule kiểu:

```text
access expires after 7 days
```

trong V2.

---

## BR-10 — Overdue không kéo theo fine/payment
**[GIỮ]**

Một borrowing overdue không tự tạo payment, fine hoặc billing obligation trong V2.

**Ví dụ**

```text
Borrowing = overdue
```

Có thể dẫn tới warning/status change.

Không mặc định dẫn tới:

```text
Fine = 50,000 VND
```

---

## BR-11 — Reservation không thuộc baseline
**[GIỮ]**

Nếu item đang được mượn, V2 không bắt buộc phải hỗ trợ queue/reservation.

**Ví dụ**

```text
Item #12 = borrowed
```

Reader khác có thể chỉ thấy unavailable.

Không bắt buộc phải có:

```text
Reserve this item
```

---

# 4. Yêu cầu phi chức năng

NFR mô tả các thuộc tính mà hệ thống phải duy trì trong khi thực hiện các chức năng.

Có thể hiểu như “passive” của hệ thống: người dùng không gọi trực tiếp nhưng chúng ảnh hưởng tới độ đúng, an toàn và maintainability.

---

## 4.1 Data Integrity & Consistency

### NFR-CON-01 — Giữ circulation invariant
**[GIỮ]**

Borrow/return operation không được làm hỏng circulation invariant.

**Ví dụ**

Hai request borrow `Item #12` đến gần như cùng lúc.

Kết quả hợp lệ:

```text
Request A → success
Request B → rejected
```

Kết quả không hợp lệ:

```text
Request A → success
Request B → success
→ 2 active borrowings
```

---

### NFR-CON-02 — Tránh partial update
**[GIỮ / LÀM RÕ]**

Nếu một business operation cần nhiều thay đổi dữ liệu liên quan, hệ thống phải tránh trạng thái chỉ update được một phần.

**Ví dụ**

Borrow flow cần:

```text
create Borrowing
+
update circulation state
```

Nếu bước sau fail thì hệ thống không được để lại state kiểu:

```text
Borrowing created
but Item still Available
```

---

## 4.2 Security

### NFR-SEC-01 — Authorization phải được enforce ở server
**[LÀM RÕ]**

Operation yêu cầu role hoặc ownership không được dựa chỉ vào việc frontend ẩn button.

**Ví dụ**

Frontend không hiện nút Admin cho reader.

Điều đó chưa đủ.

Reader vẫn không được phép gọi trực tiếp:

```http
DELETE /admin/resources/12
```

và thành công.

---

### NFR-SEC-02 — Secret không được hard-code
**[V2 REPAIR]**

Credential, token và production secret phải nằm ngoài source code.

**Ví dụ**

Không hợp lệ:

```java
String password = "postgres123";
```

Phù hợp hơn:

```text
DB_PASSWORD=<environment variable>
```

---

## 4.3 Maintainability

### NFR-MNT-01 — Responsibility phải đủ rõ
**[V2 REPAIR]**

Module/layer phải có responsibility đủ rõ để maintainer hiểu được flow chính.

**Ví dụ**

Một business rule về borrowing không nên nằm rải rác ở:

```text
Controller
React component
Repository
SQL script
```

mà không có một nơi chịu responsibility rõ ràng.

---

### NFR-MNT-02 — Không giữ academic structure chỉ vì đã tồn tại
**[V2 REPAIR]**

Code và documentation không cần preserve structure chỉ vì Mock Project từng yêu cầu.

**Ví dụ**

Nếu một WBS spreadsheet không còn giúp development:

```text
→ không cần duy trì chỉ để project "trông đầy đủ"
```

---

### NFR-MNT-03 — Không over-engineer cho future chưa tồn tại
**[V2 REPAIR]**

V2 không nên tạo abstraction chỉ để chuẩn bị cho use case chưa có.

**Ví dụ**

Nếu hiện tại chỉ có một search implementation:

```text
SearchService
```

không cần ngay lập tức tạo:

```text
SearchProviderFactory
SearchProviderStrategy
SearchProviderAdapter
SearchProviderRegistry
```

chỉ vì tương lai “có thể dùng Elasticsearch”.

---

## 4.4 Testability

### NFR-TST-01 — Core business rule phải kiểm thử tự động được
**[V2 REPAIR]**

Các rule có khả năng phá core behavior phải có automated tests phù hợp.

**Ví dụ**

Cần test ít nhất behavior:

```text
given Item #12 already borrowed
when another borrow is attempted
then operation must fail
```

Mục tiêu là bảo vệ behavior, không chỉ tăng coverage percentage.

---

## 4.5 Configuration & Deployment

### NFR-DEP-01 — Configuration phải tách khỏi source
**[V2 REPAIR]**

Environment-specific config không nên yêu cầu sửa code bằng tay.

**Ví dụ**

Cùng một build có thể dùng:

```text
local DB
staging DB
production DB
```

bằng configuration khác nhau.

---

### NFR-DEP-02 — Deployment phải có thể tái tạo
**[V2 REPAIR]**

V2 phải có một deployment path mà future maintainer có thể thực hiện lại từ documentation/configuration hiện có.

**Ví dụ**

Không nên phụ thuộc vào:

```text
"máy t chạy được vì trước đây t click một setting nào đó mà giờ không nhớ"
```

---

## 4.6 Observability & Error Handling

### NFR-OBS-01 — Lỗi quan trọng phải chẩn đoán được
**[V2 REPAIR]**

Backend phải cung cấp đủ error/log context để chẩn đoán lỗi quan trọng.

**Ví dụ**

Không tốt:

```text
500 Internal Server Error
```

và server không có thông tin gì thêm.

Tốt hơn:

```text
request failed
operation = borrow
itemId = 12
exception = ...
```

mà không log secret/sensitive information.

---

### NFR-OBS-02 — Client nhận error response nhất quán
**[V2 REPAIR / LÀM RÕ]**

Các error phổ biến nên có response format đủ ổn định để frontend xử lý.

**Ví dụ**

Thay vì từng endpoint trả:

```text
"error"
"failed"
null
HTML error page
```

nên có một error contract thống nhất ở mức cần thiết.

Chi tiết thuộc API/design phase.

---

# 5. Phạm vi V2

## 5.1 V2 phải bảo toàn

Baseline gồm:

- resource discovery;
- resource detail;
- availability;
- physical borrowing;
- due date / overdue;
- return;
- basic digital access;
- user behavior cần cho core workflow;
- minimal librarian administration.

---

## 5.2 Không yêu cầu mở rộng trong V2

V2 không có nghĩa vụ bổ sung:

- reservation system;
- fine/payment;
- digital expiry/licensing;
- advanced librarian administration;
- advanced statistics/reporting;
- social login;
- Google Books / ISBN integration;
- cloud storage integration mới;
- AI recommendation;
- metadata extraction;
- summarization;
- plagiarism detection;
- IoT/hardware integration.

Các capability này có thể trở thành requirement của version sau qua một vòng Analyze → Design mới.

---

# 6. Những điểm phải làm rõ từ V1

## 6.1 Search
- searchable fields hiện tại;
- filter/sort/pagination hiện có;
- behavior nào thực sự đang được frontend sử dụng.

## 6.2 User / Security
- registration;
- login;
- profile;
- role;
- authorization;
- ownership behavior.

## 6.3 Borrowing / Overdue
- active borrowing được nhận diện thế nào;
- overdue persisted hay derived;
- return state được biểu diễn ra sao.

## 6.4 Availability
- item-level availability được lưu hay derive;
- resource-level availability được aggregate ở đâu;
- frontend đang phụ thuộc vào field nào.

## 6.5 Digital Access
- loại digital item hiện có;
- access mechanism;
- download/viewer;
- permission hiện tại.

## 6.6 Librarian Administration
- operation nào tồn tại thật trong source;
- operation nào chỉ tồn tại trong proposal/docs;
- operation nào frontend thực sự expose.

---

# 7. Nguyên tắc khi duy trì requirements

- FR chỉ mô tả behavior mà actor/system có thể thực hiện hoặc quan sát.
- Scope exclusion không viết thành FR.
- Business policy không phải user action nên đặt ở Business Rules.
- Business Rule nên có ví dụ đủ cụ thể để đọc độc lập vẫn hiểu.
- NFR nên được nhóm theo quality category.
- NFR nên có ví dụ cho thấy requirement ảnh hưởng runtime/design như thế nào.
- Requirement chưa được source hiện tại chứng minh phải được đánh dấu **[LÀM RÕ]**, không tự giả định.
- Requirements được phép thay đổi khi quá trình salvage hoặc implementation cung cấp evidence mới.