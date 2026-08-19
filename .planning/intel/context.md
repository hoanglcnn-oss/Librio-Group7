## D03. Requirement & Design Documents — Librio Group 7
- source: docs/README.md
DATA_Y4M8P2QK_START
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
DATA_Y4M8P2QK_END

## Database Design & ERD Specification — Overview
- source: docs/database/README.md
DATA_H3V7N1RC_START
> Thư mục này chứa toàn bộ thiết kế cơ sở dữ liệu (Logical Schema & ERD) của dự án **Librio**.

---

## 📂 Danh mục File Thiết kế CSDL

- 📊 **[Source ERD Mermaid (`erd.mmd`)](file:///d:/FM%E1%BB%81m/Librio-Group7/docs/database/erd.mmd)**  
  *(Sơ đồ Entity-Relationship Diagram dạng code-first Mermaid editable)*
- 📑 **[Schema Specification & Constraints (`schema-spec.md`)](file:///d:/FM%E1%BB%81m/Librio-Group7/docs/database/schema-spec.md)**  
  *(Mô tả chi tiết các Bảng, Cột, Kiểu dữ liệu, Primary/Foreign Keys và các Business Constraints BC-01 đến BC-04)*
DATA_H3V7N1RC_END

## Low-Level Design (LLD) — Overview
- source: docs/lld/README.md
DATA_B6Q2K9XT_START
> Thư mục này chứa toàn bộ thiết kế chi tiết (Low-Level Design) của dự án **Librio**, phân chia theo API Contracts, Frontend Design, Sequence Diagrams và Test Verification.

---

## 📂 Danh mục File Thiết kế LLD

- 📘 **[Sprint 1 Unified LLD Specification (`sprint-1-lld.md`)](file:///d:/FM%E1%BB%81m/Librio-Group7/docs/lld/sprint-1-lld.md)**  
  *(Tài liệu tổng hợp Baseline LLD cho Sprint 1, bao gồm cả Frontend, Backend & 18 Test Scenarios)*
- 🔌 **[API Contracts (`api-contracts/sprint-1-api.md`)](file:///d:/FM%E1%BB%81m/Librio-Group7/docs/lld/api-contracts/sprint-1-api.md)**  
  *(Định nghĩa chi tiết JSON REST API payloads cho `GET /resources` và `GET /resources/{id}`)*
- 🎨 **[Frontend Architecture (`frontend-lld.md`)](file:///d:/FM%E1%BB%81m/Librio-Group7/docs/lld/frontend-lld.md)**  
  *(Cấu trúc thư mục `src/`, phân rã Components, UI State Rules)*
- 🔄 **[Sequence Diagram Source (`sequence-diagrams/s1-search-detail.mmd`)](file:///d:/FM%E1%BB%81m/Librio-Group7/docs/lld/sequence-diagrams/s1-search-detail.mmd)**  
  *(Sơ đồ luồng tương tác tuần tự giữa User ➔ React UI ➔ API ➔ Service ➔ Database)*
DATA_B6Q2K9XT_END
