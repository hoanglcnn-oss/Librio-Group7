package com.librio.service;

import com.librio.domain.CirculationStatus;
import com.librio.domain.InventoryStatus;
import com.librio.domain.PhysicalItem;
import com.librio.domain.Resource;
import com.librio.dto.CreatePhysicalItemRequestDto;
import com.librio.dto.PhysicalItemDto;
import com.librio.dto.UpdatePhysicalItemRequestDto;
import com.librio.exception.BorrowErrorCode;
import com.librio.exception.BorrowFlowException;
import com.librio.repository.PhysicalItemRepository;
import com.librio.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PhysicalItemService {

    private final ResourceRepository resourceRepository;
    private final PhysicalItemRepository physicalItemRepository;

    @Transactional
    public PhysicalItemDto createPhysicalItem(Long resourceId, CreatePhysicalItemRequestDto request) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BorrowFlowException(
                        BorrowErrorCode.RESOURCE_NOT_FOUND.name(),
                        HttpStatus.NOT_FOUND,
                        "Resource not found with id: " + resourceId));

        String barcode = validateBarcode(request.getBarcode());
        String location = validateLocation(request.getLocation());

        if (physicalItemRepository.existsByBarcode(barcode)) {
            throw new BorrowFlowException(
                    "DUPLICATE_ITEM_BARCODE",
                    HttpStatus.CONFLICT,
                    "Physical item with barcode already exists: " + barcode);
        }

        PhysicalItem item = PhysicalItem.builder()
                .resource(resource)
                .barcode(barcode)
                .location(location)
                .inventoryStatus(InventoryStatus.ACTIVE)
                .circulationStatus(CirculationStatus.AVAILABLE)
                .build();

        try {
            return toDto(physicalItemRepository.saveAndFlush(item));
        } catch (DataIntegrityViolationException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("uq_physical_item_barcode") || msg.contains("barcode")) {
                throw new BorrowFlowException(
                        "DUPLICATE_ITEM_BARCODE",
                        HttpStatus.CONFLICT,
                        "Physical item with barcode already exists: " + barcode);
            }
            throw e;
        }
    }

    @Transactional
    public PhysicalItemDto updatePhysicalItem(Long id, UpdatePhysicalItemRequestDto request) {
        PhysicalItem item = physicalItemRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BorrowFlowException(
                        "PHYSICAL_ITEM_NOT_FOUND",
                        HttpStatus.NOT_FOUND,
                        "Physical item not found with id: " + id));

        String barcode = validateBarcode(request.getBarcode());
        String location = validateLocation(request.getLocation());

        if (physicalItemRepository.existsByBarcodeAndIdNot(barcode, id)) {
            throw new BorrowFlowException(
                    "DUPLICATE_ITEM_BARCODE",
                    HttpStatus.CONFLICT,
                    "Physical item with barcode already exists: " + barcode);
        }

        if (request.getInventoryStatus() != null) {
            validateInventoryStatusTransition(item, request.getInventoryStatus());
            item.setInventoryStatus(request.getInventoryStatus());
        }

        item.setBarcode(barcode);
        item.setLocation(location);

        try {
            return toDto(physicalItemRepository.saveAndFlush(item));
        } catch (DataIntegrityViolationException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("uq_physical_item_barcode") || msg.contains("barcode")) {
                throw new BorrowFlowException(
                        "DUPLICATE_ITEM_BARCODE",
                        HttpStatus.CONFLICT,
                        "Physical item with barcode already exists: " + barcode);
            }
            throw e;
        }
    }

    private void validateInventoryStatusTransition(PhysicalItem item, InventoryStatus targetStatus) {
        InventoryStatus currentStatus = item.getInventoryStatus();
        if (currentStatus == targetStatus) {
            return;
        }

        if (currentStatus == InventoryStatus.ACTIVE && targetStatus != InventoryStatus.ACTIVE) {
            if (item.getCirculationStatus() != CirculationStatus.AVAILABLE) {
                throw new BorrowFlowException(
                        "ACTIVE_CIRCULATION_CONFLICT",
                        HttpStatus.CONFLICT,
                        "Cannot transition physical copy in active circulation ("
                                + item.getCirculationStatus() + ") to " + targetStatus);
            }
        } else if (currentStatus != InventoryStatus.ACTIVE && targetStatus != InventoryStatus.ACTIVE) {
            throw new BorrowFlowException(
                    "INVALID_INVENTORY_TRANSITION",
                    HttpStatus.CONFLICT,
                    "Invalid inventory status transition from " + currentStatus + " to " + targetStatus
                            + ". Must restore to ACTIVE first.");
        }
    }

    private String validateBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            throw new BorrowFlowException(
                    BorrowErrorCode.VALIDATION_ERROR.name(),
                    HttpStatus.BAD_REQUEST,
                    "Barcode is required");
        }
        return barcode.trim();
    }

    private String validateLocation(String location) {
        if (location == null || location.isBlank()) {
            throw new BorrowFlowException(
                    BorrowErrorCode.VALIDATION_ERROR.name(),
                    HttpStatus.BAD_REQUEST,
                    "Location is required");
        }
        return location.trim();
    }

    private PhysicalItemDto toDto(PhysicalItem item) {
        return PhysicalItemDto.builder()
                .id(item.getId())
                .resourceId(item.getResource().getId())
                .barcode(item.getBarcode())
                .location(item.getLocation())
                .inventoryStatus(item.getInventoryStatus())
                .circulationStatus(item.getCirculationStatus())
                .build();
    }
}
