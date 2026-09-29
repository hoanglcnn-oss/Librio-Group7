# API Contracts & Specifications — Sprint 2
**Project:** Librio  
**Sprint:** S2 — Authentication, Borrow Requests & My Library  
**Sources:** Sprint 2 SRS, T-072, T-082, T-091

---

API không sử dụng prefix `/api`. Protected endpoint sử dụng server-side session cookie. State-changing request yêu cầu CSRF token trong header do `/auth/csrf` cung cấp. ID dùng JSON number; timestamp dùng ISO 8601 có offset. Empty collection trả `200 OK`, không trả `404`.

## 1. Authentication APIs
### 1.1 `GET /auth/csrf`
Public endpoint dùng để lấy CSRF token trước khi gọi state-changing request.

#### `200 OK`
```json
{
  "token": "csrf-token-value",
  "headerName": "X-CSRF-TOKEN",
  "parameterName": "_csrf"
}
```

### 1.2 `POST /auth/login`
**Access:** Public; CSRF token bắt buộc.

#### Request
```json
{
  "email": "reader@librio.local",
  "password": "reader-password"
}
```

Email được trim và chuyển lowercase trước khi xác thực.

#### `200 OK`
```json
{
  "id": 1,
  "email": "reader@librio.local",
  "displayName": "Demo Reader",
  "role": "READER",
  "accountStatus": "ACTIVE"
}
```

#### Possible errors
`400 VALIDATION_ERROR`  
`401 INVALID_CREDENTIALS`  
`403 CSRF_TOKEN_INVALID`

Sai email, sai password và account `DISABLED` cùng trả `INVALID_CREDENTIALS`.

### 1.3 `GET /auth/me`
**Access:** Authenticated account.

#### `200 OK`
```json
{
  "id": 1,
  "email": "reader@librio.local",
  "displayName": "Demo Reader",
  "role": "READER",
  "accountStatus": "ACTIVE"
}
```

#### Possible errors
`401 AUTHENTICATION_REQUIRED`

### 1.4 `POST /auth/logout`
**Access:** Authenticated account; CSRF token bắt buộc.

#### `204 No Content`
Server xóa SecurityContext, invalidate session và clear session cookie. Response không có body.

#### Possible errors
`401 AUTHENTICATION_REQUIRED`  
`403 CSRF_TOKEN_INVALID`

---

## 2. Borrow Request API
### 2.1 `POST /borrow-requests`
**Access:** `ROLE_READER`; CSRF token bắt buộc.

Reader identity lấy từ authenticated session. Request không nhận `readerId`, `physicalItemId` hoặc `dueDate`.

#### Request
```json
{
  "resourceId": 10
}
```

#### `201 Created`
```json
{
  "id": 1001,
  "status": "REQUESTED",
  "resource": {
    "id": 10,
    "title": "Clean Code",
    "authors": ["Robert C. Martin"]
  },
  "requestedAt": "2026-08-25T09:00:00+07:00",
  "statusUpdatedAt": "2026-08-25T09:00:00+07:00",
  "expiresAt": "2026-08-26T09:00:00+07:00"
}
```

Request thành công reserve ngay một physical item nhưng chưa tạo `Borrowing` hoặc `dueDate`.

#### Possible errors
`400 VALIDATION_ERROR`  
`401 AUTHENTICATION_REQUIRED`  
`403 OPERATION_FORBIDDEN`  
`403 CSRF_TOKEN_INVALID`  
`404 RESOURCE_NOT_FOUND`  
`409 NO_PHYSICAL_COPY`  
`409 NO_AVAILABLE_COPY`  
`409 DUPLICATE_ACTIVE_REQUEST`  
`409 ACTIVE_BORROWING_EXISTS`  
`409 BORROWING_LIMIT_REACHED`

---

## 3. My Library APIs
### 3.1 `GET /me/borrow-requests`
**Access:** `ROLE_READER`.

Chỉ trả request thuộc authenticated reader. Active requests được sắp theo urgency; recent outcomes chứa tối đa 5 terminal requests gần nhất.

#### `200 OK`
```json
{
  "activeRequests": [
    {
      "id": 1001,
      "resource": {
        "id": 10,
        "title": "Clean Code",
        "authors": ["Robert C. Martin"]
      },
      "status": "READY_FOR_PICKUP",
      "requestedAt": "2026-08-25T09:00:00+07:00",
      "statusUpdatedAt": "2026-08-25T10:00:00+07:00",
      "expiresAt": "2026-08-27T17:00:00+07:00"
    }
  ],
  "recentOutcomes": [
    {
      "id": 998,
      "resource": {
        "id": 12,
        "title": "Refactoring",
        "authors": ["Martin Fowler"]
      },
      "status": "EXPIRED",
      "requestedAt": "2026-08-20T09:00:00+07:00",
      "statusUpdatedAt": "2026-08-22T17:00:00+07:00",
      "expiresAt": "2026-08-22T17:00:00+07:00"
    }
  ]
}
```

#### `200 OK` — Empty
```json
{
  "activeRequests": [],
  "recentOutcomes": []
}
```

#### Ordering
```text
Active Requests:
READY_FOR_PICKUP trước REQUESTED
→ expiresAt ASC NULLS LAST
→ requestedAt ASC
→ id ASC

Recent Outcomes:
statusUpdatedAt DESC
→ id ASC
→ LIMIT 5
```

#### Possible errors
`401 AUTHENTICATION_REQUIRED`  
`403 OPERATION_FORBIDDEN`

### 3.2 `GET /me/borrowings`
**Access:** `ROLE_READER`.

Chỉ trả active borrowings thuộc authenticated reader. Completed borrowing history chưa thuộc Sprint 2.

#### `200 OK`
```json
{
  "activeBorrowings": [
    {
      "id": 2001,
      "resource": {
        "id": 10,
        "title": "Clean Code",
        "authors": ["Robert C. Martin"]
      },
      "borrowedAt": "2026-08-25T14:00:00+07:00",
      "dueDate": "2026-09-08T14:00:00+07:00"
    }
  ]
}
```

#### `200 OK` — Empty
```json
{
  "activeBorrowings": []
}
```

#### Ordering
```text
dueDate ASC
→ borrowedAt ASC
→ id ASC
```

#### Possible errors
`401 AUTHENTICATION_REQUIRED`  
`403 OPERATION_FORBIDDEN`

### 3.3 `POST /me/borrow-requests/{requestId}/cancel`
**Access:** `ROLE_READER`; CSRF token bắt buộc.

Chỉ request `REQUESTED` hoặc `READY_FOR_PICKUP` thuộc authenticated reader được cancel. Request không có body và không bị hard-delete.

#### Request Parameters
| Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `requestId` | path param | Yes | ID của borrow request cần cancel. |

#### `200 OK`
```json
{
  "id": 1001,
  "status": "CANCELLED",
  "resource": {
    "id": 10,
    "title": "Clean Code",
    "authors": ["Robert C. Martin"]
  },
  "requestedAt": "2026-08-25T09:00:00+07:00",
  "statusUpdatedAt": "2026-08-25T11:00:00+07:00",
  "expiresAt": "2026-08-27T17:00:00+07:00"
}
```

#### Possible errors
`401 AUTHENTICATION_REQUIRED`  
`403 OPERATION_FORBIDDEN`  
`403 CSRF_TOKEN_INVALID`  
`404 REQUEST_NOT_FOUND`  
`409 REQUEST_NOT_CANCELLABLE`  
`409 REQUEST_EXPIRED`

Request không tồn tại và request thuộc reader khác cùng trả `404 REQUEST_NOT_FOUND`.

---

## 4. Librarian Circulation APIs
### 4.1 `GET /librarian/borrow-requests`
**Access:** `ROLE_LIBRARIAN`.

Trả các request `REQUESTED` và `READY_FOR_PICKUP` cần librarian xử lý.

#### `200 OK`
```json
{
  "items": [
    {
      "id": 1001,
      "status": "READY_FOR_PICKUP",
      "reader": {
        "id": 1,
        "displayName": "Demo Reader"
      },
      "resource": {
        "id": 10,
        "title": "Clean Code",
        "authors": ["Robert C. Martin"]
      },
      "physicalItemId": 501,
      "requestedAt": "2026-08-25T09:00:00+07:00",
      "statusUpdatedAt": "2026-08-25T10:00:00+07:00",
      "expiresAt": "2026-08-27T17:00:00+07:00"
    }
  ]
}
```

#### `200 OK` — Empty
```json
{
  "items": []
}
```

#### Possible errors
`401 AUTHENTICATION_REQUIRED`  
`403 OPERATION_FORBIDDEN`

### 4.2 `POST /librarian/borrow-requests/{requestId}/prepare`
**Access:** `ROLE_LIBRARIAN`; CSRF token bắt buộc.

#### Request
```json
{
  "physicalItemId": 501
}
```

#### `200 OK`
```json
{
  "id": 1001,
  "status": "READY_FOR_PICKUP",
  "reader": {
    "id": 1,
    "displayName": "Demo Reader"
  },
  "resource": {
    "id": 10,
    "title": "Clean Code",
    "authors": ["Robert C. Martin"]
  },
  "physicalItemId": 501,
  "requestedAt": "2026-08-25T09:00:00+07:00",
  "statusUpdatedAt": "2026-08-25T10:00:00+07:00",
  "expiresAt": "2026-08-27T17:00:00+07:00"
}
```

#### Possible errors
`400 VALIDATION_ERROR`  
`401 AUTHENTICATION_REQUIRED`  
`403 OPERATION_FORBIDDEN`  
`403 CSRF_TOKEN_INVALID`  
`404 REQUEST_NOT_FOUND`  
`409 ITEM_MISMATCH`  
`409 INVALID_REQUEST_STATE`  
`409 REQUEST_EXPIRED`  
`409 RESERVATION_CONFLICT`

### 4.3 `POST /librarian/borrow-requests/{requestId}/reject`
**Access:** `ROLE_LIBRARIAN`; CSRF token bắt buộc.

Sprint 2 không nhận custom rejection reason. Request không có body.

#### `200 OK`
```json
{
  "id": 1001,
  "status": "REJECTED",
  "reader": {
    "id": 1,
    "displayName": "Demo Reader"
  },
  "resource": {
    "id": 10,
    "title": "Clean Code",
    "authors": ["Robert C. Martin"]
  },
  "physicalItemId": 501,
  "requestedAt": "2026-08-25T09:00:00+07:00",
  "statusUpdatedAt": "2026-08-25T10:30:00+07:00",
  "expiresAt": "2026-08-27T17:00:00+07:00"
}
```

#### Possible errors
`401 AUTHENTICATION_REQUIRED`  
`403 OPERATION_FORBIDDEN`  
`403 CSRF_TOKEN_INVALID`  
`404 REQUEST_NOT_FOUND`  
`409 INVALID_REQUEST_STATE`  
`409 REQUEST_EXPIRED`  
`409 RESERVATION_CONFLICT`

### 4.4 `POST /librarian/borrow-requests/{requestId}/fulfil`
**Access:** `ROLE_LIBRARIAN`; CSRF token bắt buộc.

Chỉ request `READY_FOR_PICKUP` được fulfil. Client xác nhận exact allocated item nhưng không được gửi hoặc override `dueDate`.

#### Request
```json
{
  "physicalItemId": 501
}
```

#### `201 Created`
```json
{
  "id": 2001,
  "borrowRequestId": 1001,
  "resource": {
    "id": 10,
    "title": "Clean Code",
    "authors": ["Robert C. Martin"]
  },
  "borrowedAt": "2026-08-25T14:00:00+07:00",
  "dueDate": "2026-09-08T14:00:00+07:00"
}
```

#### Possible errors
`400 VALIDATION_ERROR`  
`401 AUTHENTICATION_REQUIRED`  
`403 OPERATION_FORBIDDEN`  
`403 CSRF_TOKEN_INVALID`  
`404 REQUEST_NOT_FOUND`  
`409 ITEM_MISMATCH`  
`409 INVALID_REQUEST_STATE`  
`409 REQUEST_EXPIRED`  
`409 READER_INELIGIBLE`  
`409 RESERVATION_CONFLICT`

---

## 5. Standard Error Response
Mọi API error trả JSON, không redirect HTML và không lộ stack trace, SQL hoặc credential.

```json
{
  "status": 409,
  "code": "NO_AVAILABLE_COPY",
  "message": "No physical copy is currently available",
  "timestamp": "2026-08-25T09:00:00+07:00"
}
```

| HTTP Status | Stable Error Code | Meaning |
| :--- | :--- | :--- |
| `400` | `VALIDATION_ERROR` | Request thiếu field hoặc sai định dạng. |
| `401` | `AUTHENTICATION_REQUIRED` | Protected endpoint được gọi khi chưa có session hợp lệ. |
| `401` | `INVALID_CREDENTIALS` | Login credential sai hoặc account bị disabled. |
| `403` | `OPERATION_FORBIDDEN` | Account không có required role. |
| `403` | `CSRF_TOKEN_INVALID` | CSRF token thiếu, sai hoặc hết hạn. |
| `404` | `RESOURCE_NOT_FOUND` | Resource không tồn tại. |
| `404` | `REQUEST_NOT_FOUND` | Request không tồn tại hoặc không thuộc current reader. |
| `409` | `NO_PHYSICAL_COPY` | Resource không có physical item. |
| `409` | `NO_AVAILABLE_COPY` | Không còn copy `AVAILABLE`. |
| `409` | `DUPLICATE_ACTIVE_REQUEST` | Reader đã có active request cùng resource. |
| `409` | `ACTIVE_BORROWING_EXISTS` | Reader đang có active borrowing cùng resource. |
| `409` | `BORROWING_LIMIT_REACHED` | Reader đã đạt configured commitment limit. |
| `409` | `ITEM_MISMATCH` | Librarian xác nhận sai allocated item. |
| `409` | `INVALID_REQUEST_STATE` | Action không hợp lệ với request state hiện tại. |
| `409` | `REQUEST_NOT_CANCELLABLE` | Request không còn được phép cancel. |
| `409` | `REQUEST_EXPIRED` | Request đã quá `expiresAt`. |
| `409` | `READER_INELIGIBLE` | Reader không còn đủ điều kiện checkout. |
| `409` | `RESERVATION_CONFLICT` | Item không còn reserved đúng cho request. |