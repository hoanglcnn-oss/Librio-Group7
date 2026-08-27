package com.librio.controller;

import com.librio.domain.*;
import com.librio.repository.AccountRepository;
import com.librio.repository.BorrowRequestRepository;
import com.librio.repository.PhysicalItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BorrowRequestControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PhysicalItemRepository physicalItemRepository;
    @Autowired private BorrowRequestRepository borrowRequestRepository;

    @Test
    @WithMockUser(username = "http-reader@test.local", roles = "READER")
    void createBorrowRequestAliasReturnsCreatedAndReservesPhysicalItem() throws Exception {
        createAccount("http-reader@test.local", AccountRole.READER);

        mockMvc.perform(post("/borrow-requests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.physicalItemId").doesNotExist());

        assertThat(physicalItemRepository.findById(101L).orElseThrow().getStatus())
                .isEqualTo(PhysicalItemStatus.RESERVED);
    }

    @Test
    @WithMockUser(username = "http-reader@test.local", roles = "READER")
    void prepareWithMismatchedItemReturnsStableBusinessCode() throws Exception {
        createAccount("http-reader@test.local", AccountRole.READER);
        createAccount("http-librarian@test.local", AccountRole.LIBRARIAN);

        mockMvc.perform(post("/borrow-requests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":4}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long requestId = borrowRequestRepository.findAll().stream().findFirst().orElseThrow().getId();

        mockMvc.perform(post("/librarian/borrow-requests/{id}/prepare", requestId)
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("http-librarian@test.local").roles("LIBRARIAN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"physicalItemId\":9999}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ITEM_MISMATCH"));
    }

    @Test
    @WithMockUser(username = "http-reader@test.local", roles = "READER")
    void readerRequestsResponseHidesInternalFields() throws Exception {
        createAccount("http-reader@test.local", AccountRole.READER);

        mockMvc.perform(post("/borrow-requests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":4}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/me/borrow-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRequests[0].readerId").doesNotExist())
                .andExpect(jsonPath("$.activeRequests[0].physicalItemId").doesNotExist())
                .andExpect(jsonPath("$.activeRequests[0].readyAt").doesNotExist())
                .andExpect(jsonPath("$.activeRequests[0].fulfilledAt").doesNotExist())
                .andExpect(jsonPath("$.activeRequests[0].rejectedAt").doesNotExist());
    }

    @Test
    @WithMockUser(username = "http-reader@test.local", roles = "READER")
    void readerBorrowingsResponseMatchesExactContract() throws Exception {
        createAccount("http-reader@test.local", AccountRole.READER);
        createAccount("http-librarian@test.local", AccountRole.LIBRARIAN);

        MvcResult createResult = mockMvc.perform(post("/borrow-requests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":4}"))
                .andExpect(status().isCreated())
                .andReturn();

        Long requestId = borrowRequestRepository.findAll().stream().findFirst().orElseThrow().getId();

        mockMvc.perform(post("/librarian/borrow-requests/{id}/prepare", requestId)
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("http-librarian@test.local").roles("LIBRARIAN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"physicalItemId\":401}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/librarian/borrow-requests/{id}/fulfil", requestId)
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("http-librarian@test.local").roles("LIBRARIAN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"physicalItemId\":401}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/me/borrowings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeBorrowings[0].borrowRequestId").doesNotExist())
                .andExpect(jsonPath("$.activeBorrowings[0].resource").exists())
                .andExpect(jsonPath("$.activeBorrowings[0].borrowedAt").exists())
                .andExpect(jsonPath("$.activeBorrowings[0].dueDate").exists());
    }

    private void createAccount(String email, AccountRole role) {
        LocalDateTime now = LocalDateTime.now();
        accountRepository.save(Account.builder()
                .email(email)
                .passwordHash("not-used-in-controller-test")
                .displayName(email)
                .role(role)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}
