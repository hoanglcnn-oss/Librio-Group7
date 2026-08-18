# Frontend Architecture & Low-Level Design (PL) — Sprint 1

**Project:** Librio  
**Sprint:** S1 Frontend Design Package  

---

## 1. Cấu trúc Thư mục Frontend (`src/`)

```text
src/
├── pages/
│   ├── ResourceListPage.jsx      # Màn hình Browse/Search danh sách
│   └── ResourceDetailPage.jsx    # Màn hình Xem chi tiết & Availability
├── components/
│   ├── SearchInput.jsx           # Input nhập từ khóa tìm kiếm (Controlled)
│   ├── ResourceList.jsx          # Container render danh sách cards hoặc Empty State
│   ├── ResourceCard.jsx          # Card hiển thị tóm tắt thông tin tài liệu
│   └── AvailabilitySection.jsx   # Widget hiển thị trạng thái Physical/Digital
├── services/
│   └── resourceApi.js            # Service layer gọi API và bắt lỗi
├── routes/
│   └── AppRoutes.jsx             # Định tuyến URL /resources và /resources/:id
└── types/
    └── resource.js               # Type definition cho DTO
```

---

## 2. Bảng Phân rã Component & Trách nhiệm

| Component | Trách nhiệm chính |
|---|---|
| `AppRoutes` | Map `/resources` và `/resources/:id` tới các trang tương ứng. |
| `ResourceListPage` | Đọc search query từ URL (`?q=...`), gọi `resourceApi.getResources()`, quản lý UI state (loading, empty, error). |
| `SearchInput` | Nhập, trim khoảng trắng và push keyword lên URL params khi submit. |
| `ResourceList` | Render danh sách `ResourceCard` hoặc hiển thị Empty State. |
| `ResourceCard` | Hiển thị tóm tắt title, authors và link mở `/resources/:id`. |
| `ResourceDetailPage` | Đọc `id` từ Route params, gọi `resourceApi.getResourceById(id)`, quản lý state detail/404/error. |
| `AvailabilitySection` | Render khối số bản vật lý (`availableCopies / totalCopies`) và trạng thái bản số (`Available`). |
| `resourceApi` | Chịu trách nhiệm HTTP GET request, chuẩn hóa lỗi cho UI. |

---

## 3. Quy tắc Hiển thị UI State
- **Loading:** Fetching API ➔ Hiển thị Skeleton/Spinner, không render Empty/Not Found.
- **Empty Search:** API trả `200` với `items: []` ➔ Render Empty State.
- **404 Not Found:** API Detail trả `404` ➔ Render màn hình Not Found.
- **Error State:** API/network lỗi ➔ Render Error State kèm nút **Retry**.
