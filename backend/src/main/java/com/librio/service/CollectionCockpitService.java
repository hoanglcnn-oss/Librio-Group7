package com.librio.service;

import com.librio.domain.BorrowRequest;
import com.librio.domain.BorrowRequestStatus;
import com.librio.domain.Borrowing;
import com.librio.domain.CirculationStatus;
import com.librio.domain.InventoryStatus;
import com.librio.domain.PhysicalItem;
import com.librio.dto.CockpitActiveOperationDto;
import com.librio.dto.CockpitPageResponseDto;
import com.librio.dto.CockpitPhysicalItemDto;
import com.librio.dto.CockpitResourceDto;
import com.librio.dto.InventorySummaryDto;
import com.librio.dto.ReaderSummaryDto;
import com.librio.repository.BorrowRequestRepository;
import com.librio.repository.BorrowingRepository;
import com.librio.repository.PhysicalItemRepository;
import lombok.RequiredArgsConstructor;
import com.librio.exception.BorrowFlowException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectionCockpitService {

    private static final List<BorrowRequestStatus> ACTIVE_REQUEST_STATUSES = List.of(
            BorrowRequestStatus.REQUESTED,
            BorrowRequestStatus.READY_FOR_PICKUP
    );

    private final PhysicalItemRepository physicalItemRepository;
    private final BorrowRequestRepository borrowRequestRepository;
    private final BorrowingRepository borrowingRepository;

    @Transactional(readOnly = true)
    public InventorySummaryDto getSummary() {
        LocalDateTime now = LocalDateTime.now();
        PhysicalItemRepository.CockpitSummaryProjection proj = physicalItemRepository.findCockpitSummary(now);

        long totalCopies = proj != null && proj.getTotalCopies() != null ? proj.getTotalCopies() : 0L;
        long availableCopies = proj != null && proj.getAvailableCopies() != null ? proj.getAvailableCopies() : 0L;
        long reservedCopies = proj != null && proj.getReservedCopies() != null ? proj.getReservedCopies() : 0L;
        long borrowedCopies = proj != null && proj.getBorrowedCopies() != null ? proj.getBorrowedCopies() : 0L;
        long attentionCopies = proj != null && proj.getAttentionCopies() != null ? proj.getAttentionCopies() : 0L;

        return InventorySummaryDto.builder()
                .totalCopies(totalCopies)
                .availableCopies(availableCopies)
                .reservedCopies(reservedCopies)
                .borrowedCopies(borrowedCopies)
                .attentionCopies(attentionCopies)
                .build();
    }

    @Transactional(readOnly = true)
    public CockpitPageResponseDto searchPhysicalItems(
            String q,
            InventoryStatus inventoryStatus,
            CirculationStatus circulationStatus,
            Boolean needsAttention,
            int page,
            int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BorrowFlowException(
                    "INVALID_PAGINATION_PARAMETER",
                    HttpStatus.BAD_REQUEST,
                    "Page index must be >= 0 and size must be between 1 and 100"
            );
        }

        String trimmedQ = (q != null && !q.isBlank()) ? q.trim() : null;
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(page, size);

        String inventoryStatusValue = inventoryStatus != null ? inventoryStatus.name() : "";
        String circulationStatusValue = circulationStatus != null ? circulationStatus.name() : "";
        int attentionMode = needsAttention == null ? -1 : (needsAttention ? 1 : 0);

        Page<PhysicalItem> itemPage = physicalItemRepository.findCockpitItemsPaged(
                trimmedQ != null ? trimmedQ : "",
                inventoryStatusValue,
                circulationStatusValue,
                attentionMode,
                now,
                pageable
        );

        List<Long> itemIds = itemPage.getContent().stream().map(PhysicalItem::getId).toList();

        Map<Long, List<BorrowRequest>> activeRequestsMap = itemIds.isEmpty()
                ? Map.of()
                : fetchActiveRequestsMap(itemIds);

        Map<Long, List<Borrowing>> activeBorrowingsMap = itemIds.isEmpty()
                ? Map.of()
                : fetchActiveBorrowingsMap(itemIds);

        List<CockpitPhysicalItemDto> dtoList = itemPage.getContent().stream()
                .map(item -> mapToCockpitDto(item, activeRequestsMap, activeBorrowingsMap, now))
                .toList();

        return CockpitPageResponseDto.builder()
                .items(dtoList)
                .page(itemPage.getNumber())
                .size(itemPage.getSize())
                .totalElements(itemPage.getTotalElements())
                .totalPages(itemPage.getTotalPages())
                .build();
    }

    private Map<Long, List<BorrowRequest>> fetchActiveRequestsMap(Collection<Long> itemIds) {
        return borrowRequestRepository.findActiveByPhysicalItemIds(itemIds, ACTIVE_REQUEST_STATUSES)
                .stream()
                .filter(r -> r.getPhysicalItem() != null)
                .collect(Collectors.groupingBy(r -> r.getPhysicalItem().getId()));
    }

    private Map<Long, List<Borrowing>> fetchActiveBorrowingsMap(Collection<Long> itemIds) {
        return borrowingRepository.findActiveByPhysicalItemIds(itemIds)
                .stream()
                .filter(b -> b.getPhysicalItem() != null)
                .collect(Collectors.groupingBy(b -> b.getPhysicalItem().getId()));
    }

    private CockpitPhysicalItemDto mapToCockpitDto(
            PhysicalItem item,
            Map<Long, List<BorrowRequest>> activeRequestsMap,
            Map<Long, List<Borrowing>> activeBorrowingsMap,
            LocalDateTime now
    ) {
        List<Borrowing> borrowings = activeBorrowingsMap.getOrDefault(item.getId(), List.of());
        List<BorrowRequest> requests = activeRequestsMap.getOrDefault(item.getId(), List.of());

        boolean hasBorrowing = !borrowings.isEmpty();
        boolean hasRequest = !requests.isEmpty();

        List<String> attentionReasons = new ArrayList<>();

        if (item.getInventoryStatus() == InventoryStatus.WITHDRAWN) {
            return CockpitPhysicalItemDto.builder()
                    .id(item.getId())
                    .barcode(item.getBarcode())
                    .location(item.getLocation())
                    .inventoryStatus(item.getInventoryStatus())
                    .circulationStatus(item.getCirculationStatus())
                    .borrowable(false)
                    .needsAttention(false)
                    .attentionReasons(List.of())
                    .resource(toResourceDto(item))
                    .activeOperation(null)
                    .build();
        }

        if (item.getInventoryStatus() == InventoryStatus.LOST) {
            attentionReasons.add("INVENTORY_LOST");
        }
        if (item.getInventoryStatus() == InventoryStatus.DAMAGED) {
            attentionReasons.add("INVENTORY_DAMAGED");
        }

        if (item.getLocation() == null || item.getLocation().isBlank() || "UNASSIGNED".equalsIgnoreCase(item.getLocation().trim())) {
            attentionReasons.add("LOCATION_UNASSIGNED");
        }

        CockpitActiveOperationDto activeOperation = null;

        if (hasBorrowing) {
            Borrowing b = borrowings.get(0);
            boolean overdue = b.getReturnedAt() == null && b.getDueAt() != null && b.getDueAt().isBefore(now);
            if (overdue) {
                attentionReasons.add("BORROWING_OVERDUE");
            }

            activeOperation = CockpitActiveOperationDto.builder()
                    .type("BORROWING")
                    .id(b.getId())
                    .borrowRequestId(b.getBorrowRequest().getId())
                    .reader(ReaderSummaryDto.builder()
                            .id(b.getReader().getId())
                            .displayName(b.getReader().getDisplayName())
                            .build())
                    .borrowedAt(b.getBorrowedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime())
                    .dueAt(b.getDueAt().atZone(ZoneId.systemDefault()).toOffsetDateTime())
                    .overdue(overdue)
                    .build();
        } else if (hasRequest) {
            BorrowRequest r = requests.get(0);
            activeOperation = CockpitActiveOperationDto.builder()
                    .type("BORROW_REQUEST")
                    .id(r.getId())
                    .status(r.getStatus().name())
                    .reader(ReaderSummaryDto.builder()
                            .id(r.getReader().getId())
                            .displayName(r.getReader().getDisplayName())
                            .build())
                    .requestedAt(r.getRequestedAt() != null ? r.getRequestedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime() : null)
                    .statusUpdatedAt(r.getStatusUpdatedAt() != null ? r.getStatusUpdatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime() : null)
                    .expiresAt(r.getExpiresAt() != null ? r.getExpiresAt().atZone(ZoneId.systemDefault()).toOffsetDateTime() : null)
                    .build();
        }

        // Detect circulation data mismatch
        boolean mismatch = false;
        if (hasBorrowing && hasRequest) {
            mismatch = true;
        } else if (item.getCirculationStatus() == CirculationStatus.RESERVED && !hasRequest) {
            mismatch = true;
        } else if (item.getCirculationStatus() == CirculationStatus.BORROWED && !hasBorrowing) {
            mismatch = true;
        } else if (item.getCirculationStatus() == CirculationStatus.AVAILABLE && (hasBorrowing || hasRequest)) {
            mismatch = true;
        }

        if (mismatch) {
            attentionReasons.add("CIRCULATION_DATA_MISMATCH");
        }

        boolean borrowable = item.getInventoryStatus() == InventoryStatus.ACTIVE && item.getCirculationStatus() == CirculationStatus.AVAILABLE;
        boolean needsAttention = !attentionReasons.isEmpty();

        return CockpitPhysicalItemDto.builder()
                .id(item.getId())
                .barcode(item.getBarcode())
                .location(item.getLocation())
                .inventoryStatus(item.getInventoryStatus())
                .circulationStatus(item.getCirculationStatus())
                .borrowable(borrowable)
                .needsAttention(needsAttention)
                .attentionReasons(attentionReasons)
                .resource(toResourceDto(item))
                .activeOperation(activeOperation)
                .build();
    }

    private CockpitResourceDto toResourceDto(PhysicalItem item) {
        if (item.getResource() == null) {
            return null;
        }
        return CockpitResourceDto.builder()
                .id(item.getResource().getId())
                .title(item.getResource().getTitle())
                .authors(parseAuthors(item.getResource().getAuthors()))
                .build();
    }

    private List<String> parseAuthors(String authors) {
        if (authors == null || authors.isBlank()) {
            return List.of();
        }
        return Arrays.stream(authors.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

}
