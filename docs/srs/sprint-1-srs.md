# D03 — Software Requirements Specification (SRS) — Sprint 1 Baseline

**Project:** Librio  
**Sprint:** S1 — 13/08–19/08/2026  
**Status:** Baseline (Đã đóng băng ngày 17/8)  
**Owners:** KL — Requirement/QA, HH — Backend, PL — Frontend/Design  

---

## 1. Purpose

Tài liệu này xác định các yêu cầu phần mềm (Software Requirements Specification - SRS) cho Sprint 1 của dự án Librio.

Sprint 1 tập trung vào workflow cốt lõi dành cho người đọc (Reader Core Workflow):

```text
Search / Browse ──► Resource Detail ──► Availability
```

Mục tiêu của Sprint 1 cho phép người đọc:
1. Duyệt (Browse) hoặc Tìm kiếm (Search) tài liệu trong thư viện.
2. Xem thông tin chi tiết (Detail) và metadata của tài liệu.
3. Kiểm tra tình trạng khả dụng (Availability) thực tế (Physical và Digital).

---

## 2. Scope

### 2.1 In Scope (Sprint 1)
- **LIB-01 — Browse Resources:** Duyệt danh sách tài liệu.
- **LIB-02 — Search Resources:** Tìm kiếm tài liệu theo từ khóa.
- **LIB-03 — Resource Detail:** Xem thông tin chi tiết & loại truy cập.
- **LIB-04 — Availability:** Trạng thái khả dụng thực tế của tài liệu.
- **LIB-05 — Resource/Item Data Model:** Model dữ liệu nền tảng.
- **LIB-06 — API Foundation:** REST API phục vụ Frontend.

### 2.2 Out of Scope (Sprint 1)
- Physical Borrowing & Return execution.
- Online Reading / PDF Stream viewer / Download.
- Library Administration / Mod / Reviewer workflows.
- Notifications & Reminders (Email / Zalo).
- AI features (Cover scanner, Chatbot, Summarizer).
- Third-party integrations (OAuth2, S3, ISBN lookup).
- Reservation / Hold system.

---

## 3. Detailed Functional Requirements

### 3.1 LIB-01 — Browse Resources
- **Requirement:** Hệ thống phải cho phép người đọc duyệt danh sách các tài liệu trong thư viện.
- **User Value:** Khám phá tài liệu mà không cần biết chính xác tên tài liệu trước.
- **Acceptance Criteria:**
  - [x] Hiển thị danh sách các Resources.
  - [x] Hiển thị thông tin tóm tắt (title, authors) trên từng Resource Card.
  - [x] Cho phép click chọn 1 resource để mở trang Resource Detail (`/resources/:id`).
  - [x] Hiển thị trạng thái Loading / Error khi tải dữ liệu.

### 3.2 LIB-02 — Search Resources
- **Requirement:** Hệ thống phải cho phép người đọc tìm kiếm tài liệu bằng từ khóa.
- **User Value:** Tìm nhanh tài liệu đúng nhu cầu mà không cần lướt toàn bộ danh sách.
- **Acceptance Criteria:**
  - [x] Có ô nhập từ khóa tìm kiếm (`SearchInput`).
  - [x] Khi submit, tự động trim khoảng trắng thừa và gửi request tìm kiếm.
  - [x] Tự động cập nhật keyword lên URL query params (`?q=...`).
  - [x] Nếu không tìm thấy kết quả, hiển thị màn hình **Empty State** (*"Không tìm thấy tài liệu phù hợp"*).

### 3.3 LIB-03 — Resource Detail
- **Requirement:** Hiển thị thông tin chi tiết và metadata cơ bản của resource được chọn.
- **Acceptance Criteria:**
  - [x] Hiển thị Title, Authors, Description chi tiết.
  - [x] Hiển thị loại hình truy cập: Physical-only, Digital-only, hoặc cả hai (`accessTypes`).
  - [x] Tích hợp phần hiển thị Availability trực tiếp trên trang Detail.
  - [x] Nếu ID không tồn tại trong DB ➔ Trả lỗi 404 và hiển thị màn hình **Not Found**.

### 3.4 LIB-04 — Availability
- **Requirement:** Hiển thị tình trạng khả dụng thực tế dựa trên dữ liệu phía Server.
- **Acceptance Criteria:**
  - [x] Đối với Physical: Hiển thị tổng số bản sao và số bản khả dụng (`availableCopies / totalCopies`).
  - [x] Đối với Digital: Hiển thị badge `Available` nếu record bản số tồn tại.
  - [x] Nếu tài liệu chỉ có 1 trong 2 loại (Physical/Digital), chỉ hiển thị khối tương ứng (Omit khối còn lại).
  - [x] Client lấy dữ liệu trực tiếp từ Server response, không tự tính toán lại.

---

## 4. Technical Enabling Requirements

### 4.1 LIB-05 — Resource/Item Data Model
- Thiết lập quan hệ `1 Resource ── 0..N PhysicalItem` và `1 Resource ── 0..1 DigitalItem`.
- `PhysicalItem` chứa enum trạng thái (`AVAILABLE`, `BORROWED`, `OVERDUE`).

### 4.2 LIB-06 — API Foundation
- Cung cấp 2 REST API endpoints chuẩn hóa:
  - `GET /resources?q={keyword}`
  - `GET /resources/{id}`

---

## 5. Traceability Matrix

```text
    LIB-01 (Browse) ──┐
                      ├──► LIB-03 (Resource Detail) ──► LIB-04 (Availability)
    LIB-02 (Search) ──┘
                             ▲
                             │ (Supported by)
                     LIB-05 & LIB-06 (Data Model & API Foundation)
```
