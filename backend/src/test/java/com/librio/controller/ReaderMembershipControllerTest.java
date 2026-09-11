package com.librio.controller;

import com.librio.domain.Account;
import com.librio.domain.AccountRole;
import com.librio.domain.AccountStatus;
import com.librio.domain.MembershipPlan;
import com.librio.domain.MembershipSubscription;
import com.librio.domain.PaymentStatus;
import com.librio.domain.PaymentTransaction;
import com.librio.repository.AccountRepository;
import com.librio.repository.MembershipPlanRepository;
import com.librio.repository.MembershipSubscriptionRepository;
import com.librio.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReaderMembershipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SpyBean
    private AccountRepository accountRepository;

    @Autowired
    private MembershipPlanRepository membershipPlanRepository;

    @Autowired
    private MembershipSubscriptionRepository membershipSubscriptionRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    private Account eligibleReader;
    private Account ineligibleReader;
    private MembershipPlan activePlan;
    private MembershipPlan inactivePlan;

    @BeforeEach
    void setUp() {
        membershipSubscriptionRepository.deleteAll();
        paymentTransactionRepository.deleteAll();
        membershipPlanRepository.deleteAll();

        eligibleReader = createAccount("eligible.reader@test.local", true);
        ineligibleReader = createAccount("ineligible.reader@test.local", false);

        activePlan = membershipPlanRepository.save(MembershipPlan.builder()
                .code("STANDARD_1M")
                .name("Standard Monthly")
                .durationMonths(1)
                .priceAmount(new BigDecimal("9.99"))
                .currency("USD")
                .monthlyBorrowQuota(5)
                .active(true)
                .build());

        inactivePlan = membershipPlanRepository.save(MembershipPlan.builder()
                .code("LEGACY_PLAN")
                .name("Legacy Plan")
                .durationMonths(12)
                .priceAmount(new BigDecimal("99.99"))
                .currency("USD")
                .monthlyBorrowQuota(50)
                .active(false)
                .build());
    }

    @Test
    @DisplayName("GET /membership/plans is public and returns active plans only")
    void testGetPublicActivePlans() throws Exception {
        mockMvc.perform(get("/membership/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("STANDARD_1M"));
    }

    @Test
    @DisplayName("GET /me/membership returns NONE when no subscription exists")
    @WithMockUser(username = "eligible.reader@test.local", roles = "READER")
    void testGetCurrentMembership_None() throws Exception {
        mockMvc.perform(get("/me/membership"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NONE"))
                .andExpect(jsonPath("$.membershipEligible").value(true));
    }

    @Test
    @DisplayName("GET /me/membership returns ACTIVE when active subscription exists")
    @WithMockUser(username = "eligible.reader@test.local", roles = "READER")
    void testGetCurrentMembership_Active() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        createSubscription(eligibleReader, activePlan, now.minusDays(5), now.plusDays(25));

        mockMvc.perform(get("/me/membership"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.plan.code").value("STANDARD_1M"));
    }

    @Test
    @DisplayName("GET /me/membership returns EXPIRED when subscription has expired")
    @WithMockUser(username = "eligible.reader@test.local", roles = "READER")
    void testGetCurrentMembership_Expired() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        createSubscription(eligibleReader, activePlan, now.minusDays(40), now.minusDays(10));

        mockMvc.perform(get("/me/membership"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"))
                .andExpect(jsonPath("$.plan.code").value("STANDARD_1M"));
    }

    @Test
    @DisplayName("POST /me/membership-payments with ineligible reader returns 403 MEMBERSHIP_NOT_ELIGIBLE")
    @WithMockUser(username = "ineligible.reader@test.local", roles = "READER")
    void testPayment_IneligibleReader_Returns403() throws Exception {
        mockMvc.perform(post("/me/membership-payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planId": %d,
                                  "outcome": "SUCCESS"
                                }
                                """.formatted(activePlan.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEMBERSHIP_NOT_ELIGIBLE"));
    }

    @Test
    @DisplayName("POST /me/membership-payments with inactive or missing plan returns 404 MEMBERSHIP_PLAN_NOT_FOUND")
    @WithMockUser(username = "eligible.reader@test.local", roles = "READER")
    void testPayment_MissingOrInactivePlan_Returns404() throws Exception {
        // Missing plan
        mockMvc.perform(post("/me/membership-payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planId": 99999,
                                  "outcome": "SUCCESS"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBERSHIP_PLAN_NOT_FOUND"));

        // Inactive plan
        mockMvc.perform(post("/me/membership-payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planId": %d,
                                  "outcome": "SUCCESS"
                                }
                                """.formatted(inactivePlan.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBERSHIP_PLAN_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /me/membership-payments with invalid outcome returns 400 INVALID_PAYMENT_OUTCOME")
    @WithMockUser(username = "eligible.reader@test.local", roles = "READER")
    void testPayment_InvalidOutcome_Returns400() throws Exception {
        mockMvc.perform(post("/me/membership-payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planId": %d,
                                  "outcome": "INVALID_OUTCOME"
                                }
                                """.formatted(activePlan.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAYMENT_OUTCOME"));
    }

    @Test
    @DisplayName("POST /me/membership-payments FAILED outcome persists payment transaction only (no subscription)")
    @WithMockUser(username = "eligible.reader@test.local", roles = "READER")
    void testPayment_FailedOutcome() throws Exception {
        mockMvc.perform(post("/me/membership-payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planId": %d,
                                  "outcome": "FAILED"
                                }
                                """.formatted(activePlan.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NONE"))
                .andExpect(jsonPath("$.latestPayment.status").value("FAILED"));

        assertEquals(1, paymentTransactionRepository.count());
        assertEquals(0, membershipSubscriptionRepository.count());

        PaymentTransaction tx = paymentTransactionRepository.findAll().get(0);
        assertEquals(PaymentStatus.FAILED, tx.getStatus());
        assertNotNull(tx.getCompletedAt());
    }

    @Test
    @DisplayName("POST /me/membership-payments SUCCESS outcome creates payment transaction and subscription, becoming ACTIVE")
    @WithMockUser(username = "eligible.reader@test.local", roles = "READER")
    void testPayment_SuccessOutcome() throws Exception {
        mockMvc.perform(post("/me/membership-payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planId": %d,
                                  "outcome": "SUCCESS"
                                }
                                """.formatted(activePlan.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.plan.code").value("STANDARD_1M"))
                .andExpect(jsonPath("$.latestPayment.status").value("SUCCESS"));

        assertEquals(1, paymentTransactionRepository.count());
        assertEquals(1, membershipSubscriptionRepository.count());

        verify(accountRepository).findByIdForUpdate(eligibleReader.getId());
    }

    @Test
    @DisplayName("POST /me/membership-payments while already ACTIVE returns 409 ACTIVE_MEMBERSHIP_EXISTS")
    @WithMockUser(username = "eligible.reader@test.local", roles = "READER")
    void testPayment_AlreadyActive_Returns409() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        createSubscription(eligibleReader, activePlan, now.minusDays(1), now.plusDays(29));

        mockMvc.perform(post("/me/membership-payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planId": %d,
                                  "outcome": "SUCCESS"
                                }
                                """.formatted(activePlan.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTIVE_MEMBERSHIP_EXISTS"));
    }

    private Account createAccount(String email, boolean membershipEligible) {
        LocalDateTime now = LocalDateTime.now();
        return accountRepository.save(Account.builder()
                .email(email)
                .passwordHash("hashed-password")
                .displayName(email)
                .role(AccountRole.READER)
                .accountStatus(AccountStatus.ACTIVE)
                .membershipEligible(membershipEligible)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private MembershipSubscription createSubscription(Account account, MembershipPlan plan, LocalDateTime startsAt, LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.now();
        PaymentTransaction tx = paymentTransactionRepository.save(PaymentTransaction.builder()
                .account(account)
                .plan(plan)
                .amount(plan.getPriceAmount())
                .currency(plan.getCurrency())
                .status(PaymentStatus.SUCCESS)
                .createdAt(now)
                .completedAt(now)
                .build());

        return membershipSubscriptionRepository.save(MembershipSubscription.builder()
                .account(account)
                .plan(plan)
                .paymentTransaction(tx)
                .startsAt(startsAt)
                .expiresAt(expiresAt)
                .build());
    }
}
