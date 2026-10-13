package com.librio.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BorrowingQuotaRepositoryTest {

    @Autowired
    private BorrowingRepository borrowingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Usage count boundaries and returned status")
    void testUsageCount() {
        Long readerId = 2L;
        
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("MERGE INTO accounts (id, email, password_hash, display_name, role, reader_type, account_status, created_at, updated_at) " +
                "KEY(id) VALUES (2, 'repo-reader@librio.local', 'hash', 'Repo Reader', 'READER', 'INSTITUTIONAL', 'ACTIVE', ?, ?)", now, now);
        jdbcTemplate.update("MERGE INTO resource (id, title, authors) KEY(id) VALUES (1, 'Title', 'Author')");
        jdbcTemplate.update("MERGE INTO physical_item (id, resource_id, barcode, location, inventory_status, circulation_status) " +
                "KEY(id) VALUES (1000, 1, 'BC-1000', 'A1', 'ACTIVE', 'AVAILABLE')");

        // Ensure clean state for reader 2
        jdbcTemplate.execute("DELETE FROM borrowing WHERE reader_id = 2");
        jdbcTemplate.execute("DELETE FROM borrow_request WHERE reader_id = 2");
        
        jdbcTemplate.update(
            "INSERT INTO borrow_request (id, reader_id, resource_id, status, requested_at, status_updated_at, created_at, updated_at) " +
            "VALUES (9990, 2, 1, 'FULFILLED', '2026-09-01 10:00:00', '2026-09-01 10:00:00', '2026-09-01 10:00:00', '2026-09-01 10:00:00')"
        );
        jdbcTemplate.update(
            "INSERT INTO borrow_request (id, reader_id, resource_id, status, requested_at, status_updated_at, created_at, updated_at) " +
            "VALUES (9991, 2, 1, 'FULFILLED', '2026-09-01 10:00:00', '2026-09-01 10:00:00', '2026-09-01 10:00:00', '2026-09-01 10:00:00')"
        );
        jdbcTemplate.update(
            "INSERT INTO borrow_request (id, reader_id, resource_id, status, requested_at, status_updated_at, created_at, updated_at) " +
            "VALUES (9992, 2, 1, 'FULFILLED', '2026-09-01 10:00:00', '2026-09-01 10:00:00', '2026-09-01 10:00:00', '2026-09-01 10:00:00')"
        );
        jdbcTemplate.update(
            "INSERT INTO borrow_request (id, reader_id, resource_id, status, requested_at, status_updated_at, created_at, updated_at) " +
            "VALUES (9993, 2, 1, 'FULFILLED', '2026-09-01 10:00:00', '2026-09-01 10:00:00', '2026-09-01 10:00:00', '2026-09-01 10:00:00')"
        );
        jdbcTemplate.update(
            "INSERT INTO borrow_request (id, reader_id, resource_id, status, requested_at, status_updated_at, created_at, updated_at) " +
            "VALUES (9994, 2, 1, 'FULFILLED', '2026-09-01 10:00:00', '2026-09-01 10:00:00', '2026-09-01 10:00:00', '2026-09-01 10:00:00')"
        );

        // inside cycle
        jdbcTemplate.update("INSERT INTO borrowing (physical_item_id, reader_id, borrow_request_id, borrowed_at, due_at, returned_at) VALUES (1000, 2, 9990, '2026-09-25 10:00:00', '2026-10-25 10:00:00', null)");
        // before cycle
        jdbcTemplate.update("INSERT INTO borrowing (physical_item_id, reader_id, borrow_request_id, borrowed_at, due_at, returned_at) VALUES (1000, 2, 9991, '2026-09-19 23:59:59', '2026-10-19 23:59:59', null)");
        // exactly at cycleStart
        jdbcTemplate.update("INSERT INTO borrowing (physical_item_id, reader_id, borrow_request_id, borrowed_at, due_at, returned_at) VALUES (1000, 2, 9992, '2026-09-20 10:00:00', '2026-10-20 10:00:00', null)");
        // exactly at cycleEnd
        jdbcTemplate.update("INSERT INTO borrowing (physical_item_id, reader_id, borrow_request_id, borrowed_at, due_at, returned_at) VALUES (1000, 2, 9993, '2026-10-20 10:00:00', '2026-11-20 10:00:00', null)");
        // returned borrowing inside cycle
        jdbcTemplate.update("INSERT INTO borrowing (physical_item_id, reader_id, borrow_request_id, borrowed_at, due_at, returned_at) VALUES (1000, 2, 9994, '2026-09-22 10:00:00', '2026-10-22 10:00:00', '2026-09-23 10:00:00')");

        LocalDateTime start = LocalDateTime.of(2026, 9, 20, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 20, 10, 0);
        
        long count = borrowingRepository.countBorrowingsByReaderIdAndPeriod(2L, start, end);
        
        // Expecting 3: inside cycle, exactly at cycleStart, returned borrowing inside cycle
        assertEquals(3, count);
    }
}