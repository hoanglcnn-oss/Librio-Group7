package com.librio.controller;

import com.librio.domain.Account;
import com.librio.domain.AccountRole;
import com.librio.domain.AccountStatus;
import com.librio.domain.BorrowRequest;
import com.librio.domain.BorrowRequestStatus;
import com.librio.domain.Borrowing;
import com.librio.domain.CirculationStatus;
import com.librio.domain.InventoryStatus;
import com.librio.domain.PhysicalItem;
import com.librio.domain.Resource;
import com.librio.repository.AccountRepository;
import com.librio.repository.BorrowRequestRepository;
import com.librio.repository.BorrowingRepository;
import com.librio.repository.DigitalItemRepository;
import com.librio.repository.PhysicalItemRepository;
import com.librio.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LibrarianInventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private PhysicalItemRepository physicalItemRepository;

    @Autowired
    private DigitalItemRepository digitalItemRepository;

    @SpyBean
    private BorrowRequestRepository borrowRequestRepository;

    @SpyBean
    private BorrowingRepository borrowingRepository;

    private Resource resourceA;
    private Resource resourceB;
    private Account reader;

    @BeforeEach
    void setUp() {
        borrowingRepository.deleteAll();
        borrowRequestRepository.deleteAll();
        digitalItemRepository.deleteAll();
        physicalItemRepository.deleteAll();
        resourceRepository.deleteAll();

        createAccount("librarian@cockpit.test", AccountRole.LIBRARIAN);
        reader = createAccount("reader@cockpit.test", AccountRole.READER);

        resourceA = resourceRepository.save(Resource.builder()
                .title("Clean Code Cockpit")
                .authors("Robert C. Martin")
                .description("Agile Craftsmanship")
                .category("Engineering")
                .build());

        resourceB = resourceRepository.save(Resource.builder()
                .title("Refactoring Cockpit")
                .authors("Martin Fowler")
                .description("Improving Design")
                .category("Engineering")
                .build());
    }

    @Test
    @DisplayName("Summary counts and WITHDRAWN exclusion test")
    @WithMockUser(username = "librarian@cockpit.test", roles = "LIBRARIAN")
    void testInventorySummary_CountsAndWithdrawnExclusion() throws Exception {
        // 1. ACTIVE + AVAILABLE (healthy)
        createPhysicalCopy(resourceA, "LIB-SUM-1", "Shelf A-1", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);

        // 2. ACTIVE + RESERVED
        PhysicalItem itemRes = createPhysicalCopy(resourceA, "LIB-SUM-2", "Shelf A-2", InventoryStatus.ACTIVE, CirculationStatus.RESERVED);
        createActiveRequest(reader, resourceA, itemRes, BorrowRequestStatus.REQUESTED);

        // 3. ACTIVE + BORROWED (Overdue -> attention)
        PhysicalItem itemBor = createPhysicalCopy(resourceB, "LIB-SUM-3", "Shelf B-1", InventoryStatus.ACTIVE, CirculationStatus.BORROWED);
        createActiveBorrowing(reader, resourceB, itemBor, LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(2));

        // 4. LOST (attention)
        createPhysicalCopy(resourceB, "LIB-SUM-4", "Shelf B-2", InventoryStatus.LOST, CirculationStatus.AVAILABLE);

        // 5. WITHDRAWN (must be excluded from total copies & attention counts)
        createPhysicalCopy(resourceA, "LIB-SUM-5", "WITHDRAWN-SHELF", InventoryStatus.WITHDRAWN, CirculationStatus.AVAILABLE);

        mockMvc.perform(get("/librarian/inventory/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCopies").value(4))
                .andExpect(jsonPath("$.availableCopies").value(1))
                .andExpect(jsonPath("$.reservedCopies").value(1))
                .andExpect(jsonPath("$.borrowedCopies").value(1))
                .andExpect(jsonPath("$.attentionCopies").value(2));
    }

    @Test
    @DisplayName("Attention reasons derivation test (LOST, DAMAGED, UNASSIGNED, OVERDUE)")
    @WithMockUser(username = "librarian@cockpit.test", roles = "LIBRARIAN")
    void testAttentionReasonsDerivation() throws Exception {
        // Lost item
        createPhysicalCopy(resourceA, "LIB-ATT-LOST", "Shelf A", InventoryStatus.LOST, CirculationStatus.AVAILABLE);

        // Damaged item
        createPhysicalCopy(resourceA, "LIB-ATT-DAMAGED", "Shelf A", InventoryStatus.DAMAGED, CirculationStatus.AVAILABLE);

        // Unassigned location
        createPhysicalCopy(resourceA, "LIB-ATT-UNASSIGNED", "UNASSIGNED", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);

        // Overdue item
        PhysicalItem overdueItem = createPhysicalCopy(resourceA, "LIB-ATT-OVERDUE", "Shelf A", InventoryStatus.ACTIVE, CirculationStatus.BORROWED);
        createActiveBorrowing(reader, resourceA, overdueItem, LocalDateTime.now().minusDays(20), LocalDateTime.now().minusDays(5));

        mockMvc.perform(get("/librarian/physical-items?needsAttention=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    @DisplayName("Q Search matching barcode, location, title, and authors")
    @WithMockUser(username = "librarian@cockpit.test", roles = "LIBRARIAN")
    void testQSearch() throws Exception {
        createPhysicalCopy(resourceA, "LIB-SEARCH-MATCH", "Shelf X-99", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);
        createPhysicalCopy(resourceB, "LIB-SEARCH-OTHER", "Shelf Y-88", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);

        // Match by barcode
        mockMvc.perform(get("/librarian/physical-items?q=SEARCH-MATCH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].barcode").value("LIB-SEARCH-MATCH"));

        // Match by location
        mockMvc.perform(get("/librarian/physical-items?q=X-99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].location").value("Shelf X-99"));

        // Match by resource title
        mockMvc.perform(get("/librarian/physical-items?q=Clean Code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].barcode").value("LIB-SEARCH-MATCH"));

        // Match by author
        mockMvc.perform(get("/librarian/physical-items?q=Fowler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].barcode").value("LIB-SEARCH-OTHER"));
    }

    @Test
    @DisplayName("Combined filtering test (inventoryStatus, circulationStatus, needsAttention)")
    @WithMockUser(username = "librarian@cockpit.test", roles = "LIBRARIAN")
    void testCombinedFilters() throws Exception {
        createPhysicalCopy(resourceA, "LIB-COMB-1", "Shelf 1", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);
        createPhysicalCopy(resourceA, "LIB-COMB-2", "Shelf 2", InventoryStatus.DAMAGED, CirculationStatus.AVAILABLE);

        mockMvc.perform(get("/librarian/physical-items?inventoryStatus=ACTIVE&circulationStatus=AVAILABLE&needsAttention=false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].barcode").value("LIB-COMB-1"));
    }

    @Test
    @DisplayName("Pagination and total elements / total pages test")
    @WithMockUser(username = "librarian@cockpit.test", roles = "LIBRARIAN")
    void testPagination() throws Exception {
        for (int i = 1; i <= 5; i++) {
            createPhysicalCopy(resourceA, "LIB-PAG-" + i, "Shelf P", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);
        }

        mockMvc.perform(get("/librarian/physical-items?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    @DisplayName("Stable ordering prioritizing operational attention followed by id ASC")
    @WithMockUser(username = "librarian@cockpit.test", roles = "LIBRARIAN")
    void testStableOrdering() throws Exception {
        PhysicalItem healthy = createPhysicalCopy(resourceA, "LIB-ORD-HEALTHY", "Shelf Z", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);
        PhysicalItem lost = createPhysicalCopy(resourceA, "LIB-ORD-LOST", "Shelf Z", InventoryStatus.LOST, CirculationStatus.AVAILABLE);
        PhysicalItem overdueItem = createPhysicalCopy(resourceA, "LIB-ORD-OVERDUE", "Shelf Z", InventoryStatus.ACTIVE, CirculationStatus.BORROWED);
        createActiveBorrowing(reader, resourceA, overdueItem, LocalDateTime.now().minusDays(15), LocalDateTime.now().minusDays(1));

        mockMvc.perform(get("/librarian/physical-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(overdueItem.getId()))
                .andExpect(jsonPath("$.items[1].id").value(lost.getId()))
                .andExpect(jsonPath("$.items[2].id").value(healthy.getId()));
    }

    @Test
    @DisplayName("Active BORROWING projection test")
    @WithMockUser(username = "librarian@cockpit.test", roles = "LIBRARIAN")
    void testActiveBorrowingProjection() throws Exception {
        PhysicalItem item = createPhysicalCopy(resourceA, "LIB-PROJ-BOR", "Shelf B", InventoryStatus.ACTIVE, CirculationStatus.BORROWED);
        Borrowing borrowing = createActiveBorrowing(reader, resourceA, item, LocalDateTime.now().minusDays(5), LocalDateTime.now().plusDays(5));

        mockMvc.perform(get("/librarian/physical-items?q=LIB-PROJ-BOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].activeOperation.type").value("BORROWING"))
                .andExpect(jsonPath("$.items[0].activeOperation.id").value(borrowing.getId()))
                .andExpect(jsonPath("$.items[0].activeOperation.reader.displayName").value(reader.getDisplayName()))
                .andExpect(jsonPath("$.items[0].activeOperation.overdue").value(false));
    }

    @Test
    @DisplayName("Active BORROW_REQUEST projection test")
    @WithMockUser(username = "librarian@cockpit.test", roles = "LIBRARIAN")
    void testActiveRequestProjection() throws Exception {
        PhysicalItem item = createPhysicalCopy(resourceA, "LIB-PROJ-REQ", "Shelf R", InventoryStatus.ACTIVE, CirculationStatus.RESERVED);
        BorrowRequest request = createActiveRequest(reader, resourceA, item, BorrowRequestStatus.REQUESTED);

        mockMvc.perform(get("/librarian/physical-items?q=LIB-PROJ-REQ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].activeOperation.type").value("BORROW_REQUEST"))
                .andExpect(jsonPath("$.items[0].activeOperation.id").value(request.getId()))
                .andExpect(jsonPath("$.items[0].activeOperation.status").value("REQUESTED"))
                .andExpect(jsonPath("$.items[0].activeOperation.reader.displayName").value(reader.getDisplayName()));
    }

    @Test
    @DisplayName("Circulation data mismatch detection test (overlap & status mismatch)")
    @WithMockUser(username = "librarian@cockpit.test", roles = "LIBRARIAN")
    void testMismatchDetection() throws Exception {
        // Mismatch 1: Both borrowing and request exist for same copy
        PhysicalItem itemOverlap = createPhysicalCopy(resourceA, "LIB-MIS-OVERLAP", "Shelf M", InventoryStatus.ACTIVE, CirculationStatus.BORROWED);
        createActiveRequest(reader, resourceA, itemOverlap, BorrowRequestStatus.REQUESTED);
        createActiveBorrowing(reader, resourceA, itemOverlap, LocalDateTime.now().minusDays(2), LocalDateTime.now().plusDays(10));

        // Mismatch 2: Status is RESERVED but no request record exists
        createPhysicalCopy(resourceB, "LIB-MIS-NOREQ", "Shelf M", InventoryStatus.ACTIVE, CirculationStatus.RESERVED);

        mockMvc.perform(get("/librarian/physical-items?needsAttention=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].attentionReasons", hasItems("CIRCULATION_DATA_MISMATCH")))
                .andExpect(jsonPath("$.items[1].attentionReasons", hasItems("CIRCULATION_DATA_MISMATCH")));
    }

    @Test
    @DisplayName("Bounded active operation query verification: repository only queries page IDs")
    @WithMockUser(username = "librarian@cockpit.test", roles = "LIBRARIAN")
    void testBoundedActiveOperationsQuery() throws Exception {
        PhysicalItem item1 = createPhysicalCopy(resourceA, "LIB-BND-1", "Shelf B1", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);
        PhysicalItem item2 = createPhysicalCopy(resourceA, "LIB-BND-2", "Shelf B2", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);
        PhysicalItem item3 = createPhysicalCopy(resourceA, "LIB-BND-3", "Shelf B3", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);

        // Request page 0 size 2 -> should return item1 and item2 (sorted by id ASC for healthy items)
        mockMvc.perform(get("/librarian/physical-items?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));

        // Verify unbounded queries were NEVER called
        verify(borrowRequestRepository, never()).findActiveForLibrarian(any());
        verify(borrowingRepository, never()).findActiveForLibrarian();

        // Verify bounded queries were called with item1 and item2 IDs only
        List<Long> expectedPageIds = List.of(item1.getId(), item2.getId());
        verify(borrowRequestRepository).findActiveByPhysicalItemIds(argThat(ids -> ids.containsAll(expectedPageIds) && !ids.contains(item3.getId())), any());
        verify(borrowingRepository).findActiveByPhysicalItemIds(argThat(ids -> ids.containsAll(expectedPageIds) && !ids.contains(item3.getId())));
    }

    @Test
    @DisplayName("Invalid pagination parameters return 400 Bad Request")
    @WithMockUser(username = "librarian@cockpit.test", roles = "LIBRARIAN")
    void testInvalidPaginationParameters() throws Exception {
        mockMvc.perform(get("/librarian/physical-items?page=-1&size=20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGINATION_PARAMETER"));

        mockMvc.perform(get("/librarian/physical-items?page=0&size=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGINATION_PARAMETER"));

        mockMvc.perform(get("/librarian/physical-items?page=0&size=101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGINATION_PARAMETER"));
    }

    @Test
    @DisplayName("Role protection: READER role is forbidden from cockpit endpoints")
    @WithMockUser(username = "reader@cockpit.test", roles = "READER")
    void testRoleProtection_ReaderForbidden() throws Exception {
        mockMvc.perform(get("/librarian/inventory/summary"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/librarian/physical-items"))
                .andExpect(status().isForbidden());
    }

    private PhysicalItem createPhysicalCopy(Resource resource, String barcode, String location, InventoryStatus inventoryStatus, CirculationStatus circulationStatus) {
        return physicalItemRepository.save(PhysicalItem.builder()
                .resource(resource)
                .barcode(barcode)
                .location(location)
                .inventoryStatus(inventoryStatus)
                .circulationStatus(circulationStatus)
                .build());
    }

    private BorrowRequest createActiveRequest(Account reader, Resource resource, PhysicalItem item, BorrowRequestStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return borrowRequestRepository.save(BorrowRequest.builder()
                .reader(reader)
                .resource(resource)
                .physicalItem(item)
                .status(status)
                .requestedAt(now)
                .statusUpdatedAt(now)
                .expiresAt(now.plusDays(1))
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private Borrowing createActiveBorrowing(Account reader, Resource resource, PhysicalItem item, LocalDateTime borrowedAt, LocalDateTime dueAt) {
        BorrowRequest req = createActiveRequest(reader, resource, item, BorrowRequestStatus.FULFILLED);
        return borrowingRepository.save(Borrowing.builder()
                .reader(reader)
                .physicalItem(item)
                .borrowRequest(req)
                .borrowedAt(borrowedAt)
                .dueAt(dueAt)
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
