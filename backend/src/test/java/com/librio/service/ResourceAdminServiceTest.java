package com.librio.service;

import com.librio.domain.MetadataSource;
import com.librio.domain.Resource;
import com.librio.dto.ManagedResourceDto;
import com.librio.dto.ResourceAdminRequestDto;
import com.librio.exception.InvalidIsbnException;
import com.librio.exception.ResourceIsbnExistsException;
import com.librio.repository.ResourceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ResourceAdminServiceTest {

    @Autowired
    private ResourceAdminService resourceAdminService;

    @Autowired
    private ResourceRepository resourceRepository;

    @Test
    void create_manualWithoutIsbn_setsMetadataSourceManual() {
        ResourceAdminRequestDto request = new ResourceAdminRequestDto();
        request.setTitle("Manual Book");
        request.setAuthors(List.of("Author One"));
        request.setDescription("A great book");
        request.setCategory("Science");
        request.setAccessTypes(List.of("PHYSICAL"));
        ResourceAdminRequestDto.PhysicalInput physical = new ResourceAdminRequestDto.PhysicalInput();
        physical.setTotalCopies(1);
        request.setPhysical(physical);

        ManagedResourceDto response = resourceAdminService.create(request);

        assertThat(response.getIsbn()).isNull();
        assertThat(response.getMetadataSource()).isEqualTo(MetadataSource.MANUAL);

        Resource entity = resourceRepository.findById(response.getId()).orElseThrow();
        assertThat(entity.getIsbn()).isNull();
        assertThat(entity.getMetadataSource()).isEqualTo(MetadataSource.MANUAL);
    }

    @Test
    void create_validIsbn13_normalizesAndPersists() {
        ResourceAdminRequestDto request = new ResourceAdminRequestDto();
        request.setTitle("Book Two");
        request.setAuthors(List.of("Author Two"));
        request.setAccessTypes(List.of("DIGITAL"));
        request.setIsbn("978-0-13-235088-4"); // format
        request.setCoverImageUrl("https://example.com/cover.jpg");
        request.setMetadataSource(MetadataSource.GOOGLE_BOOKS);
        request.setExternalSourceId("xyz123");

        ManagedResourceDto response = resourceAdminService.create(request);

        assertThat(response.getIsbn()).isEqualTo("9780132350884");
        assertThat(response.getCoverImageUrl()).isEqualTo("https://example.com/cover.jpg");
        assertThat(response.getMetadataSource()).isEqualTo(MetadataSource.GOOGLE_BOOKS);
        assertThat(response.getExternalSourceId()).isEqualTo("xyz123");

        Resource entity = resourceRepository.findById(response.getId()).orElseThrow();
        assertThat(entity.getIsbn()).isEqualTo("9780132350884");
        assertThat(entity.getCoverImageUrl()).isEqualTo("https://example.com/cover.jpg");
    }

    @Test
    void create_isbn10_convertsToIsbn13() {
        ResourceAdminRequestDto request = new ResourceAdminRequestDto();
        request.setTitle("Book Three");
        request.setAuthors(List.of("Author Three"));
        request.setAccessTypes(List.of("DIGITAL"));
        request.setIsbn("0-13-235088-2"); // isbn 10

        ManagedResourceDto response = resourceAdminService.create(request);

        assertThat(response.getIsbn()).isEqualTo("9780132350884");
    }

    @Test
    void create_invalidIsbn_throws400() {
        ResourceAdminRequestDto request = new ResourceAdminRequestDto();
        request.setTitle("Book Four");
        request.setAuthors(List.of("Author Four"));
        request.setAccessTypes(List.of("DIGITAL"));
        request.setIsbn("invalid");

        assertThatThrownBy(() -> resourceAdminService.create(request))
                .isInstanceOf(InvalidIsbnException.class)
                .hasMessageContaining("Invalid ISBN format");
    }

    @Test
    void create_duplicateIsbn_throws409() {
        ResourceAdminRequestDto request1 = new ResourceAdminRequestDto();
        request1.setTitle("Book Five");
        request1.setAuthors(List.of("Author"));
        request1.setAccessTypes(List.of("DIGITAL"));
        request1.setIsbn("9780132350884");
        resourceAdminService.create(request1);

        ResourceAdminRequestDto request2 = new ResourceAdminRequestDto();
        request2.setTitle("Book Six");
        request2.setAuthors(List.of("Author"));
        request2.setAccessTypes(List.of("DIGITAL"));
        request2.setIsbn("978-0-13-235088-4"); // Same underlying ISBN

        assertThatThrownBy(() -> resourceAdminService.create(request2))
                .isInstanceOf(ResourceIsbnExistsException.class)
                .hasMessageContaining("ISBN already exists");
    }

    @Test
    void update_sameIsbn_succeeds() {
        ResourceAdminRequestDto request = new ResourceAdminRequestDto();
        request.setTitle("Book Seven");
        request.setAuthors(List.of("Author"));
        request.setAccessTypes(List.of("DIGITAL"));
        request.setIsbn("9780132350884");
        ManagedResourceDto created = resourceAdminService.create(request);

        request.setTitle("Book Seven Updated"); // change title, keep isbn
        ManagedResourceDto updated = resourceAdminService.update(created.getId(), request);

        assertThat(updated.getTitle()).isEqualTo("Book Seven Updated");
        assertThat(updated.getIsbn()).isEqualTo("9780132350884");
    }

    @Test
    void update_duplicateIsbn_throws409() {
        ResourceAdminRequestDto request1 = new ResourceAdminRequestDto();
        request1.setTitle("Book A");
        request1.setAuthors(List.of("A"));
        request1.setAccessTypes(List.of("DIGITAL"));
        request1.setIsbn("9780132350884");
        resourceAdminService.create(request1);

        ResourceAdminRequestDto request2 = new ResourceAdminRequestDto();
        request2.setTitle("Book B");
        request2.setAuthors(List.of("B"));
        request2.setAccessTypes(List.of("DIGITAL"));
        request2.setIsbn("9781234567897");
        ManagedResourceDto created2 = resourceAdminService.create(request2);

        // Update Book B to use Book A's ISBN
        request2.setIsbn("9780132350884");
        assertThatThrownBy(() -> resourceAdminService.update(created2.getId(), request2))
                .isInstanceOf(ResourceIsbnExistsException.class)
                .hasMessageContaining("ISBN already exists");
    }

    @Autowired
    private com.librio.repository.PhysicalItemRepository physicalItemRepository;

    @Test
    void update_quantityDecrease_withdrawsEligibleCopies() {
        ResourceAdminRequestDto request = new ResourceAdminRequestDto();
        request.setTitle("Withdraw Test");
        request.setAuthors(List.of("Author"));
        request.setAccessTypes(List.of("PHYSICAL"));
        ResourceAdminRequestDto.PhysicalInput physical = new ResourceAdminRequestDto.PhysicalInput();
        physical.setTotalCopies(3);
        request.setPhysical(physical);
        ManagedResourceDto created = resourceAdminService.create(request);

        assertThat(created.getPhysical().getTotalCopies()).isEqualTo(3);
        
        // Decrease to 1
        physical.setTotalCopies(1);
        ManagedResourceDto updated = resourceAdminService.update(created.getId(), request);

        assertThat(updated.getPhysical().getTotalCopies()).isEqualTo(1);
        
        // Database should still contain 3 rows (1 ACTIVE, 2 WITHDRAWN)
        long totalRows = physicalItemRepository.countByResourceId(created.getId());
        assertThat(totalRows).isEqualTo(3);
        
        long withdrawnRows = physicalItemRepository.countByResourceIdAndInventoryStatusNot(created.getId(), com.librio.domain.InventoryStatus.WITHDRAWN);
        assertThat(withdrawnRows).isEqualTo(1);
    }

    @Test
    void update_secondReconciliation_createsCorrectDelta() {
        ResourceAdminRequestDto request = new ResourceAdminRequestDto();
        request.setTitle("Delta Test");
        request.setAuthors(List.of("Author"));
        request.setAccessTypes(List.of("PHYSICAL"));
        ResourceAdminRequestDto.PhysicalInput physical = new ResourceAdminRequestDto.PhysicalInput();
        physical.setTotalCopies(2);
        request.setPhysical(physical);
        ManagedResourceDto created = resourceAdminService.create(request);

        // Withdraw 1 copy
        physical.setTotalCopies(1);
        resourceAdminService.update(created.getId(), request);

        // Increase back to 2 copies
        physical.setTotalCopies(2);
        ManagedResourceDto updated = resourceAdminService.update(created.getId(), request);

        assertThat(updated.getPhysical().getTotalCopies()).isEqualTo(2);
        
        // Total rows should be 3 (2 ACTIVE, 1 WITHDRAWN)
        long totalRows = physicalItemRepository.countByResourceId(created.getId());
        assertThat(totalRows).isEqualTo(3);
    }

    @Test
    void update_activeCirculation_isNotWithdrawn() {
        ResourceAdminRequestDto request = new ResourceAdminRequestDto();
        request.setTitle("Circulation Test");
        request.setAuthors(List.of("Author"));
        request.setAccessTypes(List.of("PHYSICAL"));
        ResourceAdminRequestDto.PhysicalInput physical = new ResourceAdminRequestDto.PhysicalInput();
        physical.setTotalCopies(2);
        request.setPhysical(physical);
        ManagedResourceDto created = resourceAdminService.create(request);

        // Manually set one to BORROWED
        List<com.librio.domain.PhysicalItem> items = physicalItemRepository.findByResourceId(created.getId());
        com.librio.domain.PhysicalItem first = items.get(0);
        first.setCirculationStatus(com.librio.domain.CirculationStatus.BORROWED);
        physicalItemRepository.save(first);

        // Try to decrease to 0
        physical.setTotalCopies(0);
        assertThatThrownBy(() -> resourceAdminService.update(created.getId(), request))
                .isInstanceOf(com.librio.exception.BorrowFlowException.class)
                .hasMessageContaining("Cannot remove reserved, borrowed or overdue physical items");

        // Try to decrease to 1 (should withdraw the AVAILABLE one)
        physical.setTotalCopies(1);
        ManagedResourceDto updated = resourceAdminService.update(created.getId(), request);
        assertThat(updated.getPhysical().getTotalCopies()).isEqualTo(1);
        
        // The borrowed item should still be ACTIVE
        com.librio.domain.PhysicalItem updatedFirst = physicalItemRepository.findById(first.getId()).orElseThrow();
        assertThat(updatedFirst.getInventoryStatus()).isEqualTo(com.librio.domain.InventoryStatus.ACTIVE);
        assertThat(updatedFirst.getCirculationStatus()).isEqualTo(com.librio.domain.CirculationStatus.BORROWED);
    }
}

