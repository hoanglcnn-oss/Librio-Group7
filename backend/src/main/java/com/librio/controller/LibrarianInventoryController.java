package com.librio.controller;

import com.librio.domain.CirculationStatus;
import com.librio.domain.InventoryStatus;
import com.librio.dto.CockpitPageResponseDto;
import com.librio.dto.InventorySummaryDto;
import com.librio.service.CollectionCockpitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LibrarianInventoryController {

    private final CollectionCockpitService collectionCockpitService;

    @GetMapping("/librarian/inventory/summary")
    public ResponseEntity<InventorySummaryDto> getSummary() {
        return ResponseEntity.ok(collectionCockpitService.getSummary());
    }

    @GetMapping("/librarian/physical-items")
    public ResponseEntity<CockpitPageResponseDto> getPhysicalItems(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) InventoryStatus inventoryStatus,
            @RequestParam(required = false) CirculationStatus circulationStatus,
            @RequestParam(required = false) Boolean needsAttention,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(collectionCockpitService.searchPhysicalItems(
                q, inventoryStatus, circulationStatus, needsAttention, page, size));
    }
}
