package com.librio.service;

import com.librio.domain.*;
import com.librio.exception.BorrowErrorCode;
import com.librio.exception.BorrowFlowException;
import com.librio.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class BorrowingQuotaConcurrencyTest {

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private PhysicalItemRepository physicalItemRepository;

    @Autowired
    private MembershipPlanRepository planRepository;

    @Autowired
    private MembershipSubscriptionRepository subscriptionRepository;

    @Autowired
    private BorrowRequestRepository borrowRequestRepository;

    private Account reader;
    private Resource resource1;
    private Resource resource2;
    private PhysicalItem item1;
    private PhysicalItem item2;
    private MembershipPlan plan;

    @BeforeEach
    void setUp() {
        cleanUp();
        reader = accountRepository.save(Account.builder()
                .email("quota_concurrent_reader@example.com")
                .passwordHash("hash")
                .role(AccountRole.READER)
                .accountStatus(AccountStatus.ACTIVE)
                .build());

        plan = planRepository.save(MembershipPlan.builder()
                .name("Quota Plan")
                .durationMonths(1)
                .priceAmount(BigDecimal.TEN)
                .currency("USD")
                .monthlyBorrowQuota(1)
                .isActive(true)
                .build());

        subscriptionRepository.save(MembershipSubscription.builder()
                .account(reader)
                .plan(plan)
                .startsAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusMonths(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        resource1 = resourceRepository.save(Resource.builder()
                .title("Resource 1")
                .author("Author 1")
                .accessType(AccessType.PHYSICAL_ONLY)
                .build());

        resource2 = resourceRepository.save(Resource.builder()
                .title("Resource 2")
                .author("Author 2")
                .accessType(AccessType.PHYSICAL_ONLY)
                .build());

        item1 = physicalItemRepository.save(PhysicalItem.builder()
                .resource(resource1)
                .barcode("BC-QUOTA-1")
                .inventoryStatus(InventoryStatus.ACTIVE)
                .circulationStatus(CirculationStatus.AVAILABLE)
                .build());

        item2 = physicalItemRepository.save(PhysicalItem.builder()
                .resource(resource2)
                .barcode("BC-QUOTA-2")
                .inventoryStatus(InventoryStatus.ACTIVE)
                .circulationStatus(CirculationStatus.AVAILABLE)
                .build());
    }

    @AfterEach
    void cleanUp() {
        borrowRequestRepository.deleteAll();
        subscriptionRepository.deleteAll();
        physicalItemRepository.deleteAll();
        resourceRepository.deleteAll();
        planRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void testSameReaderQuotaConcurrency() throws InterruptedException {
        // Reader has quota = 1.
        // Two concurrent threads try to borrow resource1 and resource2.
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger quotaExceededCount = new AtomicInteger(0);

        Runnable task1 = () -> {
            try {
                startLatch.await();
                borrowService.createRequest(reader.getId(), resource1.getId());
                successCount.incrementAndGet();
            } catch (BorrowFlowException e) {
                if (BorrowErrorCode.BORROW_QUOTA_EXCEEDED.name().equals(e.getErrorCode())) {
                    quotaExceededCount.incrementAndGet();
                }
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        };

        Runnable task2 = () -> {
            try {
                startLatch.await();
                borrowService.createRequest(reader.getId(), resource2.getId());
                successCount.incrementAndGet();
            } catch (BorrowFlowException e) {
                if (BorrowErrorCode.BORROW_QUOTA_EXCEEDED.name().equals(e.getErrorCode())) {
                    quotaExceededCount.incrementAndGet();
                }
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        };

        executor.submit(task1);
        executor.submit(task2);

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(quotaExceededCount.get()).isEqualTo(1);
        
        long activeRequests = borrowRequestRepository.countByReaderIdAndStatusIn(
                reader.getId(), List.of(BorrowRequestStatus.REQUESTED));
        assertThat(activeRequests).isEqualTo(1);
    }
}