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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
    private PaymentTransactionRepository paymentTransactionRepository;

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
                .email("quota_concurrent_reader" + UUID.randomUUID() + "@example.com")
                .passwordHash("hash")
                .role(AccountRole.READER)
                .accountStatus(AccountStatus.ACTIVE)
                .build());

        plan = planRepository.save(MembershipPlan.builder()
                .code("QUOTA_CONCUR_" + UUID.randomUUID())
                .name("Quota Plan")
                .durationMonths(1)
                .priceAmount(BigDecimal.TEN)
                .currency("USD")
                .monthlyBorrowQuota(1)
                .active(true)
                .build());

        PaymentTransaction tx = paymentTransactionRepository.save(PaymentTransaction.builder()
                .account(reader)
                .plan(plan)
                .amount(plan.getPriceAmount())
                .currency(plan.getCurrency())
                .status(PaymentStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build());

        subscriptionRepository.save(MembershipSubscription.builder()
                .account(reader)
                .plan(plan)
                .paymentTransaction(tx)
                .startsAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusMonths(1))
                .build());

        resource1 = resourceRepository.save(Resource.builder()
                .title("Resource 1")
                .authors("Author 1")
                .build());

        resource2 = resourceRepository.save(Resource.builder()
                .title("Resource 2")
                .authors("Author 2")
                .build());

        item1 = physicalItemRepository.save(PhysicalItem.builder()
                .resource(resource1)
                .barcode("BC-QUOTA-" + UUID.randomUUID())
                .location("A1")
                .inventoryStatus(InventoryStatus.ACTIVE)
                .circulationStatus(CirculationStatus.AVAILABLE)
                .build());

        item2 = physicalItemRepository.save(PhysicalItem.builder()
                .resource(resource2)
                .barcode("BC-QUOTA-" + UUID.randomUUID())
                .location("A1")
                .inventoryStatus(InventoryStatus.ACTIVE)
                .circulationStatus(CirculationStatus.AVAILABLE)
                .build());
    }

    @AfterEach
    void cleanUp() {
        borrowRequestRepository.deleteAll();
        subscriptionRepository.deleteAll();
        paymentTransactionRepository.deleteAll();
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
        AtomicReference<Exception> unexpectedException = new AtomicReference<>();

        Runnable task1 = () -> {
            try {
                startLatch.await();
                borrowService.createRequest(reader.getId(), resource1.getId());
                successCount.incrementAndGet();
            } catch (BorrowFlowException e) {
                if (BorrowErrorCode.BORROW_QUOTA_EXCEEDED.name().equals(e.getErrorCode())) {
                    quotaExceededCount.incrementAndGet();
                } else {
                    unexpectedException.compareAndSet(null, e);
                }
            } catch (Exception e) {
                unexpectedException.compareAndSet(null, e);
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
                } else {
                    unexpectedException.compareAndSet(null, e);
                }
            } catch (Exception e) {
                unexpectedException.compareAndSet(null, e);
            } finally {
                doneLatch.countDown();
            }
        };

        try {
            executor.submit(task1);
            executor.submit(task2);

            startLatch.countDown();
            boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
            assertThat(completed).isTrue();

            assertThat(unexpectedException.get()).isNull();
            assertThat(successCount.get()).isEqualTo(1);
            assertThat(quotaExceededCount.get()).isEqualTo(1);
            
            long activeRequests = borrowRequestRepository.countByReaderIdAndStatusIn(
                    reader.getId(), List.of(BorrowRequestStatus.REQUESTED));
            assertThat(activeRequests).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
}