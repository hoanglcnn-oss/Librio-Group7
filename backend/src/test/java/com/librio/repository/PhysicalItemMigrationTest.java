package com.librio.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PhysicalItemMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Verify migration logic works on legacy-shaped table with status column")
    void testLegacyDatabaseMigrationScript() {
        jdbcTemplate.execute("CREATE TABLE legacy_physical_item (" +
                "id BIGINT PRIMARY KEY, " +
                "resource_id BIGINT NOT NULL, " +
                "status VARCHAR(32) NOT NULL" +
                ")");

        jdbcTemplate.execute("INSERT INTO legacy_physical_item (id, resource_id, status) VALUES " +
                "(10001, 1, 'AVAILABLE'), " +
                "(10002, 1, 'RESERVED'), " +
                "(10003, 1, 'BORROWED'), " +
                "(10004, 1, 'OVERDUE')");

        // Run migration sequence
        jdbcTemplate.execute("ALTER TABLE legacy_physical_item ADD COLUMN IF NOT EXISTS status VARCHAR(32)");
        jdbcTemplate.execute("ALTER TABLE legacy_physical_item ADD COLUMN IF NOT EXISTS barcode VARCHAR(255)");
        jdbcTemplate.execute("ALTER TABLE legacy_physical_item ADD COLUMN IF NOT EXISTS location VARCHAR(255)");
        jdbcTemplate.execute("ALTER TABLE legacy_physical_item ADD COLUMN IF NOT EXISTS inventory_status VARCHAR(32)");
        jdbcTemplate.execute("ALTER TABLE legacy_physical_item ADD COLUMN IF NOT EXISTS circulation_status VARCHAR(32)");

        jdbcTemplate.execute("UPDATE legacy_physical_item SET inventory_status = 'ACTIVE' WHERE inventory_status IS NULL");
        jdbcTemplate.execute("UPDATE legacy_physical_item SET circulation_status = CASE " +
                "WHEN status = 'AVAILABLE' THEN 'AVAILABLE' " +
                "WHEN status = 'RESERVED' THEN 'RESERVED' " +
                "WHEN status IN ('BORROWED', 'OVERDUE') THEN 'BORROWED' " +
                "ELSE 'AVAILABLE' END WHERE circulation_status IS NULL AND status IS NOT NULL");
        jdbcTemplate.execute("UPDATE legacy_physical_item SET circulation_status = 'AVAILABLE' WHERE circulation_status IS NULL");
        jdbcTemplate.execute("UPDATE legacy_physical_item SET barcode = CONCAT('LIB-', id) WHERE barcode IS NULL");
        jdbcTemplate.execute("UPDATE legacy_physical_item SET location = 'UNASSIGNED' WHERE location IS NULL");

        jdbcTemplate.execute("ALTER TABLE legacy_physical_item DROP COLUMN IF EXISTS status");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, barcode, location, inventory_status, circulation_status FROM legacy_physical_item ORDER BY id ASC");

        assertThat(rows).hasSize(4);

        assertThat(rows.get(0)).containsEntry("BARCODE", "LIB-10001")
                .containsEntry("LOCATION", "UNASSIGNED")
                .containsEntry("INVENTORY_STATUS", "ACTIVE")
                .containsEntry("CIRCULATION_STATUS", "AVAILABLE");

        assertThat(rows.get(1)).containsEntry("BARCODE", "LIB-10002")
                .containsEntry("LOCATION", "UNASSIGNED")
                .containsEntry("INVENTORY_STATUS", "ACTIVE")
                .containsEntry("CIRCULATION_STATUS", "RESERVED");

        assertThat(rows.get(2)).containsEntry("BARCODE", "LIB-10003")
                .containsEntry("LOCATION", "UNASSIGNED")
                .containsEntry("INVENTORY_STATUS", "ACTIVE")
                .containsEntry("CIRCULATION_STATUS", "BORROWED");

        assertThat(rows.get(3)).containsEntry("BARCODE", "LIB-10004")
                .containsEntry("LOCATION", "UNASSIGNED")
                .containsEntry("INVENTORY_STATUS", "ACTIVE")
                .containsEntry("CIRCULATION_STATUS", "BORROWED");
    }
}

