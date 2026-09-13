package com.librio.controller;

import com.librio.domain.*;
import com.librio.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DigitalAccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private DigitalItemRepository digitalItemRepository;

    @Autowired
    private MembershipSubscriptionRepository membershipSubscriptionRepository;

    @Autowired
    private MembershipPlanRepository membershipPlanRepository;
    
    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    private Resource resource;
    private DigitalItem configuredPreviewItem;
    private DigitalItem configuredFullItem;
    private DigitalItem configuredBothItem;
    private DigitalItem unconfiguredItem;
    
    private Account noMembershipReader;
    private Account expiredMembershipReader;
    private Account activeMembershipReader;
    private MembershipPlan plan;

    @BeforeEach
    void setUp() {
        digitalItemRepository.deleteAll();
        resourceRepository.deleteAll();
        membershipSubscriptionRepository.deleteAll();
        accountRepository.deleteAll();

        // Create Resources and Digital Items
        resource = resourceRepository.save(Resource.builder()
                .title("Digital Book")
                .authors("Author")
                .build());
                
        configuredPreviewItem = digitalItemRepository.save(DigitalItem.builder()
                .resource(resource)
                .previewContentKey("preview-key")
                .build());

        Resource resource2 = resourceRepository.save(Resource.builder()
                .title("Digital Book 2")
                .authors("Author")
                .build());
        configuredFullItem = digitalItemRepository.save(DigitalItem.builder()
                .resource(resource2)
                .fullContentKey("full-key")
                .build());

        Resource resource3 = resourceRepository.save(Resource.builder()
                .title("Digital Book 3")
                .authors("Author")
                .build());
        configuredBothItem = digitalItemRepository.save(DigitalItem.builder()
                .resource(resource3)
                .previewContentKey("preview-key")
                .fullContentKey("full-key")
                .build());

        Resource resource4 = resourceRepository.save(Resource.builder()
                .title("Digital Book 4")
                .authors("Author")
                .build());
        unconfiguredItem = digitalItemRepository.save(DigitalItem.builder()
                .resource(resource4)
                .build());

        // Create Accounts
        LocalDateTime now = LocalDateTime.now();
        noMembershipReader = createAccount("nomem@test.local");
        expiredMembershipReader = createAccount("expired@test.local");
        activeMembershipReader = createAccount("active@test.local");

        plan = membershipPlanRepository.save(MembershipPlan.builder()
                .code("TEST_PLAN")
                .name("Test Plan")
                .durationMonths(1)
                .priceAmount(new java.math.BigDecimal("10.00"))
                .currency("USD")
                .active(true)
                .build());

        createSubscription(expiredMembershipReader, now.minusDays(40), now.minusDays(10));
        createSubscription(activeMembershipReader, now.minusDays(10), now.plusDays(20));
    }

    private Account createAccount(String email) {
        return accountRepository.save(Account.builder()
                .email(email)
                .passwordHash("hash")
                .displayName(email)
                .role(AccountRole.READER)
                .accountStatus(AccountStatus.ACTIVE)
                .membershipEligible(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private MembershipSubscription createSubscription(Account account, LocalDateTime starts, LocalDateTime expires) {
        PaymentTransaction tx = paymentTransactionRepository.save(PaymentTransaction.builder()
                .account(account)
                .plan(plan)
                .amount(plan.getPriceAmount())
                .currency(plan.getCurrency())
                .status(PaymentStatus.SUCCESS)
                .createdAt(starts)
                .completedAt(starts)
                .build());

        return membershipSubscriptionRepository.save(MembershipSubscription.builder()
                .account(account)
                .plan(plan)
                .paymentTransaction(tx)
                .startsAt(starts)
                .expiresAt(expires)
                .build());
    }

    // A. anonymous + preview configured
    @Test
    void anonymous_previewConfigured_returnsPreview() throws Exception {
        mockMvc.perform(get("/resources/{id}/digital-access", configuredPreviewItem.getResource().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessLevel").value("PREVIEW"))
                .andExpect(jsonPath("$.previewUrl").value(containsString("/digital-preview")))
                .andExpect(jsonPath("$.contentUrl").isEmpty())
                // L. raw keys never appear
                .andExpect(content().string(not(containsString("preview-key"))));
    }

    // B. reader + no membership + preview configured
    @Test
    @WithMockUser(username = "nomem@test.local", roles = "READER")
    void readerNoMem_previewConfigured_returnsPreview() throws Exception {
        mockMvc.perform(get("/resources/{id}/digital-access", configuredPreviewItem.getResource().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessLevel").value("PREVIEW"));
    }

    // C. reader + expired membership + preview configured
    @Test
    @WithMockUser(username = "expired@test.local", roles = "READER")
    void readerExpiredMem_previewConfigured_returnsPreview() throws Exception {
        mockMvc.perform(get("/resources/{id}/digital-access", configuredBothItem.getResource().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessLevel").value("PREVIEW"));
    }

    // D. active member + full configured
    @Test
    @WithMockUser(username = "active@test.local", roles = "READER")
    void activeMember_fullConfigured_returnsFull() throws Exception {
        mockMvc.perform(get("/resources/{id}/digital-access", configuredBothItem.getResource().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessLevel").value("FULL"))
                .andExpect(jsonPath("$.previewUrl").value(containsString("/digital-preview")))
                .andExpect(jsonPath("$.contentUrl").value(containsString("/digital-content")))
                .andExpect(content().string(not(containsString("full-key"))));
    }

    // E. no DigitalItem
    @Test
    void anonymous_noDigitalItem_returns404() throws Exception {
        Resource noDigitalResource = resourceRepository.save(Resource.builder().title("No Digital").authors("Author").build());
        mockMvc.perform(get("/resources/{id}/digital-access", noDigitalResource.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DIGITAL_CONTENT_NOT_FOUND"));
    }

    // F. preview key missing
    @Test
    void anonymous_previewMissing_returns404() throws Exception {
        mockMvc.perform(get("/resources/{id}/digital-access", unconfiguredItem.getResource().getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DIGITAL_CONTENT_NOT_FOUND"));
                
        mockMvc.perform(get("/resources/{id}/digital-preview", unconfiguredItem.getResource().getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DIGITAL_CONTENT_NOT_FOUND"));
    }

    // G. full key missing for active member
    @Test
    @WithMockUser(username = "active@test.local", roles = "READER")
    void activeMember_fullKeyMissing_returnsPreviewOnly() throws Exception {
        mockMvc.perform(get("/resources/{id}/digital-access", configuredPreviewItem.getResource().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessLevel").value("PREVIEW"))
                .andExpect(jsonPath("$.contentUrl").isEmpty());
    }

    // PROTECTED DELIVERY
    // H. anonymous direct full URL
    @Test
    void anonymous_directFullUrl_returns401() throws Exception {
        mockMvc.perform(get("/resources/{id}/digital-content", configuredBothItem.getResource().getId()))
                .andExpect(status().isUnauthorized());
    }

    // I. authenticated reader without active membership direct full URL
    @Test
    @WithMockUser(username = "nomem@test.local", roles = "READER")
    void readerNoMem_directFullUrl_returns403() throws Exception {
        mockMvc.perform(get("/resources/{id}/digital-content", configuredBothItem.getResource().getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DIGITAL_MEMBERSHIP_REQUIRED"));
    }

    // J. expired reader direct full URL
    @Test
    @WithMockUser(username = "expired@test.local", roles = "READER")
    void expiredReader_directFullUrl_returns403() throws Exception {
        mockMvc.perform(get("/resources/{id}/digital-content", configuredBothItem.getResource().getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DIGITAL_MEMBERSHIP_REQUIRED"));
    }

    // K. active reader + configured full content
    @Test
    @WithMockUser(username = "active@test.local", roles = "READER")
    void activeReader_directFullUrl_returnsPdf() throws Exception {
        mockMvc.perform(get("/resources/{id}/digital-content", configuredBothItem.getResource().getId()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("librio-resource-")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));
    }
}
