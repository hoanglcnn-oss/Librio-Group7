# Librio Database Migrations (Flyway)

Starting with Librio V2 (Batch 20B), database schema evolution is managed via versioned **Flyway** migrations rather than raw `schema.sql` startup scripts.

## Migration Files
- `V1__initial_schema.sql`: Establishes the complete baseline schema for Librio V1 (Catalog, Accounts, Circulation, Memberships/Payments, and PostgreSQL-specific partial indexes).

---

## Behavior by Environment

### 1. Clean / Fresh Databases (Default)
On a fresh PostgreSQL database with no pre-existing schema:
- Flyway runs automatically on application startup.
- Creates the `flyway_schema_history` table.
- Executes `V1__initial_schema.sql` cleanly to establish version 1.
- No special configuration required.

### 2. Pre-existing Pre-Flyway Databases (Baseline Procedure)
By default, **`baseline-on-migrate` is disabled (`false`)** to prevent silently masking unknown or incompatible schema discrepancies:
```yaml
spring.flyway.baseline-on-migrate: ${FLYWAY_BASELINE_ON_MIGRATE:false}
spring.flyway.baseline-version: 1
```

If an existing database created before Flyway is started without a baseline, Flyway will deliberately fail on startup with:
```
FlywayException: Found non-empty schema(s) "public" but no schema history table. Use baseline() or set baselineOnMigrate to true to initialize the schema history table.
```

#### Exact Procedure to Baseline an Existing Database:
1. **Verify Schema Alignment**:
   Compare the existing database schema with `V1__initial_schema.sql` to verify that all 9 tables, partial indexes (`uq_active_request_physical_item`, `uq_active_request_reader_resource`, `uq_active_borrowing_physical_item`), foreign keys, and constraints are present and intact.
2. **Explicit Baseline Execution**:
   - **Option A (Flyway CLI / Maven)**:
     ```bash
     ./mvnw flyway:baseline -Dflyway.baselineVersion=1 -Dflyway.url=... -Dflyway.user=... -Dflyway.password=...
     ```
   - **Option B (One-time Environment Variable on Startup)**:
     Start the application once with the explicit baseline flag:
     ```bash
     FLYWAY_BASELINE_ON_MIGRATE=true ./mvnw spring-boot:run
     ```
     Once the `flyway_schema_history` record for version 1 is created, unset `FLYWAY_BASELINE_ON_MIGRATE` or keep it at default `false`.

#### Caveat:
> **WARNING**: Never set `baseline-on-migrate=true` globally or permanently in production configs. Doing so risks silently baselining partially-migrated, legacy, or corrupted database states against future migrations (e.g. V2, V3). Always baseline explicitly only after verifying the schema matches V1.

---

## Demo Seed Data Separation (`data.sql`)
- In production / standard PostgreSQL mode, `spring.sql.init.mode` is set to `${SPRING_SQL_INIT_MODE:never}`. Demo seed data is **not** loaded automatically.
- To seed development demo data on startup, explicitly run with:
  ```bash
  SPRING_SQL_INIT_MODE=always ./mvnw spring-boot:run
  ```
- In the `test` profile (H2 in-memory), Hibernate handles temporary DDL generation (`create-drop`), Flyway is disabled, and `data.sql` is loaded automatically for unit and integration testing.
