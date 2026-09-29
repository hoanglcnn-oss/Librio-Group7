# Librio

## 1. Tầm nhìn sản phẩm

Librio là một hệ thống thư viện học thuật dành cho môi trường đại học, hỗ trợ việc khám phá và truy cập cả tài nguyên vật lý lẫn tài nguyên số.

Librio ban đầu được phát triển dưới dạng Mock Project với phạm vi cố tình được giữ nhỏ. Trọng tâm của phiên bản đầu là một luồng sử dụng hoàn chỉnh:

**Khám phá → Xem chi tiết → Kiểm tra khả dụng → Mượn bản vật lý / Truy cập bản số**

Sau khi Mock Project kết thúc, Librio được tiếp tục như một dự án cá nhân.

Mục tiêu trước mắt không phải bổ sung ngay toàn bộ các tính năng có thể có của một hệ thống thư viện, mà là chuyển implementation hiện tại thành một nền tảng mà người phát triển có thể thực sự hiểu, chịu trách nhiệm và tiếp tục mở rộng.

---

## 2. Vì sao Librio tồn tại

Một tài nguyên thư viện không chỉ là một bản ghi trong danh mục.

Người đọc cần có khả năng:

- tìm và khám phá tài nguyên;
- hiểu tài nguyên đó là gì;
- biết tài nguyên tồn tại dưới hình thức nào;
- biết tài nguyên hiện có thể được sử dụng hay không;
- mượn bản vật lý hoặc truy cập bản số.

Ở phía thư viện, hệ thống cần duy trì thông tin và trạng thái của các tài nguyên này một cách nhất quán.

Librio tập trung vào việc kết nối quá trình từ **khám phá tài nguyên** đến **thực sự sử dụng được tài nguyên**, thay vì coi tìm kiếm, availability, borrowing và digital access là những chức năng không liên quan tới nhau.

---

## 3. Bối cảnh sản phẩm

Librio hiện tập trung vào:

**Thư viện học thuật / thư viện đại học**

Việc giữ một bối cảnh cụ thể giúp các quyết định về:

- loại tài nguyên;
- cách mượn/trả;
- quyền truy cập;
- vai trò người dùng;
- quản trị thư viện;

có cơ sở rõ ràng hơn.

Librio hiện không cố trở thành một giải pháp chung cho mọi loại thư viện.

---

## 4. Người dùng chính

### 4.1 Người đọc / Sinh viên

Người đọc là người sử dụng chính của hệ thống.

Các nhu cầu chính gồm:

- tìm kiếm và khám phá tài nguyên;
- xem thông tin chi tiết;
- xem availability và hình thức truy cập;
- mượn và trả tài nguyên vật lý;
- truy cập tài nguyên số;
- theo dõi các hoạt động liên quan tới tài khoản của mình.

### 4.2 Thủ thư

Thủ thư chịu trách nhiệm duy trì dữ liệu và hỗ trợ hoạt động thư viện.

Trong phạm vi hiện tại, các chức năng quản trị chỉ cần đủ để hỗ trợ core workflow.

Các vai trò khác chỉ nên được thêm khi xuất hiện nhu cầu thực tế.

---

## 5. Luồng sản phẩm cốt lõi

Luồng chính của Librio là:

**Khám phá → Hiểu tài nguyên → Kiểm tra khả năng truy cập → Sử dụng tài nguyên**

Có thể hình dung:

```text
Nhu cầu thông tin
       ↓
Tìm kiếm / Duyệt
       ↓
Chi tiết tài nguyên
       ↓
Availability / Hình thức truy cập
       ↓
 ┌───────────────┬───────────────┐
 │ Bản vật lý    │ Bản số        │
 │               │               │
 │ Mượn / Trả    │ Truy cập      │
 └───────────────┴───────────────┘
```

Các chức năng khác nên hỗ trợ hoặc mở rộng luồng này, thay vì trở thành các feature rời rạc không có liên hệ với product core.

---

## 6. Tiến trình phát triển sản phẩm

### 6.1 V1 — Mock Project

V1 là phiên bản được xây dựng trong bối cảnh Mock Project.

Mục tiêu chính khi đó là hoàn thành một MVP có phạm vi đủ nhỏ để nhóm có thể triển khai đầy đủ.

V1 tập trung vào:

- tìm kiếm và duyệt tài nguyên;
- xem chi tiết tài nguyên;
- hiển thị availability và access type;
- mượn tài nguyên vật lý;
- due date, overdue và trả tài nguyên;
- basic digital access;
- các chức năng tài khoản cần thiết;
- quản trị thư viện ở mức tối thiểu.

V1 đồng thời chứa những artifact, cách tổ chức và implementation chịu ảnh hưởng bởi:

- yêu cầu học vụ;
- cách chia sprint của Mock Project;
- deadline;
- việc phát triển theo nhóm;
- các quyết định được đưa ra để kịp hoàn thành project.

Những yếu tố này không mặc định phải được giữ lại sau khi Mock Project kết thúc.

---

### 6.2 V2 — Baseline cá nhân

V2 không phải là một feature expansion lớn.

Mục tiêu của V2 là:

> **Chuyển implementation của Mock Project thành một baseline cá nhân có thể tiếp tục phát triển: giữ lại những hành vi sản phẩm và hiểu biết domain còn đúng, đồng thời loại bỏ các ràng buộc học vụ và technical debt không cần thiết.**

V2 chủ yếu là một giai đoạn:

- rà soát;
- refactor;
- rebuild có chọn lọc;
- chỉnh lại documentation;
- làm rõ các quyết định đang được hệ thống sử dụng.

Các chức năng hữu ích đang tồn tại trong V1 nên tiếp tục hoạt động trong V2, trừ khi có quyết định rõ ràng rằng chúng không còn phù hợp.

---

## 7. Phạm vi hiện tại của sản phẩm

V2 lấy behavior hữu ích của V1 làm baseline.

Phạm vi hiện tại bao gồm:

- resource discovery;
- resource detail;
- availability;
- physical borrowing;
- due date và overdue;
- return;
- basic digital access;
- các chức năng user cần thiết cho core workflow;
- minimal librarian administration.

Danh sách này mô tả **behavior hiện tại cần được bảo toàn trong quá trình repair**, không phải danh sách toàn bộ những gì Librio cuối cùng phải có.

---

## 8. Cách xử lý implementation của V1 trong V2

Không mặc định giữ toàn bộ code cũ.

Mỗi phần của V1 được đánh giá theo ba hướng.

### 8.1 Giữ nguyên để tái sử dụng

Giữ implementation khi:

- trách nhiệm của code rõ ràng;
- behavior vẫn phù hợp;
- design không tạo ra coupling không cần thiết;
- code đủ rõ để người maintain hiện tại sẵn sàng chịu trách nhiệm.

### 8.2 Giữ quyết định, làm lại implementation

Giữ business behavior hoặc domain decision nhưng rebuild code khi:

- ý tưởng cũ vẫn đúng;
- implementation có technical debt đáng kể;
- design bị ảnh hưởng quá nhiều bởi constraint của Mock Project;
- rebuild giúp responsibility và boundary rõ ràng hơn.

Đây dự kiến sẽ là trường hợp phổ biến nhất khi chuyển từ V1 sang V2.

### 8.3 Loại bỏ

Có thể bỏ những phần chỉ tồn tại vì:

- yêu cầu nộp bài;
- quy trình học vụ;
- giải pháp tạm thời vì deadline;
- assumption không còn đúng;
- complexity không còn mang lại giá trị.

Nguyên tắc chung:

> **Giữ lại quyết định có giá trị, không giữ artifact chỉ vì nó đã tồn tại.**

---

## 9. Điều kiện để coi V2 hoàn thành

V2 được coi là hoàn thành khi:

- core workflow hữu ích của V1 vẫn hoạt động end-to-end;
- product requirement hiện tại được mô tả rõ;
- domain concept và business rule quan trọng được ghi lại;
- database design phản ánh model mà project thực sự muốn duy trì;
- architecture đủ rõ để giải thích responsibility và dependency giữa các phần;
- technical debt quan trọng đã được sửa hoặc được chấp nhận có chủ đích;
- các luồng quan trọng có test phù hợp;
- application có thể được cấu hình và deploy theo cách có thể tái tạo;
- code hiện tại là code mà người phát triển sẵn sàng tiếp tục maintain;
- việc thêm một capability mới không bắt buộc phải tiếp tục phụ thuộc vào constraint của Mock Project.

V2 không cần dự đoán hoặc chuẩn bị implementation cho mọi feature tương lai.

---

## 10. Định hướng phát triển dài hạn

Đề tài Library Management ban đầu vẫn được sử dụng như một **product horizon**, không phải backlog bắt buộc của V2.

Trong các phiên bản sau, Librio có thể dần mở rộng sang:

- quản lý catalog đầy đủ hơn;
- author và category;
- quản lý tài nguyên số tốt hơn;
- PDF / ebook / audio / video;
- moderation;
- statistics về lượt đọc và lượt mượn;
- activity/history;
- reservation hoặc các circulation extension khác;
- các chức năng quản trị thư viện rộng hơn.

Thứ tự triển khai chưa được cố định.

Mỗi capability mới sẽ được phân tích khi nó thực sự trở thành candidate cho iteration tiếp theo.

---

### 10.1 Tích hợp bên thứ ba

Các hướng có thể thử trong tương lai:

- đăng nhập Google hoặc social login;
- ISBN / Google Books API;
- Email hoặc các kênh nhắc hạn trả;
- Google Drive;
- AWS S3 hoặc object storage tương đương;
- viewer hoặc media service phù hợp.

Nguyên tắc ưu tiên:

> **Tái sử dụng các capability phổ thông trước; chỉ tự xây hoặc thay thế khi cần nhiều control hơn, gặp giới hạn scale hoặc nó trở thành điểm khác biệt của sản phẩm.**

---

### 10.2 AI

Các hướng AI có thể được thử gồm:

- nhận diện metadata từ ảnh bìa hoặc PDF;
- recommendation dựa trên sở thích và lịch sử;
- summarization;
- plagiarism detection.

AI không phải điều kiện để Librio trở thành một sản phẩm hoàn chỉnh.

Không có AI capability nào mặc định thuộc scope V2.

Các chức năng này chỉ được đưa vào implementation khi có use case đủ rõ và có cách đánh giá kết quả phù hợp.

---

## 11. Nguyên tắc phát triển sản phẩm

Librio giữ một số nguyên tắc:

- Reader journey vẫn là trục chính của sản phẩm.
- Feature mới phải hỗ trợ một product hoặc engineering need cụ thể.
- Không thêm technical complexity chỉ để thể hiện công nghệ.
- Không thiết kế trước những abstraction chưa có use case thực tế.
- Có thể reuse giải pháp có sẵn khi chúng không phải điểm khác biệt của sản phẩm.
- Documentation và architecture có thể thay đổi khi implementation cung cấp thêm thông tin.
- Những quyết định lớn nên giữ lại lý do, không chỉ giữ kết quả cuối cùng.

Product direction có thể thay đổi theo thời gian, nhưng thay đổi nên xuất phát từ điều đã học được qua quá trình phát triển thay vì từ việc thêm feature một cách ngẫu nhiên.