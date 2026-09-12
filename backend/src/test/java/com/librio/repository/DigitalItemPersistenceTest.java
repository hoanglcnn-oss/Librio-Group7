package com.librio.repository;

import com.librio.domain.DigitalItem;
import com.librio.domain.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DigitalItemPersistenceTest {

    @Autowired
    private DigitalItemRepository digitalItemRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    private Resource createAndSaveResource() {
        Resource r = Resource.builder()
                .title("Test Resource")
                .authors("Test Author")
                .build();
        return resourceRepository.saveAndFlush(r);
    }

    @Test
    @DisplayName("DigitalItem can persist with previewContentKey = null, fullContentKey = null")
    void testPersistBothNull() {
        Resource r = createAndSaveResource();
        DigitalItem di = DigitalItem.builder().resource(r).build();
        
        DigitalItem saved = digitalItemRepository.saveAndFlush(di);
        
        assertNotNull(saved.getId());
        assertNull(saved.getPreviewContentKey());
        assertNull(saved.getFullContentKey());
    }

    @Test
    @DisplayName("DigitalItem can persist with previewContentKey set, fullContentKey = null")
    void testPersistPreviewOnly() {
        Resource r = createAndSaveResource();
        DigitalItem di = DigitalItem.builder()
                .resource(r)
                .previewContentKey("preview-ref-123")
                .build();
        
        DigitalItem saved = digitalItemRepository.saveAndFlush(di);
        
        assertNotNull(saved.getId());
        assertEquals("preview-ref-123", saved.getPreviewContentKey());
        assertNull(saved.getFullContentKey());
    }

    @Test
    @DisplayName("DigitalItem can persist with previewContentKey = null, fullContentKey set")
    void testPersistFullOnly() {
        Resource r = createAndSaveResource();
        DigitalItem di = DigitalItem.builder()
                .resource(r)
                .fullContentKey("full-ref-123")
                .build();
        
        DigitalItem saved = digitalItemRepository.saveAndFlush(di);
        
        assertNotNull(saved.getId());
        assertNull(saved.getPreviewContentKey());
        assertEquals("full-ref-123", saved.getFullContentKey());
    }

    @Test
    @DisplayName("DigitalItem can persist with both keys set")
    void testPersistBothKeys() {
        Resource r = createAndSaveResource();
        DigitalItem di = DigitalItem.builder()
                .resource(r)
                .previewContentKey("preview-ref-123")
                .fullContentKey("full-ref-123")
                .build();
        
        DigitalItem saved = digitalItemRepository.saveAndFlush(di);
        
        assertNotNull(saved.getId());
        assertEquals("preview-ref-123", saved.getPreviewContentKey());
        assertEquals("full-ref-123", saved.getFullContentKey());
    }

    @Test
    @DisplayName("Existing one-DigitalItem-per-Resource invariant remains intact")
    void testUniqueResourceIdConstraint() {
        Resource r = createAndSaveResource();
        
        DigitalItem di1 = DigitalItem.builder().resource(r).build();
        digitalItemRepository.saveAndFlush(di1);
        
        DigitalItem di2 = DigitalItem.builder().resource(r).build();
        assertThrows(DataIntegrityViolationException.class, () -> {
            digitalItemRepository.saveAndFlush(di2);
        });
    }
}