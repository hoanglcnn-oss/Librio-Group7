package com.librio.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FlywayPostgreSqlMigrationTest {

    private static final String PG_HOST = "localhost:5432";
    private static final String PG_USER = "postgres";
    private static final String PG_PASSWORD = "postgres";
    private static final String ADMIN_URL = "jdbc:postgresql://" + PG_HOST + "/postgres";

    private static final String CLEAN_DB_NAME = "librio_flyway_clean_test";
    private static final String CLEAN_DB_URL = "jdbc:postgresql://" + PG_HOST + "/" + CLEAN_DB_NAME;

    private static final String BASELINE_DB_NAME = "librio_flyway_baseline_test";
    private static final String BASELINE_DB_URL = "jdbc:postgresql://" + PG_HOST + "/" + BASELINE_DB_NAME;

    private static boolean postgresAvailable = false;

    @BeforeAll
    static void checkPostgreSqlAvailability() {
        try (Connection conn = DriverManager.getConnection(ADMIN_URL, PG_USER, PG_PASSWORD);
             Statement stmt = conn.createStatement()) {
            postgresAvailable = true;
            stmt.execute("DROP DATABASE IF EXISTS " + CLEAN_DB_NAME);
            stmt.execute("CREATE DATABASE " + CLEAN_DB_NAME);

            stmt.execute("DROP DATABASE IF EXISTS " + BASELINE_DB_NAME);
            stmt.execute("CREATE DATABASE " + BASELINE_DB_NAME);
        } catch (Exception e) {
            postgresAvailable = false;
        }
    }

    @AfterAll
    static void tearDownDatabases() {
        if (!postgresAvailable) return;
        try (Connection conn = DriverManager.getConnection(ADMIN_URL, PG_USER, PG_PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP DATABASE IF EXISTS " + CLEAN_DB_NAME);
            stmt.execute("DROP DATABASE IF EXISTS " + BASELINE_DB_NAME);
        } catch (Exception ignored) {
        }
    }

    @Test
    void cleanDatabaseReachesCurrentV1SchemaWithFlyway() throws Exception {
        assumeTrue(postgresAvailable, "PostgreSQL not reachable at " + PG_HOST);

        Flyway flyway = Flyway.configure()
                .dataSource(CLEAN_DB_URL, PG_USER, PG_PASSWORD)
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .load();

        MigrateResult result = flyway.migrate();
        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isEqualTo(2);
        assertThat(result.targetSchemaVersion).isEqualTo("2");

        try (Connection conn = DriverManager.getConnection(CLEAN_DB_URL, PG_USER, PG_PASSWORD);
             Statement stmt = conn.createStatement()) {

            // 1. Verify all 9 core tables exist
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE'")) {
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
            }
            assertThat(tables).contains(
                    "accounts",
                    "resource",
                    "physical_item",
                    "digital_item",
                    "borrow_request",
                    "borrowing",
                    "membership_plan",
                    "payment_transaction",
                    "membership_subscription",
                    "flyway_schema_history"
            );

            // 2. Verify accounts columns: reader_type present, membership_eligible removed
            List<String> accountColumns = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT column_name FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'accounts'")) {
                while (rs.next()) {
                    accountColumns.add(rs.getString(1));
                }
            }
            assertThat(accountColumns).contains("reader_type");
            assertThat(accountColumns).doesNotContain("membership_eligible");

            // 3. Verify PostgreSQL partial unique indexes
            List<String> partialIndexes = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT indexname FROM pg_indexes WHERE schemaname = 'public' AND indexdef LIKE '%WHERE%'")) {
                while (rs.next()) {
                    partialIndexes.add(rs.getString(1));
                }
            }
            assertThat(partialIndexes).contains(
                    "uq_active_request_physical_item",
                    "uq_active_request_reader_resource",
                    "idx_borrow_request_expiration",
                    "uq_active_borrowing_physical_item",
                    "idx_borrowing_reader_active_due",
                    "idx_borrowing_active_due"
            );

            // 4. Verify check constraints (including V2 reader_type check constraints)
            List<String> checkConstraints = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT conname FROM pg_constraint WHERE contype = 'c' AND conrelid::regclass::text NOT LIKE 'pg_%'")) {
                while (rs.next()) {
                    checkConstraints.add(rs.getString(1));
                }
            }
            assertThat(checkConstraints).contains(
                    "chk_resource_metadata_source",
                    "chk_physical_item_inventory_status",
                    "chk_physical_item_circulation_status",
                    "chk_account_role",
                    "chk_account_status",
                    "chk_account_reader_type",
                    "chk_account_reader_type_role",
                    "chk_borrow_request_status",
                    "chk_borrowing_due_after_borrowed",
                    "chk_borrowing_returned_after_borrowed",
                    "chk_payment_status",
                    "chk_expires_after_starts"
            );

            // 5. Verify sequences
            List<String> sequences = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = 'public'")) {
                while (rs.next()) {
                    sequences.add(rs.getString(1));
                }
            }
            assertThat(sequences).contains(
                    "resource_id_seq",
                    "physical_item_id_seq",
                    "digital_item_id_seq"
            );
        }
    }

    @Test
    void existingNonFlywayDatabaseFailsWithoutExplicitBaselineAndSucceedsWithBaseline() throws Exception {
        assumeTrue(postgresAvailable, "PostgreSQL not reachable at " + PG_HOST);

        // Pre-create full V1 schema to simulate an existing pre-Flyway V1 database
        try (Connection conn = DriverManager.getConnection(BASELINE_DB_URL, PG_USER, PG_PASSWORD);
             Statement stmt = conn.createStatement()) {
            String v1Sql = new String(getClass().getResourceAsStream("/db/migration/V1__initial_schema.sql").readAllBytes());
            stmt.execute(v1Sql);
        }

        // 1. With baselineOnMigrate=false (our default), migrating an un-baselined existing database MUST fail
        Flyway strictFlyway = Flyway.configure()
                .dataSource(BASELINE_DB_URL, PG_USER, PG_PASSWORD)
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .load();

        assertThatThrownBy(strictFlyway::migrate)
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("Found non-empty schema(s)");

        // 2. Explicit baseline operation marks the existing database at V1 without re-running V1
        Flyway baseliningFlyway = Flyway.configure()
                .dataSource(BASELINE_DB_URL, PG_USER, PG_PASSWORD)
                .locations("classpath:db/migration")
                .baselineVersion("1")
                .baselineDescription("Baseline pre-Flyway V1 schema")
                .load();

        baseliningFlyway.baseline();

        // 3. Subsequent migrate succeeds and applies V2 (since baseline is at V1)
        MigrateResult afterBaseline = baseliningFlyway.migrate();
        assertThat(afterBaseline.success).isTrue();
        assertThat(afterBaseline.migrationsExecuted).isEqualTo(1);
        assertThat(afterBaseline.targetSchemaVersion).isEqualTo("2");
    }
}
