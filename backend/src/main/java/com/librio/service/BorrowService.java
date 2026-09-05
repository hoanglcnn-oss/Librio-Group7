package com.librio.service;

import com.librio.config.CirculationPolicyProperties;
import com.librio.domain.*;
import com.librio.dto.*;
import com.librio.exception.BorrowErrorCode;
import com.librio.exception.BorrowFlowException;
import com.librio.exception.RequestExpiredTransitionException;
import com.librio.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Điều phối toàn bộ vòng đời mượn sách vật lý.
 *
 * <p>Các invariant chính:
 * <ul>
 *   <li>Một physical item chỉ thuộc tối đa một active request hoặc một active borrowing.</li>
 *   <li>Reserve làm giảm availability; fulfil không được giảm availability lần thứ hai.</li>
 *   <li>Transition terminal release hoặc consume đúng item đã reserve trong cùng transaction.</li>
 *   <li>Overdue được derive từ dueAt và returnedAt, không persist như borrowing status.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class BorrowService {

    private static final List<BorrowRequestStatus> ACTIVE_STATUSES = List.of(
            BorrowRequestStatus.REQUESTED,
            BorrowRequestStatus.READY_FOR_PICKUP
    );
    private static final List<BorrowRequestStatus> TERMINAL_STATUSES = List.of(
            BorrowRequestStatus.FULFILLED,
            BorrowRequestStatus.CANCELLED,
            BorrowRequestStatus.REJECTED,
            BorrowRequestStatus.EXPIRED
    );
    private static final Map<BorrowRequestStatus, Set<BorrowRequestStatus>> ALLOWED_TRANSITIONS =
            buildAllowedTransitions();

    private final ResourceRepository resourceRepository;
    private final PhysicalItemRepository physicalItemRepository;
    private final AccountRepository accountRepository;
    private final BorrowRequestRepository borrowRequestRepository;
    private final BorrowingRepository borrowingRepository;
    private final CirculationPolicyProperties circulationPolicy;

    @Transactional(noRollbackFor = RequestExpiredTransitionException.class)
    public ReaderBorrowRequestItemDto createRequest(Long readerId, Long resourceId) {
        if (resourceId == null) {
            throw validation("resourceId is required");
        }

        // Lock reader để serialize eligibility, duplicate và commitment-limit checks của cùng reader.
        Account reader = accountRepository.findByIdForUpdate(readerId)
                .orElseThrow(() -> notFound(BorrowErrorCode.RESOURCE_NOT_FOUND, "Reader not found"));
        requireReaderEligible(reader);
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> notFound(BorrowErrorCode.RESOURCE_NOT_FOUND, "Resource not found"));

        if (borrowRequestRepository.existsByReaderIdAndResourceIdAndStatusIn(
                readerId, resourceId, ACTIVE_STATUSES)) {
            throw conflict(BorrowErrorCode.DUPLICATE_ACTIVE_REQUEST,
                    "Reader already has an active request for this resource");
        }

        if (borrowingRepository.existsActiveBorrowingByReaderIdAndResourceId(readerId, resourceId)) {
            throw conflict(BorrowErrorCode.ACTIVE_BORROWING_EXISTS,
                    "Reader already has an active borrowing for this resource");
        }

        long activeCommitments = borrowRequestRepository.countByReaderIdAndStatusIn(readerId, ACTIVE_STATUSES)
                + borrowingRepository.countActiveBorrowingsByReaderId(readerId);
        if (activeCommitments >= circulationPolicy.commitmentLimit()) {
            throw conflict(BorrowErrorCode.BORROWING_LIMIT_REACHED,
                    "Reader has reached the commitment limit");
        }

        if (physicalItemRepository.countByResourceId(resourceId) == 0) {
            throw conflict(BorrowErrorCode.NO_PHYSICAL_COPY, "Resource has no physical copy");
        }

        PhysicalItem item = physicalItemRepository.findForUpdate(
                        resourceId, PhysicalItemStatus.AVAILABLE, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElseThrow(() -> conflict(BorrowErrorCode.NO_AVAILABLE_COPY,
                        "No physical item is currently available"));

        LocalDateTime now = LocalDateTime.now();
        // Reserve là thời điểm availability giảm; các bước sau chỉ chuyển commitment sang state khác.
        item.setStatus(PhysicalItemStatus.RESERVED);

        BorrowRequest request = BorrowRequest.builder()
                .reader(reader)
                .resource(resource)
                .physicalItem(item)
                .status(BorrowRequestStatus.REQUESTED)
                .requestedAt(now)
                .statusUpdatedAt(now)
                .expiresAt(now.plus(circulationPolicy.requestExpiration()))
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toReaderRequestDto(borrowRequestRepository.save(request));
    }

    @Transactional(noRollbackFor = RequestExpiredTransitionException.class)
    public LibrarianBorrowRequestItemDto prepare(Long librarianId, Long requestId, Long physicalItemId) {
        requirePhysicalItemId(physicalItemId);
        BorrowRequest request = getRequestForUpdate(requestId);
        Account librarian = getLibrarian(librarianId);
        requireState(request, BorrowRequestStatus.REQUESTED);
        requireNotExpired(request);
        requireMatchingItem(request, physicalItemId);
        requireReservedItem(request);

        LocalDateTime now = LocalDateTime.now();
        transition(request, BorrowRequestStatus.READY_FOR_PICKUP, now);
        request.setPreparedAt(now);
        request.setPreparedBy(librarian);
        request.setExpiresAt(now.plus(circulationPolicy.pickupExpiration()));

        return toLibrarianRequestDto(request);
    }

    @Transactional(noRollbackFor = RequestExpiredTransitionException.class)
    public LibrarianBorrowingDto fulfil(Long librarianId, Long requestId, Long physicalItemId) {
        requirePhysicalItemId(physicalItemId);
        BorrowRequest request = getRequestForUpdate(requestId);
        Account librarian = getLibrarian(librarianId);
        requireState(request, BorrowRequestStatus.READY_FOR_PICKUP);
        requireNotExpired(request);

        Account reader = accountRepository.findByIdForUpdate(request.getReader().getId())
                .orElseThrow(() -> notFound(BorrowErrorCode.RESOURCE_NOT_FOUND, "Reader not found"));
        requireReaderEligible(reader);
        requireMatchingItem(request, physicalItemId);
        requireReservedItem(request);

        if (borrowingRepository.existsByBorrowRequestId(requestId)) {
            throw conflict(BorrowErrorCode.INVALID_REQUEST_STATE, "Borrowing already exists for this request");
        }

        LocalDateTime now = LocalDateTime.now();
        transition(request, BorrowRequestStatus.FULFILLED, now);
        request.setFulfilledAt(now);
        request.setFulfilledBy(librarian);
        // Item đã bị loại khỏi availability khi reserve, nên fulfil chỉ consume reservation sang borrowing.
        request.getPhysicalItem().setStatus(PhysicalItemStatus.BORROWED);

        Borrowing borrowing = Borrowing.builder()
                .physicalItem(request.getPhysicalItem())
                .reader(reader)
                .borrowRequest(request)
                .borrowedAt(now)
                .dueAt(now.plus(circulationPolicy.loanPeriod()))
                .build();

        return toLibrarianBorrowingDto(borrowingRepository.save(borrowing));
    }

    @Transactional(readOnly = true)
    public ReaderBorrowRequestsResponseDto getReaderRequests(Long readerId) {
        return ReaderBorrowRequestsResponseDto.builder()
                .activeRequests(borrowRequestRepository.findActiveForReader(readerId, ACTIVE_STATUSES)
                        .stream()
                        .map(this::toReaderRequestDto)
                        .toList())
                .recentOutcomes(borrowRequestRepository.findRecentOutcomesForReader(
                                readerId, TERMINAL_STATUSES, PageRequest.of(0, 5))
                        .stream()
                        .map(this::toReaderRequestDto)
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public ReaderBorrowingsResponseDto getReaderBorrowings(Long readerId) {
        return ReaderBorrowingsResponseDto.builder()
                .activeBorrowings(borrowingRepository.findActiveByReaderId(readerId)
                        .stream()
                        .map(this::toReaderBorrowingDto)
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public LibrarianBorrowingsResponseDto getActiveBorrowingsForLibrarian(Long librarianId) {
        getLibrarian(librarianId);
        LocalDateTime now = LocalDateTime.now();
        return LibrarianBorrowingsResponseDto.builder()
                .activeBorrowings(borrowingRepository.findActiveForLibrarian()
                        .stream()
                        .map(borrowing -> toLibrarianBorrowingDto(borrowing, now))
                        .toList())
                .build();
    }

    @Transactional
    public LibrarianBorrowingDto returnBorrowing(Long librarianId, Long borrowingId) {
        getLibrarian(librarianId);
        // Lock borrowing trước để một lượt mượn chỉ có một return thắng, rồi mới lock exact item.
        Borrowing borrowing = borrowingRepository.findByIdForUpdate(borrowingId)
                .orElseThrow(() -> notFound(BorrowErrorCode.BORROWING_NOT_FOUND, "Borrowing not found"));
        if (borrowing.getReturnedAt() != null) {
            throw conflict(BorrowErrorCode.BORROWING_ALREADY_RETURNED, "Borrowing has already been returned");
        }
        PhysicalItem item = physicalItemRepository.findByIdForUpdate(borrowing.getPhysicalItem().getId())
                .orElseThrow(() -> conflict(BorrowErrorCode.BORROWING_ITEM_CONFLICT,
                        "Borrowed item no longer exists"));
        if (item.getStatus() != PhysicalItemStatus.BORROWED) {
            throw conflict(BorrowErrorCode.BORROWING_ITEM_CONFLICT,
                    "Borrowed item is not in BORROWED state");
        }

        LocalDateTime now = LocalDateTime.now();
        borrowing.setReturnedAt(now);
        item.setStatus(PhysicalItemStatus.AVAILABLE);
        return toLibrarianBorrowingDto(borrowing, now);
    }

    @Transactional(readOnly = true)
    public LibrarianBorrowRequestsResponseDto getAllRequests(Long librarianId) {
        getLibrarian(librarianId);
        return LibrarianBorrowRequestsResponseDto.builder()
                .items(borrowRequestRepository.findActiveForLibrarian(ACTIVE_STATUSES)
                        .stream()
                        .map(this::toLibrarianRequestDto)
                        .toList())
                .recentOutcomes(borrowRequestRepository.findRecentOutcomesForLibrarian(
                                TERMINAL_STATUSES, PageRequest.of(0, 50))
                        .stream()
                        .map(this::toLibrarianRequestDto)
                        .toList())
                .build();
    }

    @Transactional(noRollbackFor = RequestExpiredTransitionException.class)
    public ReaderBorrowRequestItemDto cancel(Long readerId, Long requestId) {
        BorrowRequest request = borrowRequestRepository.findByIdAndReaderIdForUpdate(requestId, readerId)
                .orElseThrow(() -> notFound(BorrowErrorCode.REQUEST_NOT_FOUND, "Borrow request not found"));
        if (TERMINAL_STATUSES.contains(request.getStatus())) {
            throw conflict(BorrowErrorCode.REQUEST_NOT_CANCELLABLE, "Borrow request cannot be cancelled");
        }
        requireNotExpired(request);

        LocalDateTime now = LocalDateTime.now();
        transition(request, BorrowRequestStatus.CANCELLED, now);
        releaseReservedItem(request);
        return toReaderRequestDto(request);
    }

    @Transactional(noRollbackFor = RequestExpiredTransitionException.class)
    public LibrarianBorrowRequestItemDto reject(Long librarianId, Long requestId) {
        BorrowRequest request = getRequestForUpdate(requestId);
        Account librarian = getLibrarian(librarianId);
        if (!Set.of(BorrowRequestStatus.REQUESTED, BorrowRequestStatus.READY_FOR_PICKUP).contains(request.getStatus())) {
            throw conflict(BorrowErrorCode.INVALID_REQUEST_STATE, "Borrow request cannot be rejected");
        }
        requireNotExpired(request);

        LocalDateTime now = LocalDateTime.now();
        transition(request, BorrowRequestStatus.REJECTED, now);
        releaseReservedItem(request);
        request.setRejectedAt(now);
        request.setRejectedBy(librarian);
        request.setRejectionReason(null);
        return toLibrarianRequestDto(request);
    }

    @Transactional
    public int expireDueRequests() {
        LocalDateTime now = LocalDateTime.now();
        List<BorrowRequest> expiredRequests =
                borrowRequestRepository.findExpiredActiveForUpdate(now, ACTIVE_STATUSES);
        expiredRequests.forEach(request -> {
            transition(request, BorrowRequestStatus.EXPIRED, now);
            releaseReservedItem(request);
        });
        return expiredRequests.size();
    }

    private BorrowRequest getRequestForUpdate(Long requestId) {
        return borrowRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> notFound(BorrowErrorCode.REQUEST_NOT_FOUND, "Borrow request not found"));
    }

    private Account getLibrarian(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> notFound(BorrowErrorCode.REQUEST_NOT_FOUND, "Librarian not found"));
        requireRole(account, AccountRole.LIBRARIAN);
        return account;
    }

    private void transition(BorrowRequest request, BorrowRequestStatus target, LocalDateTime now) {
        Set<BorrowRequestStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(
                request.getStatus(), Set.of());
        if (!allowed.contains(target)) {
            throw conflict(BorrowErrorCode.INVALID_REQUEST_STATE,
                    "Cannot transition borrow request from " + request.getStatus() + " to " + target);
        }
        request.setStatus(target);
        touch(request, now);
    }

    private void requireRole(Account account, AccountRole role) {
        if (account.getRole() != role) {
            throw new BorrowFlowException(BorrowErrorCode.OPERATION_FORBIDDEN.name(), HttpStatus.FORBIDDEN,
                    "Account must have role " + role);
        }
    }

    private void requireReaderEligible(Account account) {
        requireRole(account, AccountRole.READER);
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw conflict(BorrowErrorCode.READER_INELIGIBLE, "Reader is not eligible to borrow");
        }
    }

    private void requirePhysicalItemId(Long physicalItemId) {
        if (physicalItemId == null) {
            throw validation("physicalItemId is required");
        }
    }

    private void requireState(BorrowRequest request, BorrowRequestStatus expected) {
        if (request.getStatus() != expected) {
            throw conflict(BorrowErrorCode.INVALID_REQUEST_STATE,
                    "Borrow request must be " + expected);
        }
    }

    private void requireNotExpired(BorrowRequest request) {
        LocalDateTime now = LocalDateTime.now();
        if (request.getExpiresAt() != null && !now.isBefore(request.getExpiresAt())) {
            // noRollbackFor cho phép commit EXPIRED + release item dù API trả 409 REQUEST_EXPIRED.
            expireRequest(request, now);
            throw new RequestExpiredTransitionException("Borrow request has expired");
        }
    }

    private void expireRequest(BorrowRequest request, LocalDateTime now) {
        transition(request, BorrowRequestStatus.EXPIRED, now);
        releaseReservedItem(request);
    }

    private void requireReservedItem(BorrowRequest request) {
        if (request.getPhysicalItem() == null
                || request.getPhysicalItem().getStatus() != PhysicalItemStatus.RESERVED) {
            throw conflict(BorrowErrorCode.RESERVATION_CONFLICT, "Physical item must be RESERVED");
        }
    }

    private void requireMatchingItem(BorrowRequest request, Long physicalItemId) {
        if (request.getPhysicalItem() == null || !request.getPhysicalItem().getId().equals(physicalItemId)) {
            throw conflict(BorrowErrorCode.ITEM_MISMATCH,
                    "Requested physical item does not match the allocated item");
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

    private ReaderBorrowRequestItemDto toReaderRequestDto(BorrowRequest request) {
        return ReaderBorrowRequestItemDto.builder()
                .id(request.getId())
                .resource(toResourceSummary(request.getResource()))
                .status(request.getStatus())
                .requestedAt(toOffset(request.getRequestedAt()))
                .expiresAt(toOffset(request.getExpiresAt()))
                .statusUpdatedAt(toOffset(request.getStatusUpdatedAt()))
                .build();
    }

    private LibrarianBorrowRequestItemDto toLibrarianRequestDto(BorrowRequest request) {
        return LibrarianBorrowRequestItemDto.builder()
                .id(request.getId())
                .reader(toReaderSummary(request.getReader()))
                .resource(toResourceSummary(request.getResource()))
                .physicalItemId(request.getPhysicalItem().getId())
                .status(request.getStatus())
                .requestedAt(toOffset(request.getRequestedAt()))
                .readyAt(toOffset(request.getPreparedAt()))
                .expiresAt(toOffset(request.getExpiresAt()))
                .fulfilledAt(toOffset(request.getFulfilledAt()))
                .rejectedAt(toOffset(request.getRejectedAt()))
                .statusUpdatedAt(toOffset(request.getStatusUpdatedAt()))
                .build();
    }

    private ReaderBorrowingItemDto toReaderBorrowingDto(Borrowing borrowing) {
        LocalDateTime now = LocalDateTime.now();
        return ReaderBorrowingItemDto.builder()
                .id(borrowing.getId())
                .resource(toResourceSummary(borrowing.getPhysicalItem().getResource()))
                .borrowedAt(toOffset(borrowing.getBorrowedAt()))
                .dueDate(toOffset(borrowing.getDueAt()))
                .overdue(isOverdue(borrowing, now))
                .build();
    }

    private LibrarianBorrowingDto toLibrarianBorrowingDto(Borrowing borrowing) {
        return toLibrarianBorrowingDto(borrowing, LocalDateTime.now());
    }

    private LibrarianBorrowingDto toLibrarianBorrowingDto(Borrowing borrowing, LocalDateTime now) {
        return LibrarianBorrowingDto.builder()
                .id(borrowing.getId())
                .borrowRequestId(borrowing.getBorrowRequest().getId())
                .reader(toReaderSummary(borrowing.getReader()))
                .resource(toResourceSummary(borrowing.getPhysicalItem().getResource()))
                .physicalItemId(borrowing.getPhysicalItem().getId())
                .borrowedAt(toOffset(borrowing.getBorrowedAt()))
                .dueDate(toOffset(borrowing.getDueAt()))
                .returnedAt(toOffset(borrowing.getReturnedAt()))
                .overdue(isOverdue(borrowing, now))
                .build();
    }

    private boolean isOverdue(Borrowing borrowing, LocalDateTime now) {
        // Overdue là derived value tại serverNow, không phải status persist trong bảng borrowing.
        return borrowing.getReturnedAt() == null && borrowing.getDueAt().isBefore(now);
    }

    private ReaderSummaryDto toReaderSummary(Account reader) {
        return ReaderSummaryDto.builder()
                .id(reader.getId())
                .displayName(reader.getDisplayName())
                .build();
    }

    private ResourceSummaryDto toResourceSummary(Resource resource) {
        return ResourceSummaryDto.builder()
                .id(resource.getId())
                .title(resource.getTitle())
                .authors(splitAuthors(resource.getAuthors()))
                .build();
    }

    private List<String> splitAuthors(String authors) {
        if (authors == null || authors.isBlank()) {
            return List.of();
        }
        return Arrays.stream(authors.split(","))
                .map(String::trim)
                .filter(author -> !author.isEmpty())
                .toList();
    }

    private OffsetDateTime toOffset(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private BorrowFlowException validation(String message) {
        return new BorrowFlowException(BorrowErrorCode.VALIDATION_ERROR.name(), HttpStatus.BAD_REQUEST, message);
    }

    private BorrowFlowException conflict(BorrowErrorCode code, String message) {
        return new BorrowFlowException(code.name(), HttpStatus.CONFLICT, message);
    }

    private BorrowFlowException notFound(BorrowErrorCode code, String message) {
        return new BorrowFlowException(code.name(), HttpStatus.NOT_FOUND, message);
    }

    private static Map<BorrowRequestStatus, Set<BorrowRequestStatus>> buildAllowedTransitions() {
        Map<BorrowRequestStatus, Set<BorrowRequestStatus>> transitions =
                new EnumMap<>(BorrowRequestStatus.class);
        transitions.put(BorrowRequestStatus.REQUESTED, Set.of(
                BorrowRequestStatus.READY_FOR_PICKUP,
                BorrowRequestStatus.CANCELLED,
                BorrowRequestStatus.REJECTED,
                BorrowRequestStatus.EXPIRED
        ));
        transitions.put(BorrowRequestStatus.READY_FOR_PICKUP, Set.of(
                BorrowRequestStatus.FULFILLED,
                BorrowRequestStatus.CANCELLED,
                BorrowRequestStatus.REJECTED,
                BorrowRequestStatus.EXPIRED
        ));
        return transitions;
    }
}
