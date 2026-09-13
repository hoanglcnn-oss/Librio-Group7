package com.librio.service;

import com.librio.domain.Account;
import com.librio.domain.AccountRole;
import com.librio.domain.AccountStatus;
import com.librio.domain.MembershipPlan;
import com.librio.dto.MembershipPaymentRequestDto;
import com.librio.exception.BorrowFlowException;
import com.librio.repository.AccountRepository;
import com.librio.repository.MembershipPlanRepository;
import com.librio.repository.MembershipSubscriptionRepository;
import com.librio.repository.PaymentTransactionRepository;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class MembershipConcurrencyTest {

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MembershipPlanRepository membershipPlanRepository;

    @Autowired
    private MembershipSubscriptionRepository membershipSubscriptionRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    private Account eligibleReader;
    private MembershipPlan activePlan;

    @BeforeEach
    void setUp() {
        membershipSubscriptionRepository.deleteAll();
        paymentTransactionRepository.deleteAll();
        membershipPlanRepository.deleteAll();

        LocalDateTime now = LocalDateTime.now();
        eligibleReader = accountRepository.save(Account.builder()
                .email("concurrent.reader@test.local")
                .passwordHash("hashed-password")
                .displayName("Concurrent Reader")
                .role(AccountRole.READER)
                .accountStatus(AccountStatus.ACTIVE)
                .membershipEligible(true)
                .createdAt(now)
                .updatedAt(now)
                .build());

        activePlan = membershipPlanRepository.save(MembershipPlan.builder()
                .code("CONCURRENT_PLAN")
                .name("Concurrent Plan")
                .durationMonths(1)
                .priceAmount(new BigDecimal("9.99"))
                .currency("USD")
                .monthlyBorrowQuota(5)
                .active(true)
                .build());
    }

    @AfterEach
    void tearDown() {
        membershipSubscriptionRepository.deleteAll();
        paymentTransactionRepository.deleteAll();
        membershipPlanRepository.deleteAll();
    }

    @Test
    void competingActivation_CreatesExactlyOneSubscription() throws InterruptedException {
        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicReference<Exception> unexpectedException = new AtomicReference<>();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await(); // wait for both threads to be ready
                    
                    MembershipPaymentRequestDto request = new MembershipPaymentRequestDto();
                    request.setPlanId(activePlan.getId());
                    request.setOutcome("SUCCESS");
                    
                    // Uses @Transactional in service layer, independent physical connections
                    membershipService.processPayment(eligibleReader.getEmail(), request);
                    successCount.incrementAndGet();
                } catch (BorrowFlowException e) {
                    if ("ACTIVE_MEMBERSHIP_EXISTS".equals(e.getCode())) {
                        conflictCount.incrementAndGet();
                    } else {
                        unexpectedException.set(e);
                    }
                } catch (Exception e) {
                    unexpectedException.set(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release both threads simultaneously
        startLatch.countDown();
        
        // Wait for both threads to finish
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        executorService.shutdown();

        // Assert no unexpected exceptions
        assertThat(unexpectedException.get()).isNull();

        // One succeeds, one fails with ACTIVE_MEMBERSHIP_EXISTS
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);

        // Assert persisted final state
        long subscriptionCount = membershipSubscriptionRepository.count();
        long paymentCount = paymentTransactionRepository.count();

        assertThat(subscriptionCount).isEqualTo(1);
        // We only persist the successful transaction because the failed one throws an exception
        assertThat(paymentCount).isEqualTo(1);
    }
}
