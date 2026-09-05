package com.librio.service;

import com.librio.domain.DigitalItem;
import com.librio.domain.PhysicalItem;
import com.librio.domain.PhysicalItemStatus;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Quản trị metadata resource và reconcile access records của librarian.
 *
 * <p>Physical copies được quản lý theo exact item rows. Giảm số lượng chỉ được xóa item AVAILABLE;
 * RESERVED, BORROWED hoặc OVERDUE là circulation commitment đang hoạt động nên phải giữ lại.
 * Authors hiện đi qua API dạng JSON array nhưng vẫn persist dạng comma-separated để tương thích schema.
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
        Resource resource = Resource.builder()
                .title(input.title())
                .authors(input.authors())
                .description(input.description())
                .category(input.category())
                .build();
        resource = resourceRepository.save(resource);
        // Reconcile physical/digital access nằm trong cùng transaction với metadata resource.
        reconcilePhysicalCopies(resource, 0, input.physicalCopies());
        reconcileDigitalItem(resource, input.digital());
        return toDto(resource);
    }

    @Transactional
    public ManagedResourceDto update(Long resourceId, ResourceAdminRequestDto request) {
        Resource resource = findResource(resourceId);
        ValidatedInput input = validate(request);
        resource.setTitle(input.title());
        resource.setAuthors(input.authors());
        resource.setDescription(input.description());
        resource.setCategory(input.category());

        long currentCopies = physicalItemRepository.countByResourceId(resourceId);
        // Atomic boundary: metadata update và reconcile copy/access cùng commit hoặc cùng rollback.
        reconcilePhysicalCopies(resource, currentCopies, input.physicalCopies());
        reconcileDigitalItem(resource, input.digital());
        return toDto(resource);
    }

    private void reconcilePhysicalCopies(Resource resource, long current, long desired) {
        if (desired > current) {
            for (long index = current; index < desired; index++) {
                physicalItemRepository.save(PhysicalItem.builder()
                        .resource(resource)
                        .status(PhysicalItemStatus.AVAILABLE)
                        .build());
            }
            return;
        }
        if (desired == current) {
            return;
        }

        long removeCount = current - desired;
        // Chỉ xóa copy AVAILABLE; RESERVED/BORROWED/OVERDUE vẫn là commitment lưu thông cần bảo toàn.
        List<PhysicalItem> available = physicalItemRepository.findForUpdate(
                resource.getId(), PhysicalItemStatus.AVAILABLE,
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
        long availableCopies = physicalItemRepository.countByResourceIdAndStatus(
                resource.getId(), PhysicalItemStatus.AVAILABLE);
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
                .build();
    }

    private ValidatedInput validate(ResourceAdminRequestDto request) {
        Set<String> accessTypes = new LinkedHashSet<>();
        request.getAccessTypes().forEach(value -> accessTypes.add(value == null ? "" : value.trim().toUpperCase()));
        if (accessTypes.isEmpty() || !SUPPORTED_ACCESS_TYPES.containsAll(accessTypes)) {
            throw validation("Unsupported access type");
        }

        // Ranh giới tương thích hiện tại: API nhận authors dạng array, database lưu chuỗi phân tách bằng dấu phẩy.
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

        return new ValidatedInput(
                request.getTitle().trim(),
                authors,
                blankToNull(request.getDescription()),
                blankToNull(request.getCategory()),
                physicalCopies,
                accessTypes.contains("DIGITAL"));
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
            boolean digital) {
    }
}
