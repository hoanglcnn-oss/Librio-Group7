package com.librio.service;

import com.librio.domain.Account;
import com.librio.domain.AccountRole;
import com.librio.domain.AccountStatus;
import com.librio.domain.Borrowing;
import com.librio.domain.BorrowRequest;
import com.librio.domain.BorrowRequestStatus;
import com.librio.domain.CirculationStatus;
import com.librio.domain.InventoryStatus;
import com.librio.domain.PhysicalItem;
import com.librio.domain.Resource;
import com.librio.dto.ManagedResourceDto;
import com.librio.dto.ResourceAdminRequestDto;
import com.librio.dto.UpdatePhysicalItemRequestDto;
import com.librio.exception.BorrowFlowException;
import com.librio.repository.AccountRepository;
import com.librio.repository.BorrowRequestRepository;
import com.librio.repository.BorrowingRepository;
import com.librio.repository.PhysicalItemRepository;
import com.librio.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-147: Verify physical-copy inventory transitions, conflict rules, borrowability,
 * historical integrity, and T-146 regression behaviour.
 *
 * Exercises PhysicalItemService directly; complements controller-layer security
 * tests in LibrarianPhysicalItemControllerTest.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PhysicalItemInventoryTest {

    @Autowired private PhysicalItemService physicalItemService;
    @Autowired private ResourceAdminService resourceAdminService;
    @Autowired private PhysicalItemRepository physicalItemRepository;
    @Autowired private ResourceRepository resourceRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private BorrowingRepository borrowingRepository;
    @Autowired private BorrowRequestRepository borrowRequestRepository;

    private Resource sampleResource;

    @BeforeEach
    void setUp() {
        sampleResource = resourceRepository.save(Resource.builder()
                .title("T-147 Test Resource")
                .authors("Author T147")
                .description("Used by T-147 tests")
                .category("Testing")
                .build());
    }

    // -----------------------------------------------------------------------
    // 1. Inventory transition rules
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("1. Inventory transition rules")
    class InventoryTransitionRules {

        @Test
        @DisplayName("ACTIVE -> LOST allowed when AVAILABLE")
        void activeToLost_allowed() {
            PhysicalItem item = createItem("T147-AL-001", "Shelf A", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);
            assertThat(updateInventoryStatus(item, InventoryStatus.LOST).getInventoryStatus()).isEqualTo(InventoryStatus.LOST);
        }

        @Test
        @DisplayName("ACTIVE -> DAMAGED allowed when AVAILABLE")
        void activeToDamaged_allowed() {
            PhysicalItem item = createItem("T147-AD-001", "Shelf B", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);
            assertThat(updateInventoryStatus(item, InventoryStatus.DAMAGED).getInventoryStatus()).isEqualTo(InventoryStatus.DAMAGED);
        }

        @Test
        @DisplayName("ACTIVE -> WITHDRAWN allowed when AVAILABLE")
        void activeToWithdrawn_allowed() {
            PhysicalItem item = createItem("T147-AW-001", "Shelf C", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);
            assertThat(updateInventoryStatus(item, InventoryStatus.WITHDRAWN).getInventoryStatus()).isEqualTo(InventoryStatus.WITHDRAWN);
        }

        @Test
        @DisplayName("LOST -> ACTIVE restore allowed")
        void lostToActive_allowed() {
            PhysicalItem item = createItem("T147-LA-001", "Storage", InventoryStatus.LOST, CirculationStatus.AVAILABLE);
            assertThat(updateInventoryStatus(item, InventoryStatus.ACTIVE).getInventoryStatus()).isEqualTo(InventoryStatus.ACTIVE);
        }

        @Test
        @DisplayName("DAMAGED -> ACTIVE restore allowed")
        void damagedToActive_allowed() {
            PhysicalItem item = createItem("T147-DA-001", "Repair", InventoryStatus.DAMAGED, CirculationStatus.AVAILABLE);
            assertThat(updateInventoryStatus(item, InventoryStatus.ACTIVE).getInventoryStatus()).isEqualTo(InventoryStatus.ACTIVE);
        }

        @Test
        @DisplayName("WITHDRAWN -> ACTIVE restore allowed")
        void withdrawnToActive_allowed() {
            PhysicalItem item = createItem("T147-WA-001", "Archive", InventoryStatus.WITHDRAWN, CirculationStatus.AVAILABLE);
            assertThat(updateInventoryStatus(item, InventoryStatus.ACTIVE).getInventoryStatus()).isEqualTo(InventoryStatus.ACTIVE);
        }

        @Test
        @DisplayName("ACTIVE -> ACTIVE same-state is idempotent")
        void sameState_active() {
            PhysicalItem item = createItem("T147-SS-AA", "Shelf A", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);
            assertThat(updateInventoryStatus(item, InventoryStatus.ACTIVE).getInventoryStatus()).isEqualTo(InventoryStatus.ACTIVE);
        }

        @Test
        @DisplayName("LOST -> LOST same-state is idempotent")
        void sameState_lost() {
            PhysicalItem item = createItem("T147-SS-LL", "Shelf B", InventoryStatus.LOST, CirculationStatus.AVAILABLE);
            assertThat(updateInventoryStatus(item, InventoryStatus.LOST).getInventoryStatus()).isEqualTo(InventoryStatus.LOST);
        }

        @Test
        @DisplayName("DAMAGED -> DAMAGED same-state is idempotent")
        void sameState_damaged() {
            PhysicalItem item = createItem("T147-SS-DD", "Repair", InventoryStatus.DAMAGED, CirculationStatus.AVAILABLE);
            assertThat(updateInventoryStatus(item, InventoryStatus.DAMAGED).getInventoryStatus()).isEqualTo(InventoryStatus.DAMAGED);
        }

        @Test
        @DisplayName("WITHDRAWN -> WITHDRAWN same-state is idempotent")
        void sameState_withdrawn() {
            PhysicalItem item = createItem("T147-SS-WW", "Archive", InventoryStatus.WITHDRAWN, CirculationStatus.AVAILABLE);
            assertThat(updateInventoryStatus(item, InventoryStatus.WITHDRAWN).getInventoryStatus()).isEqualTo(InventoryStatus.WITHDRAWN);
        }

        @Test
        @DisplayName("LOST -> DAMAGED rejected: must restore to ACTIVE first (INVALID_INVENTORY_TRANSITION)")
        void lostToDamaged_rejected() {
            PhysicalItem item = createItem("T147-NA-LD", "Storage", InventoryStatus.LOST, CirculationStatus.AVAILABLE);
            assertBorrowCode(() -> updateInventoryStatus(item, InventoryStatus.DAMAGED), "INVALID_INVENTORY_TRANSITION");
        }

        @Test
        @DisplayName("LOST -> WITHDRAWN rejected")
        void lostToWithdrawn_rejected() {
            PhysicalItem item = createItem("T147-NA-LW", "Storage", InventoryStatus.LOST, CirculationStatus.AVAILABLE);
            assertBorrowCode(() -> updateInventoryStatus(item, InventoryStatus.WITHDRAWN), "INVALID_INVENTORY_TRANSITION");
        }

        @Test
        @DisplayName("DAMAGED -> WITHDRAWN rejected")
        void damagedToWithdrawn_rejected() {
            PhysicalItem item = createItem("T147-NA-DW", "Repair", InventoryStatus.DAMAGED, CirculationStatus.AVAILABLE);
            assertBorrowCode(() -> updateInventoryStatus(item, InventoryStatus.WITHDRAWN), "INVALID_INVENTORY_TRANSITION");
        }

        @Test
        @DisplayName("WITHDRAWN -> LOST rejected")
        void withdrawnToLost_rejected() {
            PhysicalItem item = createItem("T147-NA-WL", "Archive", InventoryStatus.WITHDRAWN, CirculationStatus.AVAILABLE);
            assertBorrowCode(() -> updateInventoryStatus(item, InventoryStatus.LOST), "INVALID_INVENTORY_TRANSITION");
        }
    }

    // -----------------------------------------------------------------------
    // 2. Circulation conflict rules
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("2. Active circulation conflict rules")
    class CirculationConflictRules {

        @Test
        @DisplayName("ACTIVE -> LOST rejected when RESERVED (ACTIVE_CIRCULATION_CONFLICT)")
        void activeLost_rejectedWhenReserved() {
            PhysicalItem item = createItem("T147-CC-RL", "Hold", InventoryStatus.ACTIVE, CirculationStatus.RESERVED);
            assertBorrowCode(() -> updateInventoryStatus(item, InventoryStatus.LOST), "ACTIVE_CIRCULATION_CONFLICT");
        }

        @Test
        @DisplayName("ACTIVE -> DAMAGED rejected when RESERVED")
        void activeDamaged_rejectedWhenReserved() {
            PhysicalItem item = createItem("T147-CC-RD", "Hold", InventoryStatus.ACTIVE, CirculationStatus.RESERVED);
            assertBorrowCode(() -> updateInventoryStatus(item, InventoryStatus.DAMAGED), "ACTIVE_CIRCULATION_CONFLICT");
        }

        @Test
        @DisplayName("ACTIVE -> WITHDRAWN rejected when RESERVED")
        void activeWithdrawn_rejectedWhenReserved() {
            PhysicalItem item = createItem("T147-CC-RW", "Hold", InventoryStatus.ACTIVE, CirculationStatus.RESERVED);
            assertBorrowCode(() -> updateInventoryStatus(item, InventoryStatus.WITHDRAWN), "ACTIVE_CIRCULATION_CONFLICT");
        }

        @Test
        @DisplayName("ACTIVE -> LOST rejected when BORROWED (ACTIVE_CIRCULATION_CONFLICT)")
        void activeLost_rejectedWhenBorrowed() {
            PhysicalItem item = createItem("T147-CC-BL", "Loan", InventoryStatus.ACTIVE, CirculationStatus.BORROWED);
            assertBorrowCode(() -> updateInventoryStatus(item, InventoryStatus.LOST), "ACTIVE_CIRCULATION_CONFLICT");
        }

        @Test
        @DisplayName("ACTIVE -> DAMAGED rejected when BORROWED")
        void activeDamaged_rejectedWhenBorrowed() {
            PhysicalItem item = createItem("T147-CC-BD", "Loan", InventoryStatus.ACTIVE, CirculationStatus.BORROWED);
            assertBorrowCode(() -> updateInventoryStatus(item, InventoryStatus.DAMAGED), "ACTIVE_CIRCULATION_CONFLICT");
        }

        @Test
        @DisplayName("ACTIVE -> WITHDRAWN rejected when BORROWED")
        void activeWithdrawn_rejectedWhenBorrowed() {
            PhysicalItem item = createItem("T147-CC-BW", "Loan", InventoryStatus.ACTIVE, CirculationStatus.BORROWED);
            assertBorrowCode(() -> updateInventoryStatus(item, InventoryStatus.WITHDRAWN), "ACTIVE_CIRCULATION_CONFLICT");
        }
    }

    // -----------------------------------------------------------------------
    // 3. Borrowability consistency
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("3. Borrowability and availability consistency")
    class BorrowabilityConsistency {

        @Test
        @DisplayName("Only ACTIVE+AVAILABLE copy counts as available")
        void onlyActiveAvailableCounts() {
            createItem("T147-BA-ACTIVE", "Shelf A", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);
            createItem("T147-BA-LOST", "Shelf B", InventoryStatus.LOST, CirculationStatus.AVAILABLE);
            createItem("T147-BA-DAMAGED", "Shelf C", InventoryStatus.DAMAGED, CirculationStatus.AVAILABLE);
            createItem("T147-BA-WITHDRAWN", "Shelf D", InventoryStatus.WITHDRAWN, CirculationStatus.AVAILABLE);
            createItem("T147-BA-RESERVED", "Hold", InventoryStatus.ACTIVE, CirculationStatus.RESERVED);
            createItem("T147-BA-BORROWED", "Loan", InventoryStatus.ACTIVE, CirculationStatus.BORROWED);

            long available = physicalItemRepository.countByResourceIdAndInventoryStatusAndCirculationStatus(
                    sampleResource.getId(), InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);
            assertThat(available).isEqualTo(1);
        }

        @Test
        @DisplayName("LOST item not borrowable: available count unchanged")
        void lostItemNotBorrowable() {
            createItem("T147-BN-ACTIVE", "Shelf A", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);
            PhysicalItem lost = createItem("T147-BN-LOST", "Storage", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);
            lost.setInventoryStatus(InventoryStatus.LOST);
            physicalItemRepository.save(lost);

            long available = physicalItemRepository.countByResourceIdAndInventoryStatusAndCirculationStatus(
                    sampleResource.getId(), InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);
            assertThat(available).isEqualTo(1);
        }
    }

    // -----------------------------------------------------------------------
    // 4. Identity constraints
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("4. Physical copy identity constraints")
    class PhysicalCopyIdentityConstraints {

        @Test
        @DisplayName("Create fails when barcode is blank")
        void createFails_blankBarcode() {
            var req = new com.librio.dto.CreatePhysicalItemRequestDto();
            req.setBarcode("");
            req.setLocation("Shelf A");
            assertThatThrownBy(() -> physicalItemService.createPhysicalItem(sampleResource.getId(), req))
                    .isInstanceOf(BorrowFlowException.class)
                    .hasMessageContaining("Barcode is required");
        }

        @Test
        @DisplayName("Create fails when location is blank")
        void createFails_blankLocation() {
            var req = new com.librio.dto.CreatePhysicalItemRequestDto();
            req.setBarcode("T147-ID-LOC");
            req.setLocation("");
            assertThatThrownBy(() -> physicalItemService.createPhysicalItem(sampleResource.getId(), req))
                    .isInstanceOf(BorrowFlowException.class)
                    .hasMessageContaining("Location is required");
        }

        @Test
        @DisplayName("Create defaults to ACTIVE+AVAILABLE regardless of caller intent")
        void createDefaultsToActiveAvailable() {
            var req = new com.librio.dto.CreatePhysicalItemRequestDto();
            req.setBarcode("T147-ID-DEFAULT");
            req.setLocation("Shelf A");
            var dto = physicalItemService.createPhysicalItem(sampleResource.getId(), req);
            assertThat(dto.getInventoryStatus()).isEqualTo(InventoryStatus.ACTIVE);
            assertThat(dto.getCirculationStatus()).isEqualTo(CirculationStatus.AVAILABLE);
        }

        @Test
        @DisplayName("Update cannot mutate circulationStatus (server-managed)")
        void updateCannotMutateCirculationStatus() {
            PhysicalItem item = createItem("T147-ID-CIRC", "Shelf A", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);
            var req = new UpdatePhysicalItemRequestDto();
            req.setBarcode("T147-ID-CIRC");
            req.setLocation("Shelf A");
            var dto = physicalItemService.updatePhysicalItem(item.getId(), req);
            assertThat(dto.getCirculationStatus()).isEqualTo(CirculationStatus.AVAILABLE);
        }
    }

    // -----------------------------------------------------------------------
    // 5. Duplicate barcode
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("5. Duplicate barcode behaviour")
    class DuplicateBarcode {

        @Test
        @DisplayName("Create with duplicate barcode returns DUPLICATE_ITEM_BARCODE")
        void createDuplicate() {
            createItem("T147-DUP-001", "Shelf A", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);
            var req = new com.librio.dto.CreatePhysicalItemRequestDto();
            req.setBarcode("T147-DUP-001");
            req.setLocation("Shelf B");
            assertBorrowCode(() -> physicalItemService.createPhysicalItem(sampleResource.getId(), req),
                    "DUPLICATE_ITEM_BARCODE");
        }

        @Test
        @DisplayName("Update to barcode of another item returns DUPLICATE_ITEM_BARCODE")
        void updateDuplicate() {
            createItem("T147-DUP-A", "Shelf A", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);
            PhysicalItem itemB = createItem("T147-DUP-B", "Shelf B", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);
            var req = new UpdatePhysicalItemRequestDto();
            req.setBarcode("T147-DUP-A");
            req.setLocation("Shelf B");
            assertBorrowCode(() -> physicalItemService.updatePhysicalItem(itemB.getId(), req),
                    "DUPLICATE_ITEM_BARCODE");
        }
    }

    // -----------------------------------------------------------------------
    // 7. Historical integrity
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("7. Historical integrity: soft withdrawal preserves rows and Borrowing history")
    class HistoricalIntegrity {

        @Test
        @DisplayName("WITHDRAWN item row is retained (soft deletion)")
        void withdrawnRowRetained() {
            PhysicalItem item = createItem("T147-HI-WITHDRAW", "Shelf A", InventoryStatus.ACTIVE, CirculationStatus.AVAILABLE);
            Long itemId = item.getId();
            updateInventoryStatus(item, InventoryStatus.WITHDRAWN);
            assertThat(physicalItemRepository.findById(itemId)).isPresent();
            assertThat(physicalItemRepository.findById(itemId).orElseThrow().getInventoryStatus())
                    .isEqualTo(InventoryStatus.WITHDRAWN);
        }

        @Test
        @DisplayName("Updating barcode/location does not delete existing Borrowing records")
        void updateMetadata_doesNotDeleteBorrowings() {
            Account readerAccount = createAccount("t147.history@test.local");
            PhysicalItem item = createItem("T147-HI-BORROW", "Shelf A", InventoryStatus.ACTIVE, CirculationStatus.BORROWED);
            LocalDateTime now = LocalDateTime.now();

            BorrowRequest borrowRequest = borrowRequestRepository.save(BorrowRequest.builder()
                    .reader(readerAccount)
                    .resource(sampleResource)
                    .physicalItem(item)
                    .status(BorrowRequestStatus.FULFILLED)
                    .requestedAt(now.minusDays(5))
                    .statusUpdatedAt(now.minusDays(4))
                    .expiresAt(now.plusDays(2))
                    .createdAt(now.minusDays(5))
                    .updatedAt(now.minusDays(4))
                    .build());

            Borrowing borrowing = borrowingRepository.save(Borrowing.builder()
                    .reader(readerAccount)
                    .physicalItem(item)
                    .borrowRequest(borrowRequest)
                    .borrowedAt(now.minusDays(4))
                    .dueAt(now.plusDays(10))
                    .build());

            Long borrowingId = borrowing.getId();

            var req = new UpdatePhysicalItemRequestDto();
            req.setBarcode("T147-HI-BORROW-RENAMED");
            req.setLocation("New Shelf B");
            physicalItemService.updatePhysicalItem(item.getId(), req);

            assertThat(borrowingRepository.findById(borrowingId)).isPresent();
        }
    }

    // -----------------------------------------------------------------------
    // 8. T-146 regression: backend availability reflects inventory mutation
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("8. T-146 regression: managed-resource availability reflects inventory mutation")
    class T146AvailabilityRegression {

        @Test
        @DisplayName("ACTIVE->DAMAGED reduces available count in managed resource fetch")
        void damagedTransition_reducesAvailability() {
            ManagedResourceDto created = createResourceWithCopies("T146 Regression Resource", 2);
            assertThat(created.getPhysical().getAvailableCopies()).isEqualTo(2);

            PhysicalItem item = physicalItemRepository.findByResourceId(created.getId()).get(0);
            var req = new UpdatePhysicalItemRequestDto();
            req.setBarcode(item.getBarcode());
            req.setLocation(item.getLocation());
            req.setInventoryStatus(InventoryStatus.DAMAGED);
            physicalItemService.updatePhysicalItem(item.getId(), req);

            ManagedResourceDto refreshed = resourceAdminService.get(created.getId());
            assertThat(refreshed.getPhysical().getAvailableCopies())
                    .as("availability must decrease by 1 after DAMAGED transition")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("WITHDRAWN item is not counted as available in managed resource fetch")
        void withdrawnTransition_notAvailable() {
            ManagedResourceDto created = createResourceWithCopies("T146 Withdrawn Resource", 2);

            PhysicalItem item = physicalItemRepository.findByResourceId(created.getId()).get(0);
            var req = new UpdatePhysicalItemRequestDto();
            req.setBarcode(item.getBarcode());
            req.setLocation(item.getLocation());
            req.setInventoryStatus(InventoryStatus.WITHDRAWN);
            physicalItemService.updatePhysicalItem(item.getId(), req);

            ManagedResourceDto refreshed = resourceAdminService.get(created.getId());
            assertThat(refreshed.getPhysical().getAvailableCopies())
                    .as("WITHDRAWN item must not count as available")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("Restoring DAMAGED item to ACTIVE increases available count")
        void restoreToActive_increasesAvailability() {
            ManagedResourceDto created = createResourceWithCopies("T146 Restore Resource", 1);

            PhysicalItem item = physicalItemRepository.findByResourceId(created.getId()).get(0);

            var damageReq = new UpdatePhysicalItemRequestDto();
            damageReq.setBarcode(item.getBarcode());
            damageReq.setLocation(item.getLocation());
            damageReq.setInventoryStatus(InventoryStatus.DAMAGED);
            physicalItemService.updatePhysicalItem(item.getId(), damageReq);
            assertThat(resourceAdminService.get(created.getId()).getPhysical().getAvailableCopies()).isEqualTo(0);

            var restoreReq = new UpdatePhysicalItemRequestDto();
            restoreReq.setBarcode(item.getBarcode());
            restoreReq.setLocation(item.getLocation());
            restoreReq.setInventoryStatus(InventoryStatus.ACTIVE);
            physicalItemService.updatePhysicalItem(item.getId(), restoreReq);

            assertThat(resourceAdminService.get(created.getId()).getPhysical().getAvailableCopies())
                    .as("availability must recover after restore to ACTIVE")
                    .isEqualTo(1);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Assert that a Runnable throws BorrowFlowException with the given error code. */
    private void assertBorrowCode(ThrowingRunnable action, String expectedCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BorrowFlowException.class)
                .satisfies(ex -> assertThat(((BorrowFlowException) ex).getCode()).isEqualTo(expectedCode));
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run();
    }

    private PhysicalItem createItem(String barcode, String location,
                                    InventoryStatus invStatus, CirculationStatus circStatus) {
        return physicalItemRepository.save(PhysicalItem.builder()
                .resource(sampleResource)
                .barcode(barcode)
                .location(location)
                .inventoryStatus(invStatus)
                .circulationStatus(circStatus)
                .build());
    }

    private PhysicalItem updateInventoryStatus(PhysicalItem item, InventoryStatus target) {
        var req = new UpdatePhysicalItemRequestDto();
        req.setBarcode(item.getBarcode());
        req.setLocation(item.getLocation());
        req.setInventoryStatus(target);
        physicalItemService.updatePhysicalItem(item.getId(), req);
        return physicalItemRepository.findById(item.getId()).orElseThrow();
    }

    private ManagedResourceDto createResourceWithCopies(String title, int copies) {
        ResourceAdminRequestDto req = new ResourceAdminRequestDto();
        req.setTitle(title);
        req.setAuthors(List.of("Author T147"));
        req.setAccessTypes(List.of("PHYSICAL"));
        ResourceAdminRequestDto.PhysicalInput physical = new ResourceAdminRequestDto.PhysicalInput();
        physical.setTotalCopies(copies);
        req.setPhysical(physical);
        return resourceAdminService.create(req);
    }

    private Account createAccount(String email) {
        LocalDateTime now = LocalDateTime.now();
        return accountRepository.save(Account.builder()
                .email(email)
                .passwordHash("hashed")
                .displayName(email)
                .role(AccountRole.READER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}