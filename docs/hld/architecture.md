# Librio — High-Level Design & System Architecture

**Project:** Librio | **Version:** Sprint 2 (HLD v0.2)  
**SRS:** [Sprint 2 SRS](../srs/sprint-2-srs.md) | **Component Diagram:** [component-diagram.mmd](component-diagram.mmd)

---

## 1. System Architecture & Tech Stack

Librio áp dụng kiến trúc **Modular Monolith** nhằm tối ưu hóa tốc độ phát triển cho MVP, đơn giản hóa vận hành và đảm bảo tính nhất quán của giao dịch (transaction) mà không gặp phức tạp của distributed systems.

```text
Browser ──► React SPA (Client) ──[ HTTP/JSON + Session Cookie ]──► Spring Security Boundary
                                                                        │
┌───────────────────────────────────────────────────────────────────────┘
▼
Spring Boot Modular Monolith
├── Account & Access Module (Identity, Authentication, Role Management)
├── Catalog Module          (Resources, Physical/Digital Items, Derived Availability)
└── Circulation Module      (Borrow Requests, Reservations, Checkouts, My Library)
        │
        ▼
PostgreSQL Database (Shared Relational Database)
```

| Layer | Technology | Key Details |
| :--- | :--- | :--- |
| **Frontend** | React SPA, Vite, React Router | CSR Architecture, Relative URL API calls (`credentials: 'include'`). |
| **Backend** | Java, Spring Boot, Spring Security | Single Deployment Artifact, Modular Layering (Controller ➔ Service ➔ Repository). |
| **Persistence**| Spring Data JPA, PostgreSQL | Single Relational DB với logical domain isolation. |
| **Security** | Session Cookie + CSRF Protection | `HttpOnly` cookies, Server-side HTTP session (no JWT). |

---

## 2. Module Boundaries & Responsibilities

### 2.1 Core Modules

* **React SPA (Client UI):** Gồm 4 phân vùng chính (`Discovery UI`, `Authentication UI`, `My Library UI`, `Librarian Circulation UI`). Client không phải là Source of Truth cho bất kỳ business rule, availability hay security authorization nào.
* **Spring Security Boundary:** Intercept toàn bộ HTTP request; chịu trách nhiệm Authentication, Session Restoration, Role-based Authorization, CSRF validation và Error Handling (Trả JSON 401/403).
* **Account & Access Module:** Sở hữu entity `Account`, Canonical Email, Password Hash (`BCrypt`) và Role (`READER`/`LIBRARIAN`). Trong Sprint 2, `Account` với role `READER` đóng vai trò trực tiếp là Reader Identity (chưa tách `ReaderProfile`).
* **Catalog Module:** Quản lý `Resource`, `PhysicalItem`, `DigitalItem`. Tính toán **Derived Availability** (Availability = Total Copies - Reserved Copies - Borrowed Copies). Physical item ở trạng thái `RESERVED` hoặc `BORROWED` bị loại khỏi availability.
* **Circulation Module:** Quản lý vòng đời `BorrowRequest` và `Borrowing`. Thực hiện allocation, prepare, reject, expire, checkout và reader cancellation.

### 2.2 Cross-Module Interactions & Layers

Mỗi module tuân thủ cấu trúc 3 lớp: `Controller (HTTP)` ➔ `Service (Business/Orchestration)` ➔ `Repository (Persistence)`.

| Source | Target | Interaction Purpose |
| :--- | :--- | :--- |
| Security Boundary | Account & Access | Authenticate credential & Load user SecurityContext. |
| Circulation Module | Account Context | Lấy `Account.id` immutable từ SecurityContext (Không tin `readerId` do Client gửi). |
| Circulation Module | Catalog Module | Phân bổ (allocate), khóa (reserve), và cập nhật trạng thái `PhysicalItem`. |
| All Backend Modules | PostgreSQL DB | Lưu trữ và truy vấn dữ liệu theo domain boundary. |

---

## 3. High-Level Workflows & Boundaries

### 3.1 Core Workflows
1. **Public Discovery:** Guest/Reader browse & search tài liệu ➔ Security cho phép public ➔ Catalog Module trả dữ liệu Resource & Derived Availability.
2. **Login & Session Restoration:** SPA gửi credentials + CSRF token ➔ Security xác thực ➔ Tạo Server Session & Set `HttpOnly` Cookie ➔ Trả `AccountSummaryResponse`. SPA gọi `/auth/me` để khôi phục state khi reload.
3. **Submit Borrow Request:** Reader tạo Yêu cầu ➔ Circulation xác thực identity ➔ Yêu cầu Catalog phân bổ 1 `AVAILABLE` physical item ➔ Tạo `BorrowRequest` & đổi item sang `RESERVED` trong 1 atomic transaction.
4. **Prepare / Reject Request:** Librarian xử lý request ➔ Đổi trạng thái sang `READY_FOR_PICKUP` (nếu có sách) hoặc `REJECTED` (giải phóng reserved item về `AVAILABLE`).
5. **Checkout & Fulfil:** Librarian xác nhận mượn ➔ Tạo `Borrowing` + Due Date ➔ Chuyển request sang `FULFILLED` & item sang `BORROWED` trong 1 atomic transaction.
6. **Reader Cancellation:** Reader hủy request active ➔ Request chuyển sang `CANCELLED` & giải phóng reserved item về `AVAILABLE`. Cạnh tranh giữa Cancel và Fulfil tuân theo one-winner behavior.

### 3.2 Security, Transaction & Concurrency Boundaries
* **Session & Privacy Boundary:** Principal từ Session là nguồn duy nhất xác định User Identity. Reader chỉ được truy cập dữ liệu do chính mình sở hữu. Dữ liệu của Reader khác nếu không thuộc quyền hạn sẽ trả về lỗi `404 Not Found` tương tự như record không tồn tại.
* **Transaction Boundary:** Thao tác tạo Request/Reserve, Checkout/Fulfil, Reject/Cancel bắt buộc phải bọc trong **Server-Side Atomic Transaction**. Nếu có lỗi, toàn bộ transaction rollback để tránh sai lệch state giữa Request, Borrowing và PhysicalItem.
* **Concurrency Boundary:** Hệ thống áp dụng locking/versioning strategy để giữ tính năng one-winner khi nhiều Reader cùng yêu cầu 1 bản sao vật lý cuối cùng hoặc khi Cancel cạnh tranh với Fulfil.

---

## 4. Architectural Decisions & Traceability

### 4.1 Architecture Decisions Log

| ID | Decision | Rationale / Status |
| :--- | :--- | :--- |
| `HLD-01` | React SPA + Spring Boot Modular Monolith + PostgreSQL | Tối ưu vận hành & tốc độ phát triển cho MVP. (Accepted) |
| `HLD-02` | REST / HTTP JSON (Không dùng `/api` prefix) | Thống nhất API conventions với Sprint 1. (Accepted) |
| `HLD-03` | Server-Side Session + `HttpOnly` Cookie (Không dùng JWT) | Bảo mật tốt chống XSS, đơn giản hóa revocation. (Accepted) |
| `HLD-04` | Spring Security Boundary | Tập trung hóa Security logic, CSRF & JSON Error Handling. (Accepted) |
| `HLD-05` | Account (`ROLE_READER`) = Reader Identity | Đơn giản hóa domain model Sprint 2, chưa tách `ReaderProfile`. (Accepted) |
| `HLD-06` | Derived Availability (No DB Table) | Availability được tính toán động từ Item status, tránh vỡ dữ liệu. (Accepted) |
| `HLD-07` | Atomic Transaction Boundaries | Bắt buộc cho mọi thao tác mượn/trả/hủy liên kết giữa Request & Item. (Accepted) |
| `HLD-08` | Single-Site Production / Vite Dev Proxy | Đơn giản hóa CORS và Cookie sharing. (Accepted) |

### 4.2 Traceability Matrix

| Architecture Area | Requirement Source | Detailed Design Document |
| :--- | :--- | :--- |
| Account & Authentication | T-071, Sprint 2 SRS | [sprint-2-auth-lld.md](../lld/sprint-2-auth-lld.md) |
| Borrow Request & Checkout | T-081, Sprint 2 SRS | `sprint-2-borrow-lld.md` (T-082) |
| My Library (Requests/Borrowings) | T-091, Sprint 2 SRS | `sprint-2-mylibrary-lld.md` (T-092/T-093) |
| Database Schema & Constraints | Sprint 2 SRS & Domain Boundaries | Database Design Specification (T-083) |
