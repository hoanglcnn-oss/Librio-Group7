# Librio Frontend

Frontend React + Vite cho luồng tìm kiếm tài nguyên, xem chi tiết và kiểm tra tình trạng khả dụng.

## Chạy local

```bash
npm install
npm run dev
```

Mặc định ứng dụng dùng fixtures tại `src/data/mockResources.js`. Component chỉ lấy dữ liệu qua `src/services/resourceApi.js`.

Để chuyển sang backend thật, tạo `.env.local`:

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

Không đặt `VITE_API_BASE_URL` nếu muốn tiếp tục dùng mock adapter. Backend được kỳ vọng cung cấp:

- `GET /resources?q=<keyword>` trả `{ "items": [...] }`.
- `GET /resources/:id` trả resource detail; HTTP 404 được hiển thị thành Not Found.

## Kiểm tra

```bash
npm run lint
npm run build
```
