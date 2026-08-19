package com.librio.service;

import com.librio.domain.PhysicalItemStatus;
import com.librio.domain.Resource;
import com.librio.dto.*;
import com.librio.exception.ResourceNotFoundException;
import com.librio.repository.DigitalItemRepository;
import com.librio.repository.PhysicalItemRepository;
import com.librio.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final PhysicalItemRepository physicalItemRepository;
    private final DigitalItemRepository digitalItemRepository;

    @Transactional(readOnly = true)
    public ResourceListResponseDto searchResources(String q) {
        String trimmedQuery = (q != null) ? q.trim() : "";
        List<Resource> resources;

        if (trimmedQuery.isEmpty()) {
            resources = resourceRepository.findAll();
        } else {
            resources = resourceRepository.searchByKeyword(trimmedQuery);
        }

        List<ResourceSummaryDto> items = resources.stream()
                .map(this::mapToSummaryDto)
                .collect(Collectors.toList());

        return new ResourceListResponseDto(items);
    }

    @Transactional(readOnly = true)
    public ResourceDetailDto getResourceById(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        long totalCopies = physicalItemRepository.countByResourceId(id);
        long availableCopies = physicalItemRepository.countByResourceIdAndStatus(id, PhysicalItemStatus.AVAILABLE);
        boolean digitalAvailable = digitalItemRepository.existsByResourceId(id);

        List<String> accessTypes = new ArrayList<>();
        PhysicalAvailabilityDto physicalDto = null;
        DigitalAvailabilityDto digitalDto = null;

        if (totalCopies > 0) {
            accessTypes.add("PHYSICAL");
            physicalDto = PhysicalAvailabilityDto.builder()
                    .totalCopies(totalCopies)
                    .availableCopies(availableCopies)
                    .build();
        }

        if (digitalAvailable) {
            accessTypes.add("DIGITAL");
            digitalDto = DigitalAvailabilityDto.builder()
                    .available(true)
                    .build();
        }

        return ResourceDetailDto.builder()
                .id(resource.getId())
                .title(resource.getTitle())
                .authors(parseAuthors(resource.getAuthors()))
                .description(resource.getDescription())
                .accessTypes(accessTypes)
                .physical(physicalDto)
                .digital(digitalDto)
                .build();
    }

    private ResourceSummaryDto mapToSummaryDto(Resource resource) {
        return ResourceSummaryDto.builder()
                .id(resource.getId())
                .title(resource.getTitle())
                .authors(parseAuthors(resource.getAuthors()))
                .build();
    }

    private List<String> parseAuthors(String authorsString) {
        if (authorsString == null || authorsString.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(authorsString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
