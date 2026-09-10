package com.librio.controller;

import com.librio.domain.Account;
import com.librio.domain.AccountRole;
import com.librio.domain.AccountStatus;
import com.librio.domain.CirculationStatus;
import com.librio.domain.InventoryStatus;
import com.librio.domain.PhysicalItem;
import com.librio.domain.Resource;
import com.librio.repository.AccountRepository;
import com.librio.repository.PhysicalItemRepository;
import com.librio.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LibrarianPhysicalItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @SpyBean
    private PhysicalItemRepository physicalItemRepository;

    private Resource sampleResource;

    @BeforeEach
    void setUp() {
        createAccount("librarian@test.local", AccountRole.LIBRARIAN);
        createAccount("reader@test.local", AccountRole.READER);

        sampleResource = resourceRepository.save(Resource.builder()
                .title("Inventory Administration Patterns")
                .authors("Martin Fowler")
                .description("Guide to physical copy management.")
                .category("Engineering")
                .build());
    }

    @Test
    @DisplayName("Create physical copy succeeds with ACTIVE and AVAILABLE statuses and ignores extraneous fields")
    @WithMockUser(username = "librarian@test.local", roles = "LIBRARIAN")
    void testCreatePhysicalCopy_Success() throws Exception {
        mockMvc.perform(post("/librarian/resources/" + sampleResource.getId() + "/physical-items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "barcode": "LIB-NEW-001",
                                  "location": "Shelf A-1",
                                  "inventoryStatus": "DAMAGED"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.resourceId").value(sampleResource.getId()))
                .andExpect(jsonPath("$.barcode").value("LIB-NEW-001"))
                .andExpect(jsonPath("$.location").value("Shelf A-1"))
                .andExpect(jsonPath("$.inventoryStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.circulationStatus").value("AVAILABLE"));
    }

    @Test
    @DisplayName("Create physical copy fails when barcode is duplicate")
    @WithMockUser(username = "librarian@test.local", roles = "LIBRARIAN")
    void testCreatePhysicalCopy_DuplicateBarcode() throws Exception {
        createPhysicalCopy("LIB-EXISTING-100", "Shelf B-2", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);

        mockMvc.perform(post("/librarian/resources/" + sampleResource.getId() + "/physical-items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "barcode": "LIB-EXISTING-100",
                                  "location": "Shelf C-3"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ITEM_BARCODE"));
    }

    @Test
    @DisplayName("Create physical copy fails when resource is not found")
    @WithMockUser(username = "librarian@test.local", roles = "LIBRARIAN")
    void testCreatePhysicalCopy_ResourceNotFound() throws Exception {
        mockMvc.perform(post("/librarian/resources/99999/physical-items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "barcode": "LIB-99999",
                                  "location": "Shelf Z"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Update physical copy uses pessimistic lock findByIdForUpdate and succeeds")
    @WithMockUser(username = "librarian@test.local", roles = "LIBRARIAN")
    void testUpdatePhysicalCopy_MetadataSuccessAndUsesFindByIdForUpdate() throws Exception {
        PhysicalItem item = createPhysicalCopy("LIB-UPDATE-101", "Old Location", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);

        mockMvc.perform(put("/librarian/physical-items/" + item.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "barcode": "LIB-UPDATE-101-RENAMED",
                                  "location": "New Location B-4"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(item.getId()))
                .andExpect(jsonPath("$.barcode").value("LIB-UPDATE-101-RENAMED"))
                .andExpect(jsonPath("$.location").value("New Location B-4"))
                .andExpect(jsonPath("$.inventoryStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.circulationStatus").value("AVAILABLE"));

        verify(physicalItemRepository).findByIdForUpdate(item.getId());
    }

    @Test
    @DisplayName("Concurrent duplicate barcode database exception normalizes to 409 DUPLICATE_ITEM_BARCODE")
    @WithMockUser(username = "librarian@test.local", roles = "LIBRARIAN")
    void testDuplicateBarcode_DatabaseIntegrityViolationNormalizedTo409() throws Exception {
        PhysicalItem item = createPhysicalCopy("LIB-RACE-BARCODE", "Loc R", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);

        Mockito.doThrow(new DataIntegrityViolationException("uq_physical_item_barcode violation"))
                .when(physicalItemRepository).saveAndFlush(any());

        mockMvc.perform(put("/librarian/physical-items/" + item.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "barcode": "LIB-RACE-BARCODE-CONCURRENT",
                                  "location": "Loc R"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ITEM_BARCODE"));
    }

    @Test
    @DisplayName("Update physical copy fails when target barcode belongs to another item")
    @WithMockUser(username = "librarian@test.local", roles = "LIBRARIAN")
    void testUpdatePhysicalCopy_DuplicateBarcode() throws Exception {
        createPhysicalCopy("LIB-BARCODE-A", "Loc A", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);
        PhysicalItem itemB = createPhysicalCopy("LIB-BARCODE-B", "Loc B", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);

        mockMvc.perform(put("/librarian/physical-items/" + itemB.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "barcode": "LIB-BARCODE-A",
                                  "location": "Loc B"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ITEM_BARCODE"));
    }

    @Test
    @DisplayName("Allowed inventory transition from ACTIVE to DAMAGED when copy is AVAILABLE")
    @WithMockUser(username = "librarian@test.local", roles = "LIBRARIAN")
    void testUpdatePhysicalCopy_AllowedTransition_ActiveToDamaged() throws Exception {
        PhysicalItem item = createPhysicalCopy("LIB-TRANS-1", "Loc 1", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);

        mockMvc.perform(put("/librarian/physical-items/" + item.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "barcode": "LIB-TRANS-1",
                                  "location": "Loc 1",
                                  "inventoryStatus": "DAMAGED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventoryStatus").value("DAMAGED"))
                .andExpect(jsonPath("$.circulationStatus").value("AVAILABLE"));
    }

    @Test
    @DisplayName("Allowed inventory transition restoring DAMAGED copy back to ACTIVE")
    @WithMockUser(username = "librarian@test.local", roles = "LIBRARIAN")
    void testUpdatePhysicalCopy_AllowedTransition_RestoringToActive() throws Exception {
        PhysicalItem item = createPhysicalCopy("LIB-RESTORE-1", "Repair Station", InventoryStatus.DAMAGED, CirculationStatus.AVAILABLE);

        mockMvc.perform(put("/librarian/physical-items/" + item.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "barcode": "LIB-RESTORE-1",
                                  "location": "Shelf A-1",
                                  "inventoryStatus": "ACTIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventoryStatus").value("ACTIVE"));
    }

    @Test
    @DisplayName("Blocked inventory transition from ACTIVE to LOST while item is RESERVED")
    @WithMockUser(username = "librarian@test.local", roles = "LIBRARIAN")
    void testUpdatePhysicalCopy_BlockedTransition_Reserved() throws Exception {
        PhysicalItem item = createPhysicalCopy("LIB-RESERVED-1", "Hold Shelf", InventoryStatus.ACTIVE, CirculationStatus.RESERVED);

        mockMvc.perform(put("/librarian/physical-items/" + item.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "barcode": "LIB-RESERVED-1",
                                  "location": "Hold Shelf",
                                  "inventoryStatus": "LOST"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTIVE_CIRCULATION_CONFLICT"));
    }

    @Test
    @DisplayName("Blocked inventory transition from ACTIVE to WITHDRAWN while item is BORROWED")
    @WithMockUser(username = "librarian@test.local", roles = "LIBRARIAN")
    void testUpdatePhysicalCopy_BlockedTransition_Borrowed() throws Exception {
        PhysicalItem item = createPhysicalCopy("LIB-BORROWED-1", "On Loan", InventoryStatus.ACTIVE, CirculationStatus.BORROWED);

        mockMvc.perform(put("/librarian/physical-items/" + item.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "barcode": "LIB-BORROWED-1",
                                  "location": "On Loan",
                                  "inventoryStatus": "WITHDRAWN"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTIVE_CIRCULATION_CONFLICT"));
    }

    @Test
    @DisplayName("Invalid inventory transition from LOST to DAMAGED directly (must restore to ACTIVE first)")
    @WithMockUser(username = "librarian@test.local", roles = "LIBRARIAN")
    void testUpdatePhysicalCopy_InvalidTransition_NonActiveToNonActive() throws Exception {
        PhysicalItem item = createPhysicalCopy("LIB-INVALID-1", "Storage", InventoryStatus.LOST, CirculationStatus.AVAILABLE);

        mockMvc.perform(put("/librarian/physical-items/" + item.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "barcode": "LIB-INVALID-1",
                                  "location": "Storage",
                                  "inventoryStatus": "DAMAGED"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_INVENTORY_TRANSITION"));
    }

    @Test
    @DisplayName("Update physical copy fails when item ID does not exist")
    @WithMockUser(username = "librarian@test.local", roles = "LIBRARIAN")
    void testUpdatePhysicalCopy_NotFound() throws Exception {
        mockMvc.perform(put("/librarian/physical-items/88888")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "barcode": "LIB-NONEXISTENT",
                                  "location": "Unknown"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PHYSICAL_ITEM_NOT_FOUND"));
    }

    @Test
    @DisplayName("Role protection: READER role is forbidden from physical item management endpoints")
    @WithMockUser(username = "reader@test.local", roles = "READER")
    void testRoleProtection_ReaderForbidden() throws Exception {
        mockMvc.perform(post("/librarian/resources/" + sampleResource.getId() + "/physical-items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "barcode": "LIB-READER-ATTEMPT",
                                  "location": "Loc R"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OPERATION_FORBIDDEN"));
    }

    @Test
    @DisplayName("Role protection: Unauthenticated user is unauthorized")
    void testRoleProtection_Unauthenticated() throws Exception {
        mockMvc.perform(post("/librarian/resources/" + sampleResource.getId() + "/physical-items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "barcode": "LIB-ANON-ATTEMPT",
                                  "location": "Loc A"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    private PhysicalItem createPhysicalCopy(String barcode, String location, InventoryStatus inventoryStatus, CirculationStatus circulationStatus) {
        return physicalItemRepository.save(PhysicalItem.builder()
                .resource(sampleResource)
                .barcode(barcode)
                .location(location)
                .inventoryStatus(inventoryStatus)
                .circulationStatus(circulationStatus)
                .build());
    }

    private Account createAccount(String email, AccountRole role) {
        LocalDateTime now = LocalDateTime.now();
        return accountRepository.save(Account.builder()
                .email(email)
                .passwordHash("hashed-password")
                .displayName(email)
                .role(role)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}
