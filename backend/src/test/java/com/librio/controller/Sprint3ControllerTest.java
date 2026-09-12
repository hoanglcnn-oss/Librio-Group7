package com.librio.controller;

import com.librio.domain.Account;
import com.librio.domain.AccountRole;
import com.librio.domain.AccountStatus;
import com.librio.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class Sprint3ControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private AccountRepository accountRepository;

    @Autowired private com.librio.repository.DigitalItemRepository digitalItemRepository;
    @Autowired private com.librio.repository.ResourceRepository resourceRepository;
    @Autowired private com.librio.repository.MembershipSubscriptionRepository membershipSubscriptionRepository;
    @Autowired private com.librio.repository.MembershipPlanRepository membershipPlanRepository;
    @Autowired private com.librio.repository.PaymentTransactionRepository paymentTransactionRepository;

    @Test
    void anonymousUserGets404ForUnconfiguredDigitalCapability() throws Exception {
        mockMvc.perform(get("/resources/1/digital-access"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DIGITAL_CONTENT_NOT_FOUND"));
    }

    @Test
    @WithMockUser(username = "digital-reader@test.local", roles = "READER")
    void activeReaderCanObtainCapabilityAndOpenPdf() throws Exception {
        Account reader = account("digital-reader@test.local", AccountRole.READER);
        com.librio.domain.Resource r1 = resourceRepository.findById(1L).orElseThrow();
        digitalItemRepository.findByResourceId(1L).ifPresent(digitalItemRepository::delete);
        digitalItemRepository.save(com.librio.domain.DigitalItem.builder()
                .resource(r1)
                .previewContentKey("preview")
                .fullContentKey("full")
                .build());

        com.librio.domain.MembershipPlan plan = membershipPlanRepository.save(com.librio.domain.MembershipPlan.builder()
                .code("PLAN")
                .name("Plan")
                .durationMonths(1)
                .priceAmount(new java.math.BigDecimal("10"))
                .currency("USD")
                .active(true)
                .build());
                
        com.librio.domain.PaymentTransaction tx = paymentTransactionRepository.save(com.librio.domain.PaymentTransaction.builder()
                .account(reader)
                .plan(plan)
                .amount(plan.getPriceAmount())
                .currency(plan.getCurrency())
                .status(com.librio.domain.PaymentStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build());

        membershipSubscriptionRepository.save(com.librio.domain.MembershipSubscription.builder()
                .account(reader)
                .plan(plan)
                .paymentTransaction(tx)
                .startsAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build());

        mockMvc.perform(get("/resources/1/digital-access"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessLevel").value("FULL"))
                .andExpect(jsonPath("$.previewUrl").value(org.hamcrest.Matchers.endsWith("/resources/1/digital-preview")))
                .andExpect(jsonPath("$.contentUrl").value(org.hamcrest.Matchers.endsWith("/resources/1/digital-content")));

        mockMvc.perform(get("/resources/1/digital-content"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().string(org.hamcrest.Matchers.startsWith("%PDF-1.4")));
    }

    @Test
    @WithMockUser(username = "admin-librarian@test.local", roles = "LIBRARIAN")
    void librarianCanCreateManagedResource() throws Exception {
        account("admin-librarian@test.local", AccountRole.LIBRARIAN);

        mockMvc.perform(post("/librarian/resources")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Domain-Driven Design",
                                  "authors": ["Eric Evans"],
                                  "description": "A domain design reference.",
                                  "category": "Technology",
                                  "accessTypes": ["PHYSICAL", "DIGITAL"],
                                  "physical": {"totalCopies": 2},
                                  "digital": {"available": true}
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.category").value("Technology"))
                .andExpect(jsonPath("$.physical.totalCopies").value(2))
                .andExpect(jsonPath("$.digital.available").value(true));
    }

    private Account account(String email, AccountRole role) {
        LocalDateTime now = LocalDateTime.now();
        return accountRepository.save(Account.builder()
                .email(email)
                .passwordHash("not-used")
                .displayName(email)
                .role(role)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}
