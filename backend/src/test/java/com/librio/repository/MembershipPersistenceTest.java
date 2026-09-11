package com.librio.repository;

import com.librio.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
public class MembershipPersistenceTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MembershipPlanRepository membershipPlanRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private MembershipSubscriptionRepository membershipSubscriptionRepository;

    @Test
    void shouldBootSchemaAndContainSeededPlans() {
        var monthly = membershipPlanRepository.findByCode("MONTHLY");
        var yearly = membershipPlanRepository.findByCode("YEARLY");

        assertThat(monthly).isPresent();
        assertThat(monthly.get().getPriceAmount()).isEqualByComparingTo("10.00");
        
        assertThat(yearly).isPresent();
        assertThat(yearly.get().getPriceAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void defaultMembershipEligibleIsFalse() {
        Account account = Account.builder()
                .email("test.eligible@librio.local")
                .passwordHash("hash")
                .role(AccountRole.READER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        account = accountRepository.save(account);

        assertThat(account.isMembershipEligible()).isFalse();
    }

    @Test
    void planCodeMustBeUnique() {
        MembershipPlan plan1 = MembershipPlan.builder()
                .code("UNIQUE_CODE")
                .name("Plan 1")
                .durationMonths(1)
                .priceAmount(BigDecimal.TEN)
                .currency("USD")
                .active(true)
                .build();
        membershipPlanRepository.saveAndFlush(plan1);

        MembershipPlan plan2 = MembershipPlan.builder()
                .code("UNIQUE_CODE")
                .name("Plan 2")
                .durationMonths(2)
                .priceAmount(BigDecimal.ONE)
                .currency("USD")
                .active(true)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> membershipPlanRepository.saveAndFlush(plan2));
    }

    @Test
    void oneSubscriptionPerPayment() {
        Account account = Account.builder()
                .email("test.sub1@librio.local")
                .passwordHash("hash")
                .role(AccountRole.READER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        account = accountRepository.saveAndFlush(account);

        MembershipPlan plan = membershipPlanRepository.findByCode("MONTHLY").get();

        PaymentTransaction payment = PaymentTransaction.builder()
                .account(account)
                .plan(plan)
                .amount(plan.getPriceAmount())
                .currency(plan.getCurrency())
                .status(PaymentStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
        payment = paymentTransactionRepository.saveAndFlush(payment);

        MembershipSubscription sub1 = MembershipSubscription.builder()
                .account(account)
                .plan(plan)
                .paymentTransaction(payment)
                .startsAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMonths(1))
                .build();
        membershipSubscriptionRepository.saveAndFlush(sub1);

        MembershipSubscription sub2 = MembershipSubscription.builder()
                .account(account)
                .plan(plan)
                .paymentTransaction(payment) // same payment
                .startsAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMonths(1))
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> membershipSubscriptionRepository.saveAndFlush(sub2));
    }

    @Test
    void expiresAtMustBeAfterStartsAt() {
        Account account = Account.builder()
                .email("test.sub2@librio.local")
                .passwordHash("hash")
                .role(AccountRole.READER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        account = accountRepository.saveAndFlush(account);

        MembershipPlan plan = membershipPlanRepository.findByCode("MONTHLY").get();

        PaymentTransaction payment = PaymentTransaction.builder()
                .account(account)
                .plan(plan)
                .amount(plan.getPriceAmount())
                .currency(plan.getCurrency())
                .status(PaymentStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
        payment = paymentTransactionRepository.saveAndFlush(payment);

        MembershipSubscription sub = MembershipSubscription.builder()
                .account(account)
                .plan(plan)
                .paymentTransaction(payment)
                .startsAt(LocalDateTime.now().plusDays(1))
                .expiresAt(LocalDateTime.now()) // Expires before starts
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> membershipSubscriptionRepository.saveAndFlush(sub));
    }
}
