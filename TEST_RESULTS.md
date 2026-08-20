# Kết quả kiểm thử Sprint 1

Ngày kiểm thử: 2026-08-20 · Người kiểm thử: Nguyen Thi Khanh Linh
Môi trường: FE `127.0.0.1:5173`, BE `127.0.0.1:8080`, H2 test profile.

| ID | Kết quả thực tế | Kết quả |
|---|---|---|
| TC01 | Tìm `Clean Code` trả đúng 1 tài liệu và đúng detail | Đạt |
| TC02 | Từ khóa không tồn tại trả 0 item, UI hiện empty state | Đạt |
| TC03 | `Code` trả `Clean Code` | Đạt |
| TC04 | `clean code` vẫn trả đúng kết quả | Đạt |
| TC05 | Query có khoảng trắng được trim | Đạt |
| TC06 | Query trống trả 4 tài liệu mặc định | Đạt |
| TC07 | `@#$%` trả rỗng, server không lỗi | Đạt |
| TC08 | Chuỗi 100 ký tự trả rỗng, server không lỗi | Đạt |
| TC09 | Browse hiện đúng 4 tài liệu seed | Đạt |
| TC10 | Component hiện empty state khi `items=[]` | Đạt |
| TC11 | 4 ID duy nhất, không lặp hoặc mất | Đạt |
| TC12 | Thiếu ảnh vẫn hiện bìa `LIB` mặc định | Đạt |
| TC13 | Thiếu category/cover/color không làm trang lỗi | Đạt |
| TC14 | ID 1 hiện đúng Clean Code | Đạt |
| TC15 | ID 999999 trả 404 và UI Not Found | Đạt |
| TC16 | ID `abc` trả 400, UI error, server không crash | Đạt |
| TC17 | SICP và hai tác giả không vỡ layout | Đạt |
| TC18 | Field optional trống không hiện null/undefined | Đạt |
| TC19 | Mở trực tiếp `/resources/1` tải đúng | Đạt |
| TC20 | Resource 4 hiện `1 / 2` physical | Đạt |
| TC21 | Resource 1 hiện `2 / 5` physical | Đạt |
| TC22 | Resource 2 hiện `0 / 3` physical | Đạt |
| TC23 | BE đếm available và total từ cùng bảng | Đạt |
| TC24 | BE dùng COUNT nên available không thể âm | Đạt |
| TC25 | Resource có digital hiện Digital Available | Đạt |
| TC26 | Resource 2 không tự hiện Digital | Đạt |
| TC27 | Resource 1/4 hiện cả Physical và Digital | Đạt |
| TC28 | Resource 2 chỉ hiện Physical | Đạt |
| TC29 | Resource 3 chỉ hiện Digital | Đạt |
| TC30 | Không có access type sẽ render grid trống | **Không đạt** |
| TC31 | Search Clean Code mở đúng detail Clean Code | Đạt |
| TC32 | Browse → Detail → back giữ được trang trước | Đạt |
| TC33 | Không xảy ra trường hợp search A nhưng detail B | Đạt |
| TC34 | HTTP không OK được chuyển sang error state | Đạt |
| TC35 | Tắt BE thì FE hiện lỗi và nút Thử lại | Đạt |
| TC36 | Thiếu authors/accessTypes có thể làm FE crash | **Không đạt** |
| TC37 | Có skeleton list và detail khi loading | Đạt |
| TC38 | Có empty state và nút xóa tìm kiếm | Đạt |
| TC39 | Có error state và nút thử lại | Đạt |
| TC40 | Link detail không tạo mutation khi click nhanh | Đạt |

## Tổng kết

- Đạt: **38/40**
- Không đạt: **TC30, TC36**
- Backend automated tests: **11/11 pass**
- Frontend lint/build: **pass**

## Cần sửa

1. Hiện thông báo khi tài liệu không có cả physical và digital.
2. Chuẩn hóa response để `authors` và `accessTypes` luôn là mảng.
