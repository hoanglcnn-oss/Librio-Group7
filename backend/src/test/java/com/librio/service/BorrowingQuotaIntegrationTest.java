package com.librio.service;

import com.librio.domain.*;
import com.librio.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class BorrowingQuotaIntegrationTest {

    @Autowired
    private BorrowingQuotaPolicy policy;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MembershipPlanRepository planRepository;

    @Autowired
    private MembershipSubscriptionRepository subscriptionRepository;

    @Autowired
    private BorrowingRepository borrowingRepository;

    @Autowired
    private BorrowRequestRepository borrowRequestRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private PhysicalItemRepository physicalItemRepository;

    private Account reader;
    private MembershipPlan plan;
    private Resource resource;
    private PhysicalItem item;

    @BeforeEach
    void setUp() {
        cleanUp();
        reader = accountRepository.save(Account.builder()
                .email("quota_integ_reader@example.com")
                .passwordHash("hash")
                .role(AccountRole.READER)
                .accountStatus(AccountStatus.ACTIVE)
                .build());

        plan = planRepository.save(MembershipPlan.builder()
                .name("Integ Plan")
                .durationMonths(1)
                .priceAmount(BigDecimal.TEN)
                .currency("USD")
                .monthlyBorrowQuota(5)
                .isActive(true)
                .build());

        resource = resourceRepository.save(Resource.builder()
                .title("Resource")
                .author("Author")
                .accessType(AccessType.PHYSICAL_ONLY)
                .build());

        item = physicalItemRepository.save(PhysicalItem.builder()
                .resource(resource)
                .barcode("BC-INTEG-1")
                .inventoryStatus(InventoryStatus.ACTIVE)
                .circulationStatus(CirculationStatus.AVAILABLE)
                .build());
    }

    @AfterEach
    void cleanUp() {
        borrowingRepository.deleteAll();
        borrowRequestRepository.deleteAll();
        subscriptionRepository.deleteAll();
        physicalItemRepository.deleteAll();
        resourceRepository.deleteAll();
        planRepository.deleteAll();
        accountRepository.deleteAll();
    }

    private MembershipSubscription createSubscription(LocalDateTime startsAt) {
        return subscriptionRepository.save(MembershipSubscription.builder()
                .account(reader)
                .plan(plan)
                .startsAt(startsAt)
                .expiresAt(startsAt.plusMonths(12))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private void createBorrowing(LocalDateTime borrowedAt, LocalDateTime returnedAt) {
        Borrowing borrowing = Borrowing.builder()
                .reader(reader)
                .physicalItem(item)
                .borrowedAt(borrowedAt)
                .returnedAt(returnedAt)
                .status(returnedAt != null ? BorrowingStatus.RETURNED : BorrowingStatus.BORROWED)
                .dueDate(borrowedAt.plusDays(14))
                .build();
        borrowingRepository.save(borrowing);
    }

    private void createBorrowRequest(BorrowRequestStatus status) {
        BorrowRequest req = BorrowRequest.builder()
                .reader(reader)
                .resource(resource)
                .physicalItem(item)
                .status(status)
                .requestedAt(LocalDateTime.now())
                .statusUpdatedAt(LocalDateTime.now())
                .build();
        borrowRequestRepository.save(req);
    }

    @Test
    @DisplayName("A. active membership, no usage -> remaining = planQuota")
    void testActiveNoUsage() {
        createSubscription(LocalDateTime.now().minusDays(10));
        BorrowingQuotaPolicy.QuotaSnapshot snapshot = policy.getQuotaSnapshot(reader.getId(), LocalDateTime.now());
        assertThat(snapshot.isActiveMembership()).isTrue();
        assertThat(snapshot.getRemainingQuota()).isEqualTo(5);
    }

    @Test
    @DisplayName("B. successful borrowing within current cycle -> usedBorrowings increments")
    void testBorrowingInCycle() {
        createSubscription(LocalDateTime.now().minusDays(10));
        createBorrowing(LocalDateTime.now().minusDays(5), null);
        BorrowingQuotaPolicy.QuotaSnapshot snapshot = policy.getQuotaSnapshot(reader.getId(), LocalDateTime.now());
        assertThat(snapshot.getUsedBorrowings()).isEqualTo(1);
        assertThat(snapshot.getRemainingQuota()).isEqualTo(4);
    }

    @Test
    @DisplayName("C. returned borrowing within current cycle -> still counts toward usedBorrowings")
    void testReturnedBorrowingInCycle() {
        createSubscription(LocalDateTime.now().minusDays(10));
        createBorrowing(LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(1));
        BorrowingQuotaPolicy.QuotaSnapshot snapshot = policy.getQuotaSnapshot(reader.getId(), LocalDateTime.now());
        assertThat(snapshot.getUsedBorrowings()).isEqualTo(1);
        assertThat(snapshot.getRemainingQuota()).isEqualTo(4);
    }

    @Test
    @DisplayName("D. borrowing before current cycle -> does not count")
    void testBorrowingBeforeCycle() {
        createSubscription(LocalDateTime.now().minusMonths(2));
        createBorrowing(LocalDateTime.now().minusMonths(1).minusDays(5), null); // previous cycle
        BorrowingQuotaPolicy.QuotaSnapshot snapshot = policy.getQuotaSnapshot(reader.getId(), LocalDateTime.now());
        assertThat(snapshot.getUsedBorrowings()).isEqualTo(0);
        assertThat(snapshot.getRemainingQuota()).isEqualTo(5);
    }

    @Test
    @DisplayName("E & F & G. Request statuses -> active commitments")
    void testRequestStatuses() {
        createSubscription(LocalDateTime.now().minusDays(10));
        createBorrowRequest(BorrowRequestStatus.REQUESTED);
        createBorrowRequest(BorrowRequestStatus.READY_FOR_PICKUP);
        createBorrowRequest(BorrowRequestStatus.CANCELLED);
        createBorrowRequest(BorrowRequestStatus.EXPIRED);
        createBorrowRequest(BorrowRequestStatus.REJECTED);

        BorrowingQuotaPolicy.QuotaSnapshot snapshot = policy.getQuotaSnapshot(reader.getId(), LocalDateTime.now());
        assertThat(snapshot.getActiveCommitments()).isEqualTo(2);
        assertThat(snapshot.getRemainingQuota()).isEqualTo(3);
    }

    @Test
    @DisplayName("H. mixed: planQuota 5, used 2, active 1 -> remaining 2")
    void testMixed() {
        createSubscription(LocalDateTime.now().minusDays(10));
        createBorrowing(LocalDateTime.now().minusDays(5), null);
        createBorrowing(LocalDateTime.now().minusDays(4), LocalDateTime.now().minusDays(2));
        createBorrowRequest(BorrowRequestStatus.REQUESTED);

        BorrowingQuotaPolicy.QuotaSnapshot snapshot = policy.getQuotaSnapshot(reader.getId(), LocalDateTime.now());
        assertThat(snapshot.getUsedBorrowings()).isEqualTo(2);
        assertThat(snapshot.getActiveCommitments()).isEqualTo(1);
        assertThat(snapshot.getRemainingQuota()).isEqualTo(2);
    }

    @Test
    @DisplayName("I. over-consumed/inconsistent persisted state -> remaining clamps to 0")
    void testOverConsumed() {
        createSubscription(LocalDateTime.now().minusDays(10));
        for (int i = 0; i < 6; i++) {
            createBorrowRequest(BorrowRequestStatus.REQUESTED);
        }

        BorrowingQuotaPolicy.QuotaSnapshot snapshot = policy.getQuotaSnapshot(reader.getId(), LocalDateTime.now());
        assertThat(snapshot.getActiveCommitments()).isEqualTo(6);
        assertThat(snapshot.getRemainingQuota()).isEqualTo(0);
    }

    @Test
    @DisplayName("J. invalid/null monthlyBorrowQuota -> fails safely")
    void testNullQuota() {
        plan.setMonthlyBorrowQuota(null);
        planRepository.save(plan);
        createSubscription(LocalDateTime.now().minusDays(10));
        
        BorrowingQuotaPolicy.QuotaSnapshot snapshot = policy.getQuotaSnapshot(reader.getId(), LocalDateTime.now());
        assertThat(snapshot.isActiveMembership()).isTrue();
        assertThat(snapshot.isValidPlanQuota()).isFalse();
    }
}