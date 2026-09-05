# Librio Frontend

React + Vite frontend cho Librio.

## Chạy local

```powershell
npm install
npm run dev
```

Tạo `.env.local` từ `.env.example`. Chỉ các biến có tiền tố `VITE_` được đưa vào frontend; không đặt mật khẩu, token, khóa API hoặc database credential trong các biến này.

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_USE_MOCK_BORROWINGS=true
VITE_USE_MOCK_LIBRARIAN_BORROWINGS=true
VITE_USE_MOCK_DIGITAL_ACCESS=true
VITE_USE_MOCK_RESOURCE_ADMIN=true
```

Mock chỉ hoạt động trong development mode. Production bundle luôn tắt mock ngay cả khi máy phát triển còn `.env.local`.

## Production build

```powershell
npm ci
npm test
npm run lint
npm run build:production
```

Output deploy nằm trong `dist/`. `build:production` tự kiểm tra bundle và thất bại nếu phát hiện `localhost`, `127.0.0.1`, tên biến mock hoặc thiếu `dist/index.html`.

`.env.production` mặc định dùng API cùng origin dưới `/api`:

```env
VITE_API_BASE_URL=/api
```

Nếu frontend và backend deploy ở hai origin khác nhau, truyền URL lúc build trên CI/hosting:

```powershell
$env:VITE_API_BASE_URL="https://api.example.edu"
npm run build:production
```

Không sửa source để hard-code URL theo môi trường.

## Biến môi trường

| Biến | Production | Mục đích |
| --- | --- | --- |
| `VITE_API_BASE_URL` | `/api` | URL API `http(s)` hoặc đường dẫn cùng origin bắt đầu bằng `/`. |
| `VITE_USE_MOCK_BORROWINGS` | `false` | Mock T-103 trong development. |
| `VITE_USE_MOCK_LIBRARIAN_BORROWINGS` | `false` | Mock T-114 trong development. |
| `VITE_USE_MOCK_DIGITAL_ACCESS` | `false` | Mock T-125 trong development. |
| `VITE_USE_MOCK_RESOURCE_ADMIN` | `false` | Mock T-135 trong development. |
