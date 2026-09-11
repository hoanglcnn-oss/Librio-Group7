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
}
