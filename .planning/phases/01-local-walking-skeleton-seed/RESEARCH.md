# Phase 1 Research: Local Walking Skeleton & Seed

## Domain & Architecture Context
- **Backend Stack**: Java 17+, Spring Boot 3.x (Web, Data JPA, PostgreSQL Driver).
- **Frontend Stack**: Node.js, React 18+, Vite, React Router DOM, Axios/Fetch.
- **Database Stack**: PostgreSQL, relational schema (`RESOURCE`, `PHYSICAL_ITEM`, `DIGITAL_ITEM`, `BORROWING`, `USER`).
- **Canonical Contracts**: `docs/lld/api-contracts/sprint-1-api.md` & `docs/lld/sprint-1-lld.md`.

## Data Model Specifications
1. `Resource`: `id` (BIGINT/VARCHAR), `title`, `authors`, `description`.
2. `PhysicalItem`: `id`, `resource_id` (FK), `status` (`AVAILABLE`, `BORROWED`, `OVERDUE`).
3. `DigitalItem`: `id`, `resource_id` (FK).
4. `Borrowing`: `id`, `physical_item_id` (FK), `user_id` (FK), `borrowed_at`, `due_at`, `returned_at`.
5. `User`: `id`, `username`.

## Four Seed Cases (Must be present in seed data)
1. **Physical Available**: Resource has physical copies and at least 1 `AVAILABLE` copy (e.g. ID=1 "Clean Code", 5 copies total, 2 available, digital item present).
2. **Physical Out of Stock**: Resource has physical copies, but all copies are `BORROWED` or `OVERDUE` (e.g. ID=2 "Refactoring", 3 copies total, 0 available, digital item absent).
3. **Digital Only**: Resource has 0 physical copies and 1 digital item (e.g. ID=3 "Designing Data-Intensive Applications", 0 physical, digital item present).
4. **Mixed / Both Physical & Digital**: Resource has both physical copies (some available) and digital item (e.g. ID=4 "Structure and Interpretation of Computer Programs", 2 physical copies, 1 available, digital item present).

## Tracer Path
Frontend (React/Vite on localhost:5173 or localhost:3000) -> Backend (Spring Boot REST API on localhost:8080) -> PostgreSQL database (localhost:5432).
CORS must be enabled on backend for frontend origin.
