package com.librio.service;

import com.librio.domain.*;
import com.librio.dto.LibrarianBorrowRequestItemDto;
import com.librio.dto.LibrarianBorrowingDto;
import com.librio.dto.ReaderBorrowRequestItemDto;
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
import static org.assertj.core.api.Assertions.catchThrowable;
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

        ReaderBorrowRequestItemDto requested = borrowService.createRequest(reader.getId(), 1L);
        Long physicalItemId = allocatedItemId(requested.getId());
        assertThat(requested.getStatus()).isEqualTo(BorrowRequestStatus.REQUESTED);
        assertThat(physicalItemId).isEqualTo(101L);
        assertThat(physicalItemRepository.findById(101L).orElseThrow().getStatus())
                .isEqualTo(PhysicalItemStatus.RESERVED);

        LibrarianBorrowRequestItemDto ready = borrowService.prepare(librarian.getId(), requested.getId(), physicalItemId);
        assertThat(ready.getStatus()).isEqualTo(BorrowRequestStatus.READY_FOR_PICKUP);
        assertThat(ready.getExpiresAt()).isAfter(ready.getReadyAt());

        LibrarianBorrowingDto borrowing = borrowService.fulfil(librarian.getId(), requested.getId(), physicalItemId);
        assertThat(borrowing.getBorrowRequestId()).isEqualTo(requested.getId());
        assertThat(borrowing.getDueDate()).isAfter(borrowing.getBorrowedAt());
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

    @Test
    void readerCanCancelRequestedRequestAndItemBecomesAvailable() {
        Account reader = createAccount("cancel-reader@test.local", AccountRole.READER);
        ReaderBorrowRequestItemDto requested = borrowService.createRequest(reader.getId(), 4L);
        Long physicalItemId = allocatedItemId(requested.getId());

        ReaderBorrowRequestItemDto cancelled = borrowService.cancel(reader.getId(), requested.getId());

        assertThat(cancelled.getStatus()).isEqualTo(BorrowRequestStatus.CANCELLED);
        assertThat(physicalItemRepository.findById(physicalItemId).orElseThrow().getStatus())
                .isEqualTo(PhysicalItemStatus.AVAILABLE);
    }

    @Test
    void librarianCanRejectRequestedRequestAndItemBecomesAvailable() {
        Account reader = createAccount("reject-reader@test.local", AccountRole.READER);
        Account librarian = createAccount("reject-librarian@test.local", AccountRole.LIBRARIAN);
        ReaderBorrowRequestItemDto requested = borrowService.createRequest(reader.getId(), 4L);
        Long physicalItemId = allocatedItemId(requested.getId());

        LibrarianBorrowRequestItemDto rejected = borrowService.reject(librarian.getId(), requested.getId());

        assertThat(rejected.getStatus()).isEqualTo(BorrowRequestStatus.REJECTED);
        assertThat(rejected.getRejectedAt()).isNotNull();
        assertThat(physicalItemRepository.findById(physicalItemId).orElseThrow().getStatus())
                .isEqualTo(PhysicalItemStatus.AVAILABLE);
    }

    @Test
    void schedulerCanExpireReadyRequestAfterPickupDeadline() {
        Account reader = createAccount("expire-reader@test.local", AccountRole.READER);
        Account librarian = createAccount("expire-librarian@test.local", AccountRole.LIBRARIAN);
        ReaderBorrowRequestItemDto requested = borrowService.createRequest(reader.getId(), 4L);
        Long physicalItemId = allocatedItemId(requested.getId());
        borrowService.prepare(librarian.getId(), requested.getId(), physicalItemId);
        BorrowRequest request = borrowRequestRepository.findById(requested.getId()).orElseThrow();
        request.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        borrowRequestRepository.saveAndFlush(request);

        int expired = borrowService.expireDueRequests();

        assertThat(expired).isEqualTo(1);
        assertThat(borrowRequestRepository.findById(requested.getId()).orElseThrow().getStatus())
                .isEqualTo(BorrowRequestStatus.EXPIRED);
        assertThat(physicalItemRepository.findById(physicalItemId).orElseThrow().getStatus())
                .isEqualTo(PhysicalItemStatus.AVAILABLE);
    }

    @Test
    void terminalStatusCannotTransitionBackToRequestedOrReady() {
        Account reader = createAccount("terminal-reader@test.local", AccountRole.READER);
        Account librarian = createAccount("terminal-librarian@test.local", AccountRole.LIBRARIAN);
        ReaderBorrowRequestItemDto requested = borrowService.createRequest(reader.getId(), 4L);
        Long physicalItemId = allocatedItemId(requested.getId());
        borrowService.prepare(librarian.getId(), requested.getId(), physicalItemId);
        borrowService.fulfil(librarian.getId(), requested.getId(), physicalItemId);

        assertThatThrownBy(() -> borrowService.prepare(librarian.getId(), requested.getId(), physicalItemId))
                .isInstanceOf(BorrowFlowException.class)
                .hasMessage("Borrow request must be REQUESTED");
        assertThat(borrowRequestRepository.findById(requested.getId()).orElseThrow().getStatus())
                .isEqualTo(BorrowRequestStatus.FULFILLED);
    }

    @Test
    void rejectsCreateRequestWhenActiveBorrowingExistsForSameResource() {
        Account reader = createAccount("active-borrow-reader@test.local", AccountRole.READER);
        Account librarian = createAccount("active-borrow-librarian@test.local", AccountRole.LIBRARIAN);
        ReaderBorrowRequestItemDto requested = borrowService.createRequest(reader.getId(), 1L);
        Long physicalItemId = allocatedItemId(requested.getId());
        borrowService.prepare(librarian.getId(), requested.getId(), physicalItemId);
        borrowService.fulfil(librarian.getId(), requested.getId(), physicalItemId);

        Throwable thrown = catchThrowable(() -> borrowService.createRequest(reader.getId(), 1L));
        assertThat(thrown).isInstanceOf(BorrowFlowException.class);
        assertThat(((BorrowFlowException) thrown).getCode()).isEqualTo("ACTIVE_BORROWING_EXISTS");
    }

    @Test
    void librarianActionRejectsMismatchedItemWhenBodyDoesNotMatchAllocation() {
        Account reader = createAccount("mismatch-reader@test.local", AccountRole.READER);
        Account librarian = createAccount("mismatch-librarian@test.local", AccountRole.LIBRARIAN);
        ReaderBorrowRequestItemDto requested = borrowService.createRequest(reader.getId(), 4L);

        Throwable thrown = catchThrowable(() -> borrowService.prepare(librarian.getId(), requested.getId(), 9999L));
        assertThat(thrown).isInstanceOf(BorrowFlowException.class);
        assertThat(((BorrowFlowException) thrown).getCode()).isEqualTo("ITEM_MISMATCH");
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

    private Long allocatedItemId(Long requestId) {
        return borrowRequestRepository.findById(requestId)
                .orElseThrow()
                .getPhysicalItem()
                .getId();
    }
}
