# D03. Requirement & Design Documents — Librio Group 7

> Thư mục lưu trữ toàn bộ tài liệu Yêu cầu (SRS), Thiết kế Cơ sở Dữ liệu (Database Design), Thiết kế Kiến trúc (HLD) và Thiết kế Chi tiết (LLD) của dự án **Librio**.

---

## 📂 Danh mục Tài liệu & Diagram (Index)

### 1. Software Requirements Specification (SRS)
- 📄 **[Overview (`srs/README.md`)](file:///d:/FM%E1%BB%81m/Librio-Group7/docs/srs/README.md)**  
- 📄 **[Sprint 1 SRS Baseline (`srs/sprint-1-srs.md`)](file:///d:/FM%E1%BB%81m/Librio-Group7/docs/srs/sprint-1-srs.md)** *(Yêu cầu LIB-01 đến LIB-06, User Stories, Acceptance Criteria)*

### 2. Database Design
- 📄 **[Overview (`database/README.md`)](file:///d:/FM%E1%BB%81m/Librio-Group7/docs/database/README.md)**  
- 📊 **[Source ERD Mermaid (`database/erd.mmd`)](file:///d:/FM%E1%BB%81m/Librio-Group7/docs/database/erd.mmd)** *(ERD Diagram-as-Code)*  
- 📑 **[Schema Specification & Constraints (`database/schema-spec.md`)](file:///d:/FM%E1%BB%81m/Librio-Group7/docs/database/schema-spec.md)** *(Tables, Columns, Data Types, Constraints BC-01 đến BC-04)*

### 3. High-Level Design (HLD)
- 📄 **[Architecture Specification (`hld/architecture.md`)](file:///d:/FM%E1%BB%81m/Librio-Group7/docs/hld/architecture.md)** *(Modular Monolith, Layers, Module Boundaries)*  
- 📊 **[Component Diagram Source (`hld/component-diagram.mmd`)](file:///d:/FM%E1%BB%81m/Librio-Group7/docs/hld/component-diagram.mmd)** *(Mermaid Component Diagram)*

### 4. Low-Level Design (LLD)
- 📄 **[Overview (`lld/README.md`)](file:///d:/FM%E1%BB%81m/Librio-Group7/docs/lld/README.md)**  
- 📘 **[Sprint 1 Unified LLD Baseline (`lld/sprint-1-lld.md`)](file:///d:/FM%E1%BB%81m/Librio-Group7/docs/lld/sprint-1-lld.md)** *(Tài liệu LLD tổng hợp + 18 Test Scenarios)*  
- 🔌 **[API Contracts (`lld/api-contracts/sprint-1-api.md`)](file:///d:/FM%E1%BB%81m/Librio-Group7/docs/lld/api-contracts/sprint-1-api.md)** *(REST JSON Payloads)*  
- 🎨 **[Frontend Architecture (`lld/frontend-lld.md`)](file:///d:/FM%E1%BB%81m/Librio-Group7/docs/lld/frontend-lld.md)** *(React Components breakdown & UI Rules)*  
- 🔄 **[Sequence Diagram Source (`lld/sequence-diagrams/s1-search-detail.mmd`)](file:///d:/FM%E1%BB%81m/Librio-Group7/docs/lld/sequence-diagrams/s1-search-detail.mmd)** *(Search & Detail Sequence Flow)*

---

## 📌 Diagram Standard
Tất cả các diagram trong thư mục này tuân thủ quy định **Code-First / Diagram-as-Code** (nhúng trực tiếp qua **Mermaid** ` ```mermaid ` và **PlantUML**), đảm bảo khả năng xem trực tiếp trên GitHub và chỉnh sửa source dễ dàng.