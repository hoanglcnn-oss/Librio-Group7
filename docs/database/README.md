# Database Design — Librio

Thư mục này chứa logical database design cho resource discovery, authentication và physical borrowing flow.

## Documents

- [Entity Relationship Diagram](./erd.mmd)
- [Schema Specification and Constraints](./schema-spec.md)
- [Executable Runtime Schema](../../backend/src/main/resources/schema.sql)

## Source Alignment

Database artifacts phải được giữ đồng bộ:

`JPA entities`
`↔ schema.sql`
`↔ schema-spec.md`
`↔ erd.mmd`

- `schema-spec.md` mô tả logical schema và business constraints.
- `erd.mmd` mô tả entity relationships.
- `schema.sql` là executable PostgreSQL schema và phải được kiểm tra riêng trên database test.

Availability là derived data; hệ thống không tạo bảng availability.