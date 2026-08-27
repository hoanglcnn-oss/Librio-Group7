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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BorrowService {

    private static final List<BorrowRequestStatus> ACTIVE_STATUSES = List.of(
            BorrowRequestStatus.REQUESTED,
            BorrowRequestStatus.READY_FOR_PICKUP
    );
    private static final int PICKUP_WINDOW_DAYS = 3;
    private static final int LOAN_PERIOD_DAYS = 14;
    private static final Map<BorrowRequestStatus, Set<BorrowRequestStatus>> ALLOWED_TRANSITIONS =
            buildAllowedTransitions();

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
        requireRole(reader, AccountRole.READER);
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
        BorrowRequest request = getRequestForUpdate(requestId);
        Account librarian = getLibrarian(librarianId);
        requireReservedItem(request);

        LocalDateTime now = LocalDateTime.now();
        transition(request, BorrowRequestStatus.READY_FOR_PICKUP, now);
        request.setPreparedAt(now);
        request.setPreparedBy(librarian);
        request.setExpiresAt(now.plusDays(PICKUP_WINDOW_DAYS));

        return toRequestDto(request);
    }

    @Transactional
    public BorrowingDto fulfil(Long librarianId, Long requestId) {
        BorrowRequest request = getRequestForUpdate(requestId);
        Account librarian = getLibrarian(librarianId);
        requireReservedItem(request);

        LocalDateTime now = LocalDateTime.now();
        if (request.getExpiresAt() != null && !now.isBefore(request.getExpiresAt())) {
            throw new BorrowFlowException(HttpStatus.CONFLICT, "Borrow request has expired");
        }
        if (borrowingRepository.existsByBorrowRequestId(requestId)) {
            throw new BorrowFlowException(HttpStatus.CONFLICT,
                    "Borrowing already exists for this request");
        }

        transition(request, BorrowRequestStatus.FULFILLED, now);
        request.setFulfilledAt(now);
        request.setFulfilledBy(librarian);
        request.getPhysicalItem().setStatus(PhysicalItemStatus.BORROWED);

        Borrowing borrowing = Borrowing.builder()
                .physicalItem(request.getPhysicalItem())
                .reader(request.getReader())
                .borrowRequest(request)
                .borrowedAt(now)
                .dueAt(now.plusDays(LOAN_PERIOD_DAYS))
                .build();

        return toBorrowingDto(borrowingRepository.save(borrowing));
    }

    @Transactional(readOnly = true)
    public List<BorrowRequestDto> getReaderRequests(Long readerId) {
        return borrowRequestRepository.findByReaderIdOrderByRequestedAtDesc(readerId)
                .stream()
                .map(this::toRequestDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BorrowRequestDto> getAllRequests(Long librarianId) {
        getLibrarian(librarianId);
        return borrowRequestRepository.findAllByOrderByRequestedAtDesc()
                .stream()
                .map(this::toRequestDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BorrowRequestDto cancel(Long readerId, Long requestId) {
        BorrowRequest request = getRequestForUpdate(requestId);
        if (!request.getReader().getId().equals(readerId)) {
            throw new BorrowFlowException(HttpStatus.NOT_FOUND, "Borrow request not found");
        }

        LocalDateTime now = LocalDateTime.now();
        transition(request, BorrowRequestStatus.CANCELLED, now);
        releaseReservedItem(request);
        return toRequestDto(request);
    }

    @Transactional
    public BorrowRequestDto reject(Long librarianId, Long requestId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new BorrowFlowException(HttpStatus.BAD_REQUEST, "Rejection reason is required");
        }

        BorrowRequest request = getRequestForUpdate(requestId);
        Account librarian = getLibrarian(librarianId);
        LocalDateTime now = LocalDateTime.now();
        transition(request, BorrowRequestStatus.REJECTED, now);
        releaseReservedItem(request);
        request.setRejectedAt(now);
        request.setRejectedBy(librarian);
        request.setRejectionReason(reason.trim());
        return toRequestDto(request);
    }

    @Transactional
    public BorrowRequestDto expire(Long librarianId, Long requestId) {
        getLibrarian(librarianId);
        BorrowRequest request = getRequestForUpdate(requestId);
        LocalDateTime now = LocalDateTime.now();
        if (request.getStatus() != BorrowRequestStatus.READY_FOR_PICKUP
                || request.getExpiresAt() == null
                || now.isBefore(request.getExpiresAt())) {
            throw new BorrowFlowException(HttpStatus.CONFLICT,
                    "Borrow request is not eligible for expiration");
        }

        transition(request, BorrowRequestStatus.EXPIRED, now);
        releaseReservedItem(request);
        return toRequestDto(request);
    }

    private BorrowRequest getRequestForUpdate(Long requestId) {
        return borrowRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new BorrowFlowException(HttpStatus.NOT_FOUND,
                        "Borrow request not found"));
    }

    private Account getLibrarian(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BorrowFlowException(HttpStatus.NOT_FOUND,
                        "Librarian not found"));
        requireRole(account, AccountRole.LIBRARIAN);
        return account;
    }

    private void transition(BorrowRequest request, BorrowRequestStatus target, LocalDateTime now) {
        Set<BorrowRequestStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(
                request.getStatus(), Set.of());
        if (!allowed.contains(target)) {
            throw new BorrowFlowException(HttpStatus.CONFLICT,
                    "Cannot transition borrow request from " + request.getStatus() + " to " + target);
        }
        request.setStatus(target);
        touch(request, now);
    }

    private void requireRole(Account account, AccountRole role) {
        if (account.getRole() != role) {
            throw new BorrowFlowException(HttpStatus.FORBIDDEN,
                    "Account must have role " + role);
        }
    }

    private void requireReservedItem(BorrowRequest request) {
        if (request.getPhysicalItem() == null
                || request.getPhysicalItem().getStatus() != PhysicalItemStatus.RESERVED) {
            throw new BorrowFlowException(HttpStatus.CONFLICT,
                    "Physical item must be RESERVED");
        }
    }

    private void releaseReservedItem(BorrowRequest request) {
        requireReservedItem(request);
        request.getPhysicalItem().setStatus(PhysicalItemStatus.AVAILABLE);
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
                .rejectedAt(request.getRejectedAt())
                .rejectionReason(request.getRejectionReason())
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

    private static Map<BorrowRequestStatus, Set<BorrowRequestStatus>> buildAllowedTransitions() {
        Map<BorrowRequestStatus, Set<BorrowRequestStatus>> transitions =
                new EnumMap<>(BorrowRequestStatus.class);
        transitions.put(BorrowRequestStatus.REQUESTED, Set.of(
                BorrowRequestStatus.READY_FOR_PICKUP,
                BorrowRequestStatus.CANCELLED,
                BorrowRequestStatus.REJECTED
        ));
        transitions.put(BorrowRequestStatus.READY_FOR_PICKUP, Set.of(
                BorrowRequestStatus.FULFILLED,
                BorrowRequestStatus.CANCELLED,
                BorrowRequestStatus.EXPIRED
        ));
        return transitions;
    }
}
