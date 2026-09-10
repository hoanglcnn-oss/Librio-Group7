package com.librio.controller;

import com.librio.dto.PhysicalItemDto;
import com.librio.dto.PhysicalItemRequestDto;
import com.librio.service.PhysicalItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LibrarianPhysicalItemController {

    private final PhysicalItemService physicalItemService;

    @PostMapping("/librarian/resources/{resourceId}/physical-items")
    public ResponseEntity<PhysicalItemDto> create(
            @PathVariable Long resourceId,
            @Valid @RequestBody PhysicalItemRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(physicalItemService.createPhysicalItem(resourceId, request));
    }

    @PutMapping("/librarian/physical-items/{id}")
    public ResponseEntity<PhysicalItemDto> update(
            @PathVariable Long id,
            @Valid @RequestBody PhysicalItemRequestDto request) {
        return ResponseEntity.ok(physicalItemService.updatePhysicalItem(id, request));
    }
}
