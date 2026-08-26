package com.librio.service;

import com.librio.domain.*;
import com.librio.dto.BorrowRequestDto;
import com.librio.dto.BorrowingDto;
import com.librio.exception.BorrowFlowException;
import com.librio.repository.AccountRepository;
import com.librio.repository.BorrowRequestRepository;
import com.librio.repository.BorrowingRepository;
import com.librio.repository.PhysicalItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BorrowServiceTest {

    @Autowired private BorrowService borrowService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PhysicalItemRepository physicalItemRepository;
    @Autowired private BorrowRequestRepository borrowRequestRepository;
    @Autowired private BorrowingRepository borrowingRepository;

    @Test
    void completesRequestPrepareAndFulfilFlow() {
        Account reader = createAccount("flow-reader@test.local", AccountRole.READER);
        Account librarian = createAccount("flow-librarian@test.local", AccountRole.LIBRARIAN);

        BorrowRequestDto requested = borrowService.createRequest(reader.getId(), 1L);
        assertThat(requested.getStatus()).isEqualTo(BorrowRequestStatus.REQUESTED);
        assertThat(requested.getPhysicalItemId()).isEqualTo(101L);
        assertThat(physicalItemRepository.findById(101L).orElseThrow().getStatus())
                .isEqualTo(PhysicalItemStatus.RESERVED);

        BorrowRequestDto ready = borrowService.prepare(librarian.getId(), requested.getId());
        assertThat(ready.getStatus()).isEqualTo(BorrowRequestStatus.READY_FOR_PICKUP);
        assertThat(ready.getExpiresAt()).isAfter(ready.getReadyAt());

        BorrowingDto borrowing = borrowService.fulfil(librarian.getId(), requested.getId());
        assertThat(borrowing.getBorrowRequestId()).isEqualTo(requested.getId());
        assertThat(borrowing.getDueAt()).isAfter(borrowing.getBorrowedAt());
        assertThat(borrowRequestRepository.findById(requested.getId()).orElseThrow().getStatus())
                .isEqualTo(BorrowRequestStatus.FULFILLED);
        assertThat(physicalItemRepository.findById(101L).orElseThrow().getStatus())
                .isEqualTo(PhysicalItemStatus.BORROWED);
        assertThat(borrowingRepository.existsByBorrowRequestId(requested.getId())).isTrue();
    }

    @Test
    void rejectsResourceWithoutAvailablePhysicalItem() {
        Account reader = createAccount("out-reader@test.local", AccountRole.READER);

        assertThatThrownBy(() -> borrowService.createRequest(reader.getId(), 2L))
                .isInstanceOf(BorrowFlowException.class)
                .hasMessage("No physical item is currently available");
    }

    @Test
    void rejectsDuplicateActiveRequest() {
        Account reader = createAccount("duplicate-reader@test.local", AccountRole.READER);
        borrowService.createRequest(reader.getId(), 1L);

        assertThatThrownBy(() -> borrowService.createRequest(reader.getId(), 1L))
                .isInstanceOf(BorrowFlowException.class)
                .hasMessage("Reader already has an active request for this resource");
    }

    private Account createAccount(String email, AccountRole role) {
        LocalDateTime now = LocalDateTime.now();
        return accountRepository.save(Account.builder()
                .email(email)
                .passwordHash("not-used-in-service-test")
                .displayName(email)
                .role(role)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}
