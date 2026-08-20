package com.librio.repository;

import com.librio.domain.PhysicalItemStatus;
import com.librio.domain.Resource;
import com.librio.dto.ResourceDetailDto;
import com.librio.dto.ResourceListResponseDto;
import com.librio.service.ResourceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
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
}
