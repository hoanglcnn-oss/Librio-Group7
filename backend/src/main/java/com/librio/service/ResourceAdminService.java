package com.librio.service;

import com.librio.domain.DigitalItem;
import com.librio.domain.PhysicalItem;
import com.librio.domain.CirculationStatus;
import com.librio.domain.InventoryStatus;
import com.librio.domain.Resource;
import com.librio.dto.DigitalAvailabilityDto;
import com.librio.dto.ManagedResourceDto;
import com.librio.dto.PhysicalAvailabilityDto;
import com.librio.dto.ResourceAdminRequestDto;
import com.librio.exception.BorrowErrorCode;
import com.librio.exception.BorrowFlowException;
import com.librio.repository.DigitalItemRepository;
import com.librio.repository.PhysicalItemRepository;
import com.librio.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import com.librio.domain.MetadataSource;
import com.librio.util.IsbnUtils;
import com.librio.exception.InvalidIsbnException;
import com.librio.exception.ResourceIsbnExistsException;

import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Quan tri metadata resource va reconcile access records cua librarian.
 *
 * <p>Physical copies duoc quan ly theo exact item rows. Giam so luong chi duoc xoa item AVAILABLE;
 * RESERVED, BORROWED hoac OVERDUE la circulation commitment dang hoat dong nen phai giu lai.
 * Authors hien di qua API dang JSON array nhung van persist dang comma-separated de tuong thich schema.
 */
@Service
@RequiredArgsConstructor
public class ResourceAdminService {
    private static final Set<String> SUPPORTED_ACCESS_TYPES = Set.of("PHYSICAL", "DIGITAL");

    private final ResourceRepository resourceRepository;
    private final PhysicalItemRepository physicalItemRepository;
    private final DigitalItemRepository digitalItemRepository;

    @Transactional(readOnly = true)
    public ManagedResourceDto get(Long resourceId) {
        return toDto(findResource(resourceId));
    }

    @Transactional
        public ManagedResourceDto create(ResourceAdminRequestDto request) {
        ValidatedInput input = validate(request);
        if (input.isbn() != null && resourceRepository.existsByIsbn(input.isbn())) {
            throw new ResourceIsbnExistsException("ISBN already exists");
        }
        Resource resource = Resource.builder()
                .title(input.title())
                .authors(input.authors())
                .description(input.description())
                .category(input.category())
                .isbn(input.isbn())
                .coverImageUrl(input.coverImageUrl())
                .metadataSource(input.metadataSource())
                .externalSourceId(input.externalSourceId())
                .build();
        try {
            resource = resourceRepository.saveAndFlush(resource);
        } catch (DataIntegrityViolationException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("uq_resource_isbn") || msg.contains("isbn")) {
                throw new ResourceIsbnExistsException("ISBN already exists");
            }
            throw e;
        }
        // Reconcile physical/digital access cung transaction voi metadata resource.
        reconcilePhysicalCopies(resource, 0, input.physicalCopies());
        reconcileDigitalItem(resource, input.digital());
        return toDto(resource);
    }

    @Transactional
        public ManagedResourceDto update(Long resourceId, ResourceAdminRequestDto request) {
        Resource resource = findResource(resourceId);
        ValidatedInput input = validate(request);
        if (input.isbn() != null && resourceRepository.existsByIsbnAndIdNot(input.isbn(), resourceId)) {
            throw new ResourceIsbnExistsException("ISBN already exists");
        }
        resource.setTitle(input.title());
        resource.setAuthors(input.authors());
        resource.setDescription(input.description());
        resource.setCategory(input.category());
        resource.setIsbn(input.isbn());
        resource.setCoverImageUrl(input.coverImageUrl());
        resource.setMetadataSource(input.metadataSource());
        resource.setExternalSourceId(input.externalSourceId());

        long currentCopies = physicalItemRepository.countByResourceId(resourceId);
        // Atomic boundary: metadata update va reconcile copy/access cung commit hoac rollback.
        reconcilePhysicalCopies(resource, currentCopies, input.physicalCopies());
        reconcileDigitalItem(resource, input.digital());
        try {
            resourceRepository.saveAndFlush(resource);
        } catch (DataIntegrityViolationException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("uq_resource_isbn") || msg.contains("isbn")) {
                throw new ResourceIsbnExistsException("ISBN already exists");
            }
            throw e;
        }
        return toDto(resource);
    }

    private void reconcilePhysicalCopies(Resource resource, long current, long desired) {
        if (desired > current) {
            for (long index = current; index < desired; index++) {
                PhysicalItem item = physicalItemRepository.save(PhysicalItem.builder()
                        .resource(resource)
                        .barcode("TEMP-" + System.nanoTime() + "-" + index)
                        .location("UNASSIGNED")
                        .inventoryStatus(InventoryStatus.ACTIVE)
                        .circulationStatus(CirculationStatus.AVAILABLE)
                        .build());
                item.setBarcode("LIB-" + item.getId());
                physicalItemRepository.save(item);
            }
            return;
        }
        if (desired == current) {
            return;
        }

        long removeCount = current - desired;
        // Chá»‰ xÃ³a copy AVAILABLE; RESERVED/BORROWED/OVERDUE váº«n lÃ  commitment lÆ°u thÃ´ng cáº§n báº£o toÃ n.
        List<PhysicalItem> available = physicalItemRepository.findForUpdate(
                resource.getId(), InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE,
                PageRequest.of(0, Math.toIntExact(removeCount)));
        if (available.size() < removeCount) {
            throw conflict(BorrowErrorCode.RESOURCE_IN_USE,
                    "Cannot remove reserved, borrowed or overdue physical items");
        }
        physicalItemRepository.deleteAll(available.subList(0, Math.toIntExact(removeCount)));
    }

    private void reconcileDigitalItem(Resource resource, boolean desired) {
        DigitalItem existing = digitalItemRepository.findByResourceId(resource.getId()).orElse(null);
        if (desired && existing == null) {
            digitalItemRepository.save(DigitalItem.builder().resource(resource).build());
        } else if (!desired && existing != null) {
            digitalItemRepository.delete(existing);
        }
    }

    private ManagedResourceDto toDto(Resource resource) {
        long totalCopies = physicalItemRepository.countByResourceId(resource.getId());
        long availableCopies = physicalItemRepository.countByResourceIdAndInventoryStatusAndCirculationStatus(
                resource.getId(), InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);
        boolean digital = digitalItemRepository.existsByResourceId(resource.getId());
        List<String> accessTypes = new ArrayList<>();
        if (totalCopies > 0) accessTypes.add("PHYSICAL");
        if (digital) accessTypes.add("DIGITAL");

        return ManagedResourceDto.builder()
                .id(resource.getId())
                .title(resource.getTitle())
                .authors(parseAuthors(resource.getAuthors()))
                .description(resource.getDescription())
                .category(resource.getCategory())
                .accessTypes(accessTypes)
                .physical(totalCopies > 0 ? PhysicalAvailabilityDto.builder()
                        .totalCopies(totalCopies)
                        .availableCopies(availableCopies)
                        .build() : null)
                .digital(digital ? DigitalAvailabilityDto.builder().available(true).build() : null)
                .isbn(resource.getIsbn())
                .coverImageUrl(resource.getCoverImageUrl())
                .metadataSource(resource.getMetadataSource())
                .externalSourceId(resource.getExternalSourceId())
                .build();
    }

    private ValidatedInput validate(ResourceAdminRequestDto request) {
        Set<String> accessTypes = new LinkedHashSet<>();
        request.getAccessTypes().forEach(value -> accessTypes.add(value == null ? "" : value.trim().toUpperCase()));
        if (accessTypes.isEmpty() || !SUPPORTED_ACCESS_TYPES.containsAll(accessTypes)) {
            throw validation("Unsupported access type");
        }

        // Ranh giá»›i tÆ°Æ¡ng thÃ­ch hiá»‡n táº¡i: API nháº­n authors dáº¡ng array, database lÆ°u chuá»—i phÃ¢n tÃ¡ch báº±ng dáº¥u pháº©y.
        String authors = request.getAuthors().stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow(() -> validation("At least one author is required"));
        if (authors.length() > 255) {
            throw validation("Combined authors must not exceed 255 characters");
        }

        long physicalCopies = accessTypes.contains("PHYSICAL")
                ? request.getPhysical() == null ? -1 : request.getPhysical().getTotalCopies()
                : 0;
        if (physicalCopies < 0 || physicalCopies > 9999) {
            throw validation("Physical totalCopies must be between 0 and 9999");
        }
        if (accessTypes.contains("PHYSICAL") && physicalCopies == 0) {
            throw validation("Physical access requires at least one copy");
        }

        String isbn = blankToNull(request.getIsbn());
        if (isbn != null) {
            try {
                isbn = IsbnUtils.normalizeToIsbn13(isbn);
            } catch (IllegalArgumentException e) {
                throw new InvalidIsbnException("Invalid ISBN format");
            }
        }
        MetadataSource metadataSource = request.getMetadataSource() != null ? request.getMetadataSource() : MetadataSource.MANUAL;

        return new ValidatedInput(
                request.getTitle().trim(),
                authors,
                blankToNull(request.getDescription()),
                blankToNull(request.getCategory()),
                physicalCopies,
                accessTypes.contains("DIGITAL"),
                isbn,
                blankToNull(request.getCoverImageUrl()),
                metadataSource,
                blankToNull(request.getExternalSourceId()));
    }

    private Resource findResource(Long resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> notFound(BorrowErrorCode.RESOURCE_NOT_FOUND, "Resource not found"));
    }

    private List<String> parseAuthors(String authors) {
        return Arrays.stream(authors.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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

    private record ValidatedInput(
            String title,
            String authors,
            String description,
            String category,
            long physicalCopies,
            boolean digital,
            String isbn,
            String coverImageUrl,
            MetadataSource metadataSource,
            String externalSourceId) {
    }
}


