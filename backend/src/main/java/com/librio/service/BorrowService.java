package com.librio.service;

import com.librio.domain.*;
import com.librio.dto.BorrowRequestDto;
import com.librio.dto.BorrowingDto;
import com.librio.exception.BorrowFlowException;
import com.librio.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BorrowService {

    private static final List<BorrowRequestStatus> ACTIVE_STATUSES = List.of(
            BorrowRequestStatus.REQUESTED,
            BorrowRequestStatus.READY_FOR_PICKUP
    );
    private static final int PICKUP_WINDOW_DAYS = 3;
    private static final int LOAN_PERIOD_DAYS = 14;

    private final ResourceRepository resourceRepository;
    private final PhysicalItemRepository physicalItemRepository;
    private final AccountRepository accountRepository;
    private final BorrowRequestRepository borrowRequestRepository;
    private final BorrowingRepository borrowingRepository;

    @Transactional
    public BorrowRequestDto createRequest(Long readerId, Long resourceId) {
        if (resourceId == null) {
            throw new BorrowFlowException(HttpStatus.BAD_REQUEST, "resourceId is required");
        }

        Account reader = accountRepository.findByIdForUpdate(readerId)
                .orElseThrow(() -> new BorrowFlowException(HttpStatus.NOT_FOUND, "Reader not found"));
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BorrowFlowException(HttpStatus.NOT_FOUND, "Resource not found"));

        if (borrowRequestRepository.existsByReaderIdAndResourceIdAndStatusIn(
                readerId, resourceId, ACTIVE_STATUSES)) {
            throw new BorrowFlowException(HttpStatus.CONFLICT,
                    "Reader already has an active request for this resource");
        }

        PhysicalItem item = physicalItemRepository.findForUpdate(
                        resourceId, PhysicalItemStatus.AVAILABLE, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElseThrow(() -> new BorrowFlowException(HttpStatus.CONFLICT,
                        "No physical item is currently available"));

        LocalDateTime now = LocalDateTime.now();
        item.setStatus(PhysicalItemStatus.RESERVED);

        BorrowRequest request = BorrowRequest.builder()
                .reader(reader)
                .resource(resource)
                .physicalItem(item)
                .status(BorrowRequestStatus.REQUESTED)
                .requestedAt(now)
                .statusUpdatedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toRequestDto(borrowRequestRepository.save(request));
    }

    @Transactional
    public BorrowRequestDto prepare(Long librarianId, Long requestId) {
        BorrowRequest request = getRequest(requestId);
        requireStatus(request, BorrowRequestStatus.REQUESTED);
        Account librarian = getAccount(librarianId, "Librarian not found");

        LocalDateTime now = LocalDateTime.now();
        request.setStatus(BorrowRequestStatus.READY_FOR_PICKUP);
        request.setPreparedAt(now);
        request.setPreparedBy(librarian);
        request.setExpiresAt(now.plusDays(PICKUP_WINDOW_DAYS));
        touch(request, now);

        return toRequestDto(request);
    }

    @Transactional
    public BorrowingDto fulfil(Long librarianId, Long requestId) {
        BorrowRequest request = getRequest(requestId);
        requireStatus(request, BorrowRequestStatus.READY_FOR_PICKUP);
        Account librarian = getAccount(librarianId, "Librarian not found");

        LocalDateTime now = LocalDateTime.now();
        if (request.getExpiresAt() != null && !now.isBefore(request.getExpiresAt())) {
            throw new BorrowFlowException(HttpStatus.CONFLICT, "Borrow request has expired");
        }
        if (borrowingRepository.existsByBorrowRequestId(requestId)) {
            throw new BorrowFlowException(HttpStatus.CONFLICT,
                    "Borrowing already exists for this request");
        }

        request.setStatus(BorrowRequestStatus.FULFILLED);
        request.setFulfilledAt(now);
        request.setFulfilledBy(librarian);
        request.getPhysicalItem().setStatus(PhysicalItemStatus.BORROWED);
        touch(request, now);

        Borrowing borrowing = Borrowing.builder()
                .physicalItem(request.getPhysicalItem())
                .reader(request.getReader())
                .borrowRequest(request)
                .borrowedAt(now)
                .dueAt(now.plusDays(LOAN_PERIOD_DAYS))
                .build();

        return toBorrowingDto(borrowingRepository.save(borrowing));
    }

    private BorrowRequest getRequest(Long requestId) {
        return borrowRequestRepository.findById(requestId)
                .orElseThrow(() -> new BorrowFlowException(HttpStatus.NOT_FOUND,
                        "Borrow request not found"));
    }

    private Account getAccount(Long accountId, String message) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new BorrowFlowException(HttpStatus.NOT_FOUND, message));
    }

    private void requireStatus(BorrowRequest request, BorrowRequestStatus expected) {
        if (request.getStatus() != expected) {
            throw new BorrowFlowException(HttpStatus.CONFLICT,
                    "Borrow request must be " + expected + " but is " + request.getStatus());
        }
    }

    private void touch(BorrowRequest request, LocalDateTime now) {
        request.setStatusUpdatedAt(now);
        request.setUpdatedAt(now);
    }

    private BorrowRequestDto toRequestDto(BorrowRequest request) {
        return BorrowRequestDto.builder()
                .id(request.getId())
                .readerId(request.getReader().getId())
                .resourceId(request.getResource().getId())
                .physicalItemId(request.getPhysicalItem().getId())
                .status(request.getStatus())
                .requestedAt(request.getRequestedAt())
                .readyAt(request.getPreparedAt())
                .expiresAt(request.getExpiresAt())
                .fulfilledAt(request.getFulfilledAt())
                .build();
    }

    private BorrowingDto toBorrowingDto(Borrowing borrowing) {
        return BorrowingDto.builder()
                .id(borrowing.getId())
                .borrowRequestId(borrowing.getBorrowRequest().getId())
                .readerId(borrowing.getReader().getId())
                .physicalItemId(borrowing.getPhysicalItem().getId())
                .borrowedAt(borrowing.getBorrowedAt())
                .dueAt(borrowing.getDueAt())
                .build();
    }
}
