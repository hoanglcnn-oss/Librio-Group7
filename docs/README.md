# Librio — Documentation Index & Architecture Hub

> Thư mục lưu trữ toàn bộ tài liệu Yêu cầu (SRS), Thiết kế Cơ sở Dữ liệu (Database Design), Thiết kế Kiến trúc (HLD) và Thiết kế Chi tiết (LLD) của dự án **Librio**.

---

## 📂 Danh mục Tài liệu & Diagram (Index Hub)

### 1. Software Requirements Specification (SRS)
- 📄 **[Sprint 1 SRS Baseline (`srs/sprint-1-srs.md`)](srs/sprint-1-srs.md)** *(LIB-01 đến LIB-06, User Stories, Acceptance Criteria)*
- 📄 **[Sprint 2 SRS Specification (`srs/sprint-2-srs.md`)](srs/sprint-2-srs.md)** *(US-07 đến US-09, Reader Identity, Auth & Physical Circulation)*

### 2. Database Design
- 📊 **[Source ERD Mermaid (`database/erd.mmd`)](database/erd.mmd)** *(ERD Diagram-as-Code)*  
- 📑 **[Schema Specification & Constraints (`database/schema-spec.md`)](database/schema-spec.md)** *(Tables, Columns, Data Types, Constraints)*

### 3. High-Level Design (HLD)
- 📄 **[Architecture Specification (`hld/architecture.md`)](hld/architecture.md)** *(Modular Monolith, Security & Transaction Boundaries)*  
- 📊 **[Component Diagram Source (`hld/component-diagram.mmd`)](hld/component-diagram.mmd)** *(Mermaid Component Diagram)*

### 4. Low-Level Design (LLD) & API Contracts
- 📘 **[Sprint 1 Unified LLD (`lld/sprint-1-lld.md`)](lld/sprint-1-lld.md)** *(Discovery & Availability LLD Baseline)*  
- 🔒 **[Sprint 2 Auth & Access LLD (`lld/sprint-2-auth-lld.md`)](lld/sprint-2-auth-lld.md)** *(Session, CSRF, Role Authorization & Account Model)*  
- 📚 **[Sprint 2 Physical Circulation LLD (`lld/sprint-2-borrow-lld.md`)](lld/sprint-2-borrow-lld.md)** *(Borrow Request Lifecycle, Checkouts & My Library)*  
- 🔌 **[Sprint 1 API Contract (`lld/api-contracts/sprint-1-api.md`)](lld/api-contracts/sprint-1-api.md)** *(Public Discovery REST API)*  
- 🔌 **[Sprint 2 API Contract (`lld/api-contracts/sprint-2-api.md`)](lld/api-contracts/sprint-2-api.md)** *(Auth & Circulation REST API)*

---

## 📌 Diagram Standard
Tất cả các diagram trong thư mục này tuân thủ quy định **Code-First / Diagram-as-Code** (nhúng trực tiếp qua **Mermaid** ` ```mermaid ` và **PlantUML**), đảm bảo khả năng xem trực tiếp trên GitHub và chỉnh sửa source dễ dàng.