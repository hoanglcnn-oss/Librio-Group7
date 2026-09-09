package com.librio.repository;

import com.librio.domain.MetadataSource;
import com.librio.domain.Resource;
import com.librio.dto.ResourceDetailDto;
import com.librio.dto.ResourceListResponseDto;
import com.librio.service.ResourceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ResourceRepositoryTest {

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private PhysicalItemRepository physicalItemRepository;

    @Autowired
    private DigitalItemRepository digitalItemRepository;

    @Autowired
    private ResourceService resourceService;

    @Test
    @DisplayName("Seed Case 1: Physical Available + Digital Available (Clean Code)")
    void testSeedCase1_PhysicalAvailableAndDigital() {
        ResourceDetailDto detail = resourceService.getResourceById(1L);

        assertEquals("Clean Code", detail.getTitle());
        assertTrue(detail.getAccessTypes().contains("PHYSICAL"));
        assertTrue(detail.getAccessTypes().contains("DIGITAL"));
        
        assertNotNull(detail.getPhysical());
        assertEquals(5, detail.getPhysical().getTotalCopies());
        assertEquals(2, detail.getPhysical().getAvailableCopies());

        assertNotNull(detail.getDigital());
        assertTrue(detail.getDigital().isAvailable());
    }

    @Test
    @DisplayName("Seed Case 2: Physical Out of Stock & Omit Digital (Refactoring)")
    void testSeedCase2_PhysicalOutOfStock() {
        ResourceDetailDto detail = resourceService.getResourceById(2L);

        assertEquals("Refactoring", detail.getTitle());
        assertTrue(detail.getAccessTypes().contains("PHYSICAL"));
        assertFalse(detail.getAccessTypes().contains("DIGITAL"));

        assertNotNull(detail.getPhysical());
        assertEquals(3, detail.getPhysical().getTotalCopies());
        assertEquals(0, detail.getPhysical().getAvailableCopies());

        assertNull(detail.getDigital());
    }

    @Test
    @DisplayName("Seed Case 3: Digital Only & Omit Physical (Designing Data-Intensive Applications)")
    void testSeedCase3_DigitalOnly() {
        ResourceDetailDto detail = resourceService.getResourceById(3L);

        assertEquals("Designing Data-Intensive Applications", detail.getTitle());
        assertFalse(detail.getAccessTypes().contains("PHYSICAL"));
        assertTrue(detail.getAccessTypes().contains("DIGITAL"));

        assertNull(detail.getPhysical());
        assertNotNull(detail.getDigital());
        assertTrue(detail.getDigital().isAvailable());
    }

    @Test
    @DisplayName("Seed Case 4: Mixed Physical & Digital (SICP)")
    void testSeedCase4_MixedPhysicalAndDigital() {
        ResourceDetailDto detail = resourceService.getResourceById(4L);

        assertEquals("Structure and Interpretation of Computer Programs", detail.getTitle());
        assertTrue(detail.getAccessTypes().contains("PHYSICAL"));
        assertTrue(detail.getAccessTypes().contains("DIGITAL"));

        assertNotNull(detail.getPhysical());
        assertEquals(2, detail.getPhysical().getTotalCopies());
        assertEquals(1, detail.getPhysical().getAvailableCopies());

        assertNotNull(detail.getDigital());
        assertTrue(detail.getDigital().isAvailable());
    }

    @Test
    @DisplayName("Search with keyword returns matching items")
    void testSearchByKeyword() {
        ResourceListResponseDto result = resourceService.searchResources("Clean");
        assertEquals(1, result.getItems().size());
        assertEquals("Clean Code", result.getItems().get(0).getTitle());
    }

    @Test
    @DisplayName("Search with empty keyword returns all default items")
    void testSearchEmptyKeyword() {
        ResourceListResponseDto result = resourceService.searchResources("   ");
        assertEquals(4, result.getItems().size());
    }

    @Test
    @DisplayName("T-194: Default metadataSource is MANUAL for new resources and seeded items")
    void testDefaultMetadataSourceIsManual() {
        Resource seeded = resourceRepository.findById(1L).orElseThrow();
        assertEquals(MetadataSource.MANUAL, seeded.getMetadataSource());

        Resource newResource = Resource.builder()
                .title("Test Manual Book")
                .authors("Author X")
                .build();
        Resource saved = resourceRepository.save(newResource);

        assertNotNull(saved.getId());
        assertEquals(MetadataSource.MANUAL, saved.getMetadataSource());
        assertNull(saved.getIsbn());
        assertNull(saved.getCoverImageUrl());
        assertNull(saved.getExternalSourceId());
    }

    @Test
    @DisplayName("T-194: Persist resource with ISBN, coverImageUrl, GOOGLE_BOOKS metadataSource, externalSourceId")
    void testSaveAndFindByIsbnAndMetadataSource() {
        String isbn13 = "9780132350884";
        Resource resource = Resource.builder()
                .title("Clean Code (Google Books edition)")
                .authors("Robert C. Martin")
                .isbn(isbn13)
                .coverImageUrl("https://books.google.com/books/content?id=vol123&printsec=frontcover")
                .metadataSource(MetadataSource.GOOGLE_BOOKS)
                .externalSourceId("vol123")
                .build();

        Resource saved = resourceRepository.save(resource);
        assertNotNull(saved.getId());

        Optional<Resource> fetchedOpt = resourceRepository.findByIsbn(isbn13);
        assertTrue(fetchedOpt.isPresent());
        Resource fetched = fetchedOpt.get();
        assertEquals("Clean Code (Google Books edition)", fetched.getTitle());
        assertEquals(isbn13, fetched.getIsbn());
        assertEquals("https://books.google.com/books/content?id=vol123&printsec=frontcover", fetched.getCoverImageUrl());
        assertEquals(MetadataSource.GOOGLE_BOOKS, fetched.getMetadataSource());
        assertEquals("vol123", fetched.getExternalSourceId());

        assertTrue(resourceRepository.existsByIsbn(isbn13));
        assertFalse(resourceRepository.existsByIsbnAndIdNot(isbn13, saved.getId()));
        assertTrue(resourceRepository.existsByIsbnAndIdNot(isbn13, 99999L));
    }

    @Test
    @DisplayName("T-194: Enforce UNIQUE constraint on non-null ISBN")
    void testUniqueIsbnConstraint() {
        String duplicateIsbn = "9780321356680";
        Resource r1 = Resource.builder()
                .title("Effective Java 1st Edition")
                .authors("Joshua Bloch")
                .isbn(duplicateIsbn)
                .build();
        resourceRepository.saveAndFlush(r1);

        Resource r2 = Resource.builder()
                .title("Effective Java 2nd Edition")
                .authors("Joshua Bloch")
                .isbn(duplicateIsbn)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> resourceRepository.saveAndFlush(r2));
    }

    @Test
    @DisplayName("T-194: Null ISBNs are allowed for multiple resources")
    void testMultipleNullIsbnsAllowed() {
        Resource r1 = Resource.builder()
                .title("Book Without ISBN 1")
                .authors("Author A")
                .isbn(null)
                .build();
        Resource r2 = Resource.builder()
                .title("Book Without ISBN 2")
                .authors("Author B")
                .isbn(null)
                .build();

        assertDoesNotThrow(() -> {
            resourceRepository.save(r1);
            resourceRepository.save(r2);
            resourceRepository.flush();
        });
    }
}
