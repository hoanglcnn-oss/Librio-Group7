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
```

## Production build

```powershell
npm ci
npm test
npm run lint
npm run build:production
```

Output deploy nằm trong `dist/`.

`.env.production` mặc định dùng API cùng origin và không thêm `/api` prefix:

```env
VITE_API_BASE_URL=
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
| `VITE_API_BASE_URL` | *(empty)* | URL API `http(s)` hoặc đường dẫn cùng origin bắt đầu bằng `/`. |
