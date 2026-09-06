# Database Design — Librio

Thư mục này chứa living database design của Librio qua Sprint 3.

## Documents

- [Entity Relationship Diagram](./erd.mmd)
- [Schema Specification and Constraints](./schema-spec.md)
- [Reviewable SQL Schema](./schema.sql)
- [Executable Runtime Schema](../../backend/src/main/resources/schema.sql)

## Source Alignment

Database artifacts phải được giữ đồng bộ:

`JPA entities`
`↔ schema.sql`
`↔ schema-spec.md`
`↔ erd.mmd`

- `schema-spec.md` mô tả logical schema và business constraints.
- `erd.mmd` mô tả entity relationships.
- `docs/database/schema.sql` phải khớp byte-for-byte với runtime `backend/src/main/resources/schema.sql`.
- Runtime schema là bản application thực thi; bản trong docs phục vụ review/audit.

Availability là derived data; hệ thống không tạo bảng availability.
