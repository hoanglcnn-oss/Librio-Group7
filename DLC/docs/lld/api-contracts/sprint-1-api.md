# API Contracts & Specifications — Sprint 1

**Project:** Librio  
**Sprint:** S1 — Read-Only Reader Discovery APIs  

---

## 1. Search / Browse API

`GET /resources?q={keyword}`

### Request Parameters
| Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `q` | `string` | No | Từ khóa tìm kiếm. Nếu trống, trả danh sách mặc định. |

### Responses

#### `200 OK` — Success (Found Items)
```json
{
  "items": [
    {
      "id": 1,
      "title": "Clean Code",
      "authors": ["Robert C. Martin"]
    }
  ]
}
```

#### `200 OK` — Success (Empty List)
```json
{
  "items": []
}
```

---

## 2. Resource Detail & Availability API

`GET /resources/{id}`

### Request Parameters
| Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `id` | `path param` | Yes | Unique ID của tài liệu |

### Responses

#### `200 OK` — Both Physical and Digital Available
```json
{
  "id": 1,
  "title": "Clean Code",
  "authors": ["Robert C. Martin"],
  "description": "A handbook of agile software craftsmanship.",
  "accessTypes": ["PHYSICAL", "DIGITAL"],
  "physical": {
    "totalCopies": 5,
    "availableCopies": 2
  },
  "digital": {
    "available": true
  }
}
```

#### `200 OK` — Physical Only (Digital Block Omitted)
```json
{
  "id": 2,
  "title": "Refactoring",
  "authors": ["Martin Fowler"],
  "description": "Improving the design of existing code.",
  "accessTypes": ["PHYSICAL"],
  "physical": {
    "totalCopies": 3,
    "availableCopies": 0
  }
}
```

#### `404 Not Found` — Resource ID Doesn't Exist
```json
{
  "message": "Resource not found"
}
```
