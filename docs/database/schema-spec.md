# Database Schema Specification & Constraints

**Project:** Librio  
**Document:** Logical Database Schema Specification  

---

## 1. Entity Specifications (Chi tiết Bảng & Cột)

### 1.1 `RESOURCE`
| Column Name | Data Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `id` | `VARCHAR(64)` / `BIGINT` | PRIMARY KEY | Định danh duy nhất cho tài liệu |
| `title` | `VARCHAR(255)` | NOT NULL | Tiêu đề sách / tài liệu |
| `authors` | `VARCHAR(255)` | NOT NULL | Tên tác giả |
| `description` | `TEXT` | NULLABLE | Mô tả chi tiết tài liệu |

### 1.2 `PHYSICAL_ITEM`
| Column Name | Data Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `id` | `VARCHAR(64)` / `BIGINT` | PRIMARY KEY | Định danh bản sao vật lý (Mã vạch/Mã nhãn) |
| `resource_id` | `VARCHAR(64)` / `BIGINT` | FOREIGN KEY (`RESOURCE.id`) | Liên kết về Resource mẹ |
| `status` | `VARCHAR(32)` | NOT NULL | Enum: `AVAILABLE`, `BORROWED`, `OVERDUE` |

### 1.3 `DIGITAL_ITEM`
| Column Name | Data Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `id` | `VARCHAR(64)` / `BIGINT` | PRIMARY KEY | Định danh tài liệu số |
| `resource_id` | `VARCHAR(64)` / `BIGINT` | FOREIGN KEY (`RESOURCE.id`) | Liên kết về Resource mẹ |

### 1.4 `BORROWING`
| Column Name | Data Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `id` | `VARCHAR(64)` / `BIGINT` | PRIMARY KEY | Định danh lượt mượn |
| `physical_item_id` | `VARCHAR(64)` / `BIGINT` | FOREIGN KEY (`PHYSICAL_ITEM.id`) | Cuốn sách vật lý được mượn |
| `user_id` | `VARCHAR(64)` / `BIGINT` | FOREIGN KEY (`USER.id`) | Người dùng thực hiện mượn |
| `borrowed_at` | `TIMESTAMP` | NOT NULL | Thời điểm bắt đầu mượn |
| `due_at` | `TIMESTAMP` | NOT NULL | Hạn trả sách |
| `returned_at` | `TIMESTAMP` | NULLABLE | Thời điểm trả sách thực tế (`NULL` = Đang mượn) |

### 1.5 `USER`
| Column Name | Data Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `id` | `VARCHAR(64)` / `BIGINT` | PRIMARY KEY | Định danh người dùng |
| `username` | `VARCHAR(100)` | NOT NULL | Tên tài khoản |

---

## 2. Business Constraints (Ràng buộc Nghiệp vụ)

- **BC-01 (One Active Borrowing):** Mỗi `PHYSICAL_ITEM` chỉ có tối đa 1 bản ghi `BORROWING` có `returned_at IS NULL`.
- **BC-02 (Valid Lifecycle):** Trạng thái `PHYSICAL_ITEM` đi từ `AVAILABLE ➔ BORROWED ➔ OVERDUE ➔ AVAILABLE`.
- **BC-03 (Derived Availability):** Không tạo bảng `Availability`. Availability được đếm động bằng SQL Query.
- **BC-04 (Zero-Item Resource):** Cho phép `RESOURCE` có `0` physical item hoặc `0` digital item mà vẫn tìm kiếm được.
