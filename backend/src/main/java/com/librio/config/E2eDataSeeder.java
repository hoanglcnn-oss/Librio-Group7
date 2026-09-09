package com.librio.config;

import com.librio.domain.Account;
import com.librio.domain.AccountRole;
import com.librio.domain.AccountStatus;
import com.librio.domain.BorrowRequest;
import com.librio.domain.BorrowRequestStatus;
import com.librio.domain.Borrowing;
import com.librio.domain.DigitalItem;
import com.librio.domain.PhysicalItem;
import com.librio.domain.CirculationStatus;
import com.librio.domain.InventoryStatus;
import com.librio.domain.Resource;
import com.librio.repository.AccountRepository;
import com.librio.repository.BorrowRequestRepository;
import com.librio.repository.BorrowingRepository;
import com.librio.repository.DigitalItemRepository;
import com.librio.repository.PhysicalItemRepository;
import com.librio.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Profile("e2e")
@RequiredArgsConstructor
public class E2eDataSeeder implements CommandLineRunner {

    public static final String READER_A_EMAIL = "e2e.reader.a@librio.local";
    public static final String READER_B_EMAIL = "e2e.reader.b@librio.local";
    public static final String LIBRARIAN_EMAIL = "e2e.librarian@librio.local";
    public static final String PASSWORD = "librio-e2e-password";

    private final JdbcTemplate jdbcTemplate;
    private final AccountRepository accountRepository;
    private final ResourceRepository resourceRepository;
    private final PhysicalItemRepository physicalItemRepository;
    private final DigitalItemRepository digitalItemRepository;
    private final BorrowRequestRepository borrowRequestRepository;
    private final BorrowingRepository borrowingRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        String jdbcUrl = jdbcTemplate.execute((ConnectionCallback<String>) connection ->
                connection.getMetaData().getURL());
        if (jdbcUrl == null || !jdbcUrl.contains("librio-e2e")) {
            throw new IllegalStateException("E2E seed chỉ được chạy trên dedicated H2 librio-e2e database");
        }

        reset();

        LocalDateTime now = LocalDateTime.now();
        Account readerA = account(READER_A_EMAIL, "E2E Reader A", AccountRole.READER, now);
        Account readerB = account(READER_B_EMAIL, "E2E Reader B", AccountRole.READER, now);
        Account librarian = account(LIBRARIAN_EMAIL, "E2E Librarian", AccountRole.LIBRARIAN, now);

        Resource digital = resource(9101L, "E2E Digital Systems Handbook",
                "E2E Digital Author", "Protected digital resource for E2E.");
        digitalItem(9101L, digital);

        Resource printOnly = resource(9102L, "E2E Print Only Reference",
                "E2E Print Author", "Physical-only resource for E2E.");
        physicalItem(91021L, printOnly, CirculationStatus.AVAILABLE);

        Resource overdue = resource(9103L, "E2E Overdue Loan",
                "E2E Circulation Author", "Borrowing with past due date.");
        PhysicalItem overdueItem = physicalItem(91031L, overdue, CirculationStatus.BORROWED);
        fulfilledBorrowing(readerA, librarian, overdue, overdueItem, now.minusDays(21), now.minusDays(7));

        Resource current = resource(9104L, "E2E Current Loan",
                "E2E Circulation Author", "Borrowing that is not overdue.");
        PhysicalItem currentItem = physicalItem(91041L, current, CirculationStatus.BORROWED);
        fulfilledBorrowing(readerA, librarian, current, currentItem, now.minusDays(1), now.plusDays(13));

        Resource privateLoan = resource(9105L, "E2E Reader B Private Loan",
                "E2E Circulation Author", "Borrowing owned by another reader.");
        PhysicalItem privateItem = physicalItem(91051L, privateLoan, CirculationStatus.BORROWED);
        fulfilledBorrowing(readerB, librarian, privateLoan, privateItem, now.minusDays(2), now.plusDays(12));

        Resource returnJourney = resource(9106L, "E2E Return Journey Book",
                "E2E Flow Author", "Available copy used for request prepare fulfil return E2E.");
        physicalItem(91061L, returnJourney, CirculationStatus.AVAILABLE);

        Resource adminSafety = resource(9107L, "E2E Admin Safety Book",
                "E2E Admin Author", "Resource with an active reserved copy for reduction safety.");
        physicalItem(91071L, adminSafety, CirculationStatus.AVAILABLE);
        PhysicalItem reserved = physicalItem(91072L, adminSafety, CirculationStatus.RESERVED);
        activeRequest(readerB, adminSafety, reserved, now);
        digitalItem(9107L, adminSafety);
    }

    private void reset() {
        borrowingRepository.deleteAll();
        borrowRequestRepository.deleteAll();
        digitalItemRepository.deleteAll();
        physicalItemRepository.deleteAll();
        resourceRepository.deleteAll();
        accountRepository.deleteAll();
    }

    private Account account(String email, String displayName, AccountRole role, LocalDateTime now) {
        return accountRepository.save(Account.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .displayName(displayName)
                .role(role)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private Resource resource(Long id, String title, String authors, String description) {
        return resourceRepository.save(Resource.builder()
                .id(id)
                .title(title)
                .authors(authors)
                .description(description)
                .category("E2E")
                .build());
    }

    private PhysicalItem physicalItem(Long id, Resource resource, CirculationStatus circulationStatus) {
        return physicalItemRepository.save(PhysicalItem.builder()
                .id(id)
                .resource(resource)
                .barcode("LIB-" + id)
                .location("UNASSIGNED")
                .inventoryStatus(InventoryStatus.ACTIVE)
                .circulationStatus(circulationStatus)
                .build());
    }

    private void digitalItem(Long id, Resource resource) {
        digitalItemRepository.save(DigitalItem.builder()
                .id(id)
                .resource(resource)
                .build());
    }

    private BorrowRequest activeRequest(Account reader, Resource resource, PhysicalItem item, LocalDateTime now) {
        return borrowRequestRepository.save(BorrowRequest.builder()
                .reader(reader)
                .resource(resource)
                .physicalItem(item)
                .status(BorrowRequestStatus.REQUESTED)
                .requestedAt(now.minusHours(1))
                .statusUpdatedAt(now.minusHours(1))
                .expiresAt(now.plusDays(1))
                .createdAt(now.minusHours(1))
                .updatedAt(now.minusHours(1))
                .build());
    }

    private void fulfilledBorrowing(Account reader, Account librarian, Resource resource, PhysicalItem item,
                                    LocalDateTime borrowedAt, LocalDateTime dueAt) {
        BorrowRequest request = borrowRequestRepository.save(BorrowRequest.builder()
                .reader(reader)
                .resource(resource)
                .physicalItem(item)
                .status(BorrowRequestStatus.FULFILLED)
                .requestedAt(borrowedAt.minusDays(1))
                .statusUpdatedAt(borrowedAt)
                .expiresAt(borrowedAt.plusDays(3))
                .preparedAt(borrowedAt.minusHours(2))
                .preparedBy(librarian)
                .fulfilledAt(borrowedAt)
                .fulfilledBy(librarian)
                .createdAt(borrowedAt.minusDays(1))
                .updatedAt(borrowedAt)
                .build());
        borrowingRepository.save(Borrowing.builder()
                .reader(reader)
                .physicalItem(item)
                .borrowRequest(request)
                .borrowedAt(borrowedAt)
                .dueAt(dueAt)
                .build());
    }
}
