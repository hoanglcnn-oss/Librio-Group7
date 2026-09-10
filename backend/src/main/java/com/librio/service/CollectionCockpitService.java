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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
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
        List<PhysicalItem> allItems = physicalItemRepository.findCockpitItems(null, null, null);
        Map<Long, List<BorrowRequest>> activeRequestsMap = fetchActiveRequestsMap();
        Map<Long, List<Borrowing>> activeBorrowingsMap = fetchActiveBorrowingsMap();

        LocalDateTime now = LocalDateTime.now();

        long totalCopies = 0;
        long availableCopies = 0;
        long reservedCopies = 0;
        long borrowedCopies = 0;
        long attentionCopies = 0;

        for (PhysicalItem item : allItems) {
            if (item.getInventoryStatus() == InventoryStatus.WITHDRAWN) {
                continue;
            }

            totalCopies++;

            if (item.getInventoryStatus() == InventoryStatus.ACTIVE && item.getCirculationStatus() == CirculationStatus.AVAILABLE) {
                availableCopies++;
            }
            if (item.getCirculationStatus() == CirculationStatus.RESERVED) {
                reservedCopies++;
            }
            if (item.getCirculationStatus() == CirculationStatus.BORROWED) {
                borrowedCopies++;
            }

            CockpitPhysicalItemDto dto = mapToCockpitDto(item, activeRequestsMap, activeBorrowingsMap, now);
            if (dto.isNeedsAttention()) {
                attentionCopies++;
            }
        }

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
        int clampedPage = Math.max(0, page);
        int clampedSize = Math.min(100, Math.max(1, size));
        String trimmedQ = (q != null && !q.isBlank()) ? q.trim() : null;

        List<PhysicalItem> rawItems = physicalItemRepository.findCockpitItems(trimmedQ, inventoryStatus, circulationStatus);
        Map<Long, List<BorrowRequest>> activeRequestsMap = fetchActiveRequestsMap();
        Map<Long, List<Borrowing>> activeBorrowingsMap = fetchActiveBorrowingsMap();

        LocalDateTime now = LocalDateTime.now();

        List<CockpitPhysicalItemDto> dtoList = rawItems.stream()
                .map(item -> mapToCockpitDto(item, activeRequestsMap, activeBorrowingsMap, now))
                .filter(dto -> needsAttention == null || dto.isNeedsAttention() == needsAttention)
                .collect(Collectors.toCollection(ArrayList::new));

        // Sort deterministically per cockpit LLD:
        // 1. needsAttention = true first
        // 2. operational attention severity
        // 3. physicalItem.id ASC tie-breaker
        dtoList.sort((a, b) -> {
            if (a.isNeedsAttention() != b.isNeedsAttention()) {
                return a.isNeedsAttention() ? -1 : 1;
            }
            if (a.isNeedsAttention()) {
                int rankA = getAttentionSeverityRank(a.getAttentionReasons());
                int rankB = getAttentionSeverityRank(b.getAttentionReasons());
                if (rankA != rankB) {
                    return Integer.compare(rankA, rankB);
                }
            }
            return Long.compare(a.getId(), b.getId());
        });

        long totalElements = dtoList.size();
        int totalPages = (int) Math.ceil((double) totalElements / clampedSize);

        int fromIndex = Math.min(clampedPage * clampedSize, (int) totalElements);
        int toIndex = Math.min(fromIndex + clampedSize, (int) totalElements);

        List<CockpitPhysicalItemDto> pageItems = dtoList.subList(fromIndex, toIndex);

        return CockpitPageResponseDto.builder()
                .items(pageItems)
                .page(clampedPage)
                .size(clampedSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

    private Map<Long, List<BorrowRequest>> fetchActiveRequestsMap() {
        return borrowRequestRepository.findActiveForLibrarian(ACTIVE_REQUEST_STATUSES)
                .stream()
                .filter(r -> r.getPhysicalItem() != null)
                .collect(Collectors.groupingBy(r -> r.getPhysicalItem().getId()));
    }

    private Map<Long, List<Borrowing>> fetchActiveBorrowingsMap() {
        return borrowingRepository.findActiveForLibrarian()
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

    private int getAttentionSeverityRank(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return 5;
        }
        if (reasons.contains("BORROWING_OVERDUE")) {
            return 1;
        }
        if (reasons.contains("INVENTORY_LOST") || reasons.contains("INVENTORY_DAMAGED")) {
            return 2;
        }
        if (reasons.contains("LOCATION_UNASSIGNED")) {
            return 3;
        }
        return 4;
    }
}
