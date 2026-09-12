package com.librio.service;

import com.librio.config.CirculationPolicyProperties;
import com.librio.domain.*;
import com.librio.exception.BorrowErrorCode;
import com.librio.exception.BorrowFlowException;
import com.librio.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowServiceQuotaTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private PhysicalItemRepository physicalItemRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BorrowRequestRepository borrowRequestRepository;

    @Mock
    private BorrowingRepository borrowingRepository;

    @Mock
    private CirculationPolicyProperties circulationPolicy;

    @Mock
    private BorrowingQuotaPolicy borrowingQuotaPolicy;

    @InjectMocks
    private BorrowService borrowService;

    private Account validReader;
    private Resource validResource;
    private PhysicalItem availableItem;

    @BeforeEach
    void setUp() {
        validReader = Account.builder().id(1L).role(AccountRole.READER).accountStatus(AccountStatus.ACTIVE).build();
        validResource = Resource.builder().id(100L).build();
        availableItem = PhysicalItem.builder().id(1000L).circulationStatus(CirculationStatus.AVAILABLE).build();
    
    @Test
    @DisplayName("I. Final-copy behavior remains intact: quota passes but copy unavailable -> NO_AVAILABLE_COPY")
    void testNoAvailableCopy() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(validReader));
        when(borrowingQuotaPolicy.getQuotaSnapshot(eq(1L), any())).thenReturn(
                BorrowingQuotaPolicy.QuotaSnapshot.builder().activeMembership(true).validPlanQuota(true).remainingQuota(1L).build()
        );
        when(resourceRepository.findById(100L)).thenReturn(Optional.of(validResource));
        when(borrowRequestRepository.existsByReaderIdAndResourceIdAndStatusIn(eq(1L), eq(100L), any())).thenReturn(false);
        when(borrowingRepository.existsActiveBorrowingByReaderIdAndResourceId(1L, 100L)).thenReturn(false);
        when(borrowRequestRepository.countByReaderIdAndStatusIn(eq(1L), any())).thenReturn(0L);
        when(borrowingRepository.countActiveBorrowingsByReaderId(1L)).thenReturn(0L);
        when(circulationPolicy.commitmentLimit()).thenReturn(5L);
        when(physicalItemRepository.countByResourceId(100L)).thenReturn(1L);
        when(physicalItemRepository.findForUpdate(eq(100L), eq(InventoryStatus.ACTIVE), eq(CirculationStatus.AVAILABLE), eq(PageRequest.of(0, 1))))
                .thenReturn(List.of());

        BorrowFlowException ex = assertThrows(BorrowFlowException.class, () -> borrowService.createRequest(1L, 100L));
        assertEquals(BorrowErrorCode.NO_AVAILABLE_COPY.name(), ex.getErrorCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        
        verify(borrowRequestRepository, never()).save(any());
    }
}


    private void mockCommonSuccessPath() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(validReader));
        when(resourceRepository.findById(100L)).thenReturn(Optional.of(validResource));
        when(borrowRequestRepository.existsByReaderIdAndResourceIdAndStatusIn(eq(1L), eq(100L), any())).thenReturn(false);
        when(borrowingRepository.existsActiveBorrowingByReaderIdAndResourceId(1L, 100L)).thenReturn(false);
        when(borrowRequestRepository.countByReaderIdAndStatusIn(eq(1L), any())).thenReturn(0L);
        when(borrowingRepository.countActiveBorrowingsByReaderId(1L)).thenReturn(0L);
        when(circulationPolicy.commitmentLimit()).thenReturn(5L);
        when(physicalItemRepository.countByResourceId(100L)).thenReturn(1L);
        when(physicalItemRepository.findForUpdate(eq(100L), eq(InventoryStatus.ACTIVE), eq(CirculationStatus.AVAILABLE), eq(PageRequest.of(0, 1))))
                .thenReturn(List.of(availableItem));
        when(borrowRequestRepository.save(any())).thenAnswer(inv -> {
            BorrowRequest req = inv.getArgument(0);
            req.setId(999L);
            return req;
        
    @Test
    @DisplayName("I. Final-copy behavior remains intact: quota passes but copy unavailable -> NO_AVAILABLE_COPY")
    void testNoAvailableCopy() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(validReader));
        when(borrowingQuotaPolicy.getQuotaSnapshot(eq(1L), any())).thenReturn(
                BorrowingQuotaPolicy.QuotaSnapshot.builder().activeMembership(true).validPlanQuota(true).remainingQuota(1L).build()
        );
        when(resourceRepository.findById(100L)).thenReturn(Optional.of(validResource));
        when(borrowRequestRepository.existsByReaderIdAndResourceIdAndStatusIn(eq(1L), eq(100L), any())).thenReturn(false);
        when(borrowingRepository.existsActiveBorrowingByReaderIdAndResourceId(1L, 100L)).thenReturn(false);
        when(borrowRequestRepository.countByReaderIdAndStatusIn(eq(1L), any())).thenReturn(0L);
        when(borrowingRepository.countActiveBorrowingsByReaderId(1L)).thenReturn(0L);
        when(circulationPolicy.commitmentLimit()).thenReturn(5L);
        when(physicalItemRepository.countByResourceId(100L)).thenReturn(1L);
        when(physicalItemRepository.findForUpdate(eq(100L), eq(InventoryStatus.ACTIVE), eq(CirculationStatus.AVAILABLE), eq(PageRequest.of(0, 1))))
                .thenReturn(List.of());

        BorrowFlowException ex = assertThrows(BorrowFlowException.class, () -> borrowService.createRequest(1L, 100L));
        assertEquals(BorrowErrorCode.NO_AVAILABLE_COPY.name(), ex.getErrorCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        
        verify(borrowRequestRepository, never()).save(any());
    }
}
);
    
    @Test
    @DisplayName("I. Final-copy behavior remains intact: quota passes but copy unavailable -> NO_AVAILABLE_COPY")
    void testNoAvailableCopy() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(validReader));
        when(borrowingQuotaPolicy.getQuotaSnapshot(eq(1L), any())).thenReturn(
                BorrowingQuotaPolicy.QuotaSnapshot.builder().activeMembership(true).validPlanQuota(true).remainingQuota(1L).build()
        );
        when(resourceRepository.findById(100L)).thenReturn(Optional.of(validResource));
        when(borrowRequestRepository.existsByReaderIdAndResourceIdAndStatusIn(eq(1L), eq(100L), any())).thenReturn(false);
        when(borrowingRepository.existsActiveBorrowingByReaderIdAndResourceId(1L, 100L)).thenReturn(false);
        when(borrowRequestRepository.countByReaderIdAndStatusIn(eq(1L), any())).thenReturn(0L);
        when(borrowingRepository.countActiveBorrowingsByReaderId(1L)).thenReturn(0L);
        when(circulationPolicy.commitmentLimit()).thenReturn(5L);
        when(physicalItemRepository.countByResourceId(100L)).thenReturn(1L);
        when(physicalItemRepository.findForUpdate(eq(100L), eq(InventoryStatus.ACTIVE), eq(CirculationStatus.AVAILABLE), eq(PageRequest.of(0, 1))))
                .thenReturn(List.of());

        BorrowFlowException ex = assertThrows(BorrowFlowException.class, () -> borrowService.createRequest(1L, 100L));
        assertEquals(BorrowErrorCode.NO_AVAILABLE_COPY.name(), ex.getErrorCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        
        verify(borrowRequestRepository, never()).save(any());
    }
}


    @Test
    @DisplayName("A. Reader without ACTIVE membership -> borrow request rejected")
    void testNoActiveMembership() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(validReader));
        when(borrowingQuotaPolicy.getQuotaSnapshot(eq(1L), any())).thenReturn(
                BorrowingQuotaPolicy.QuotaSnapshot.builder().activeMembership(false).build()
        );

        BorrowFlowException ex = assertThrows(BorrowFlowException.class, () -> borrowService.createRequest(1L, 100L));
        assertEquals(BorrowErrorCode.ACTIVE_MEMBERSHIP_REQUIRED.name(), ex.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        
        verify(physicalItemRepository, never()).findForUpdate(any(), any(), any(), any());
    
    @Test
    @DisplayName("I. Final-copy behavior remains intact: quota passes but copy unavailable -> NO_AVAILABLE_COPY")
    void testNoAvailableCopy() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(validReader));
        when(borrowingQuotaPolicy.getQuotaSnapshot(eq(1L), any())).thenReturn(
                BorrowingQuotaPolicy.QuotaSnapshot.builder().activeMembership(true).validPlanQuota(true).remainingQuota(1L).build()
        );
        when(resourceRepository.findById(100L)).thenReturn(Optional.of(validResource));
        when(borrowRequestRepository.existsByReaderIdAndResourceIdAndStatusIn(eq(1L), eq(100L), any())).thenReturn(false);
        when(borrowingRepository.existsActiveBorrowingByReaderIdAndResourceId(1L, 100L)).thenReturn(false);
        when(borrowRequestRepository.countByReaderIdAndStatusIn(eq(1L), any())).thenReturn(0L);
        when(borrowingRepository.countActiveBorrowingsByReaderId(1L)).thenReturn(0L);
        when(circulationPolicy.commitmentLimit()).thenReturn(5L);
        when(physicalItemRepository.countByResourceId(100L)).thenReturn(1L);
        when(physicalItemRepository.findForUpdate(eq(100L), eq(InventoryStatus.ACTIVE), eq(CirculationStatus.AVAILABLE), eq(PageRequest.of(0, 1))))
                .thenReturn(List.of());

        BorrowFlowException ex = assertThrows(BorrowFlowException.class, () -> borrowService.createRequest(1L, 100L));
        assertEquals(BorrowErrorCode.NO_AVAILABLE_COPY.name(), ex.getErrorCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        
        verify(borrowRequestRepository, never()).save(any());
    }
}


    @Test
    @DisplayName("B. Active membership with remaining quota -> request succeeds -> item becomes RESERVED")
    void testActiveMembershipSuccess() {
        mockCommonSuccessPath();
        when(borrowingQuotaPolicy.getQuotaSnapshot(eq(1L), any())).thenReturn(
                BorrowingQuotaPolicy.QuotaSnapshot.builder().activeMembership(true).validPlanQuota(true).remainingQuota(1L).build()
        );

        borrowService.createRequest(1L, 100L);

        assertEquals(CirculationStatus.RESERVED, availableItem.getCirculationStatus());
        verify(borrowRequestRepository).save(any());
    
    @Test
    @DisplayName("I. Final-copy behavior remains intact: quota passes but copy unavailable -> NO_AVAILABLE_COPY")
    void testNoAvailableCopy() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(validReader));
        when(borrowingQuotaPolicy.getQuotaSnapshot(eq(1L), any())).thenReturn(
                BorrowingQuotaPolicy.QuotaSnapshot.builder().activeMembership(true).validPlanQuota(true).remainingQuota(1L).build()
        );
        when(resourceRepository.findById(100L)).thenReturn(Optional.of(validResource));
        when(borrowRequestRepository.existsByReaderIdAndResourceIdAndStatusIn(eq(1L), eq(100L), any())).thenReturn(false);
        when(borrowingRepository.existsActiveBorrowingByReaderIdAndResourceId(1L, 100L)).thenReturn(false);
        when(borrowRequestRepository.countByReaderIdAndStatusIn(eq(1L), any())).thenReturn(0L);
        when(borrowingRepository.countActiveBorrowingsByReaderId(1L)).thenReturn(0L);
        when(circulationPolicy.commitmentLimit()).thenReturn(5L);
        when(physicalItemRepository.countByResourceId(100L)).thenReturn(1L);
        when(physicalItemRepository.findForUpdate(eq(100L), eq(InventoryStatus.ACTIVE), eq(CirculationStatus.AVAILABLE), eq(PageRequest.of(0, 1))))
                .thenReturn(List.of());

        BorrowFlowException ex = assertThrows(BorrowFlowException.class, () -> borrowService.createRequest(1L, 100L));
        assertEquals(BorrowErrorCode.NO_AVAILABLE_COPY.name(), ex.getErrorCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        
        verify(borrowRequestRepository, never()).save(any());
    }
}


    @Test
    @DisplayName("C. Exhausted quota -> request rejected -> item remains AVAILABLE")
    void testExhaustedQuota() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(validReader));
        when(borrowingQuotaPolicy.getQuotaSnapshot(eq(1L), any())).thenReturn(
                BorrowingQuotaPolicy.QuotaSnapshot.builder().activeMembership(true).validPlanQuota(true).remainingQuota(0L).build()
        );

        BorrowFlowException ex = assertThrows(BorrowFlowException.class, () -> borrowService.createRequest(1L, 100L));
        assertEquals(BorrowErrorCode.BORROW_QUOTA_EXCEEDED.name(), ex.getErrorCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        
        verify(physicalItemRepository, never()).findForUpdate(any(), any(), any(), any());
    
    @Test
    @DisplayName("I. Final-copy behavior remains intact: quota passes but copy unavailable -> NO_AVAILABLE_COPY")
    void testNoAvailableCopy() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(validReader));
        when(borrowingQuotaPolicy.getQuotaSnapshot(eq(1L), any())).thenReturn(
                BorrowingQuotaPolicy.QuotaSnapshot.builder().activeMembership(true).validPlanQuota(true).remainingQuota(1L).build()
        );
        when(resourceRepository.findById(100L)).thenReturn(Optional.of(validResource));
        when(borrowRequestRepository.existsByReaderIdAndResourceIdAndStatusIn(eq(1L), eq(100L), any())).thenReturn(false);
        when(borrowingRepository.existsActiveBorrowingByReaderIdAndResourceId(1L, 100L)).thenReturn(false);
        when(borrowRequestRepository.countByReaderIdAndStatusIn(eq(1L), any())).thenReturn(0L);
        when(borrowingRepository.countActiveBorrowingsByReaderId(1L)).thenReturn(0L);
        when(circulationPolicy.commitmentLimit()).thenReturn(5L);
        when(physicalItemRepository.countByResourceId(100L)).thenReturn(1L);
        when(physicalItemRepository.findForUpdate(eq(100L), eq(InventoryStatus.ACTIVE), eq(CirculationStatus.AVAILABLE), eq(PageRequest.of(0, 1))))
                .thenReturn(List.of());

        BorrowFlowException ex = assertThrows(BorrowFlowException.class, () -> borrowService.createRequest(1L, 100L));
        assertEquals(BorrowErrorCode.NO_AVAILABLE_COPY.name(), ex.getErrorCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        
        verify(borrowRequestRepository, never()).save(any());
    }
}


    @Test
    @DisplayName("D. Invalid/null monthlyBorrowQuota -> request rejected safely -> no reservation")
    void testInvalidPlanQuota() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(validReader));
        when(borrowingQuotaPolicy.getQuotaSnapshot(eq(1L), any())).thenReturn(
                BorrowingQuotaPolicy.QuotaSnapshot.builder().activeMembership(true).validPlanQuota(false).build()
        );

        BorrowFlowException ex = assertThrows(BorrowFlowException.class, () -> borrowService.createRequest(1L, 100L));
        assertEquals(BorrowErrorCode.INVALID_PLAN_QUOTA.name(), ex.getErrorCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        
        verify(physicalItemRepository, never()).findForUpdate(any(), any(), any(), any());
    
    @Test
    @DisplayName("I. Final-copy behavior remains intact: quota passes but copy unavailable -> NO_AVAILABLE_COPY")
    void testNoAvailableCopy() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(validReader));
        when(borrowingQuotaPolicy.getQuotaSnapshot(eq(1L), any())).thenReturn(
                BorrowingQuotaPolicy.QuotaSnapshot.builder().activeMembership(true).validPlanQuota(true).remainingQuota(1L).build()
        );
        when(resourceRepository.findById(100L)).thenReturn(Optional.of(validResource));
        when(borrowRequestRepository.existsByReaderIdAndResourceIdAndStatusIn(eq(1L), eq(100L), any())).thenReturn(false);
        when(borrowingRepository.existsActiveBorrowingByReaderIdAndResourceId(1L, 100L)).thenReturn(false);
        when(borrowRequestRepository.countByReaderIdAndStatusIn(eq(1L), any())).thenReturn(0L);
        when(borrowingRepository.countActiveBorrowingsByReaderId(1L)).thenReturn(0L);
        when(circulationPolicy.commitmentLimit()).thenReturn(5L);
        when(physicalItemRepository.countByResourceId(100L)).thenReturn(1L);
        when(physicalItemRepository.findForUpdate(eq(100L), eq(InventoryStatus.ACTIVE), eq(CirculationStatus.AVAILABLE), eq(PageRequest.of(0, 1))))
                .thenReturn(List.of());

        BorrowFlowException ex = assertThrows(BorrowFlowException.class, () -> borrowService.createRequest(1L, 100L));
        assertEquals(BorrowErrorCode.NO_AVAILABLE_COPY.name(), ex.getErrorCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        
        verify(borrowRequestRepository, never()).save(any());
    }
}


    @Test
    @DisplayName("E. Quota = 1, used = 0, active commitments = 0 -> one request allowed")
    void testQuotaOneUsedZero() {
        mockCommonSuccessPath();
        when(borrowingQuotaPolicy.getQuotaSnapshot(eq(1L), any())).thenReturn(
                BorrowingQuotaPolicy.QuotaSnapshot.builder().activeMembership(true).validPlanQuota(true).remainingQuota(1L).build()
        );

        borrowService.createRequest(1L, 100L);

        verify(borrowRequestRepository).save(any());
    
    @Test
    @DisplayName("I. Final-copy behavior remains intact: quota passes but copy unavailable -> NO_AVAILABLE_COPY")
    void testNoAvailableCopy() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(validReader));
        when(borrowingQuotaPolicy.getQuotaSnapshot(eq(1L), any())).thenReturn(
                BorrowingQuotaPolicy.QuotaSnapshot.builder().activeMembership(true).validPlanQuota(true).remainingQuota(1L).build()
        );
        when(resourceRepository.findById(100L)).thenReturn(Optional.of(validResource));
        when(borrowRequestRepository.existsByReaderIdAndResourceIdAndStatusIn(eq(1L), eq(100L), any())).thenReturn(false);
        when(borrowingRepository.existsActiveBorrowingByReaderIdAndResourceId(1L, 100L)).thenReturn(false);
        when(borrowRequestRepository.countByReaderIdAndStatusIn(eq(1L), any())).thenReturn(0L);
        when(borrowingRepository.countActiveBorrowingsByReaderId(1L)).thenReturn(0L);
        when(circulationPolicy.commitmentLimit()).thenReturn(5L);
        when(physicalItemRepository.countByResourceId(100L)).thenReturn(1L);
        when(physicalItemRepository.findForUpdate(eq(100L), eq(InventoryStatus.ACTIVE), eq(CirculationStatus.AVAILABLE), eq(PageRequest.of(0, 1))))
                .thenReturn(List.of());

        BorrowFlowException ex = assertThrows(BorrowFlowException.class, () -> borrowService.createRequest(1L, 100L));
        assertEquals(BorrowErrorCode.NO_AVAILABLE_COPY.name(), ex.getErrorCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        
        verify(borrowRequestRepository, never()).save(any());
    }
}


    @Test
    @DisplayName("H. Existing circulation commitment limit still applies independently")
    void testCommitmentLimitStillApplies() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(validReader));
        when(resourceRepository.findById(100L)).thenReturn(Optional.of(validResource));
        when(borrowingQuotaPolicy.getQuotaSnapshot(eq(1L), any())).thenReturn(
                BorrowingQuotaPolicy.QuotaSnapshot.builder().activeMembership(true).validPlanQuota(true).remainingQuota(5L).build()
        );
        when(borrowRequestRepository.existsByReaderIdAndResourceIdAndStatusIn(eq(1L), eq(100L), any())).thenReturn(false);
        when(borrowingRepository.existsActiveBorrowingByReaderIdAndResourceId(1L, 100L)).thenReturn(false);
        
        when(borrowRequestRepository.countByReaderIdAndStatusIn(eq(1L), any())).thenReturn(3L);
        when(borrowingRepository.countActiveBorrowingsByReaderId(1L)).thenReturn(2L);
        when(circulationPolicy.commitmentLimit()).thenReturn(5L);

        BorrowFlowException ex = assertThrows(BorrowFlowException.class, () -> borrowService.createRequest(1L, 100L));
        assertEquals(BorrowErrorCode.BORROWING_LIMIT_REACHED.name(), ex.getErrorCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    
    @Test
    @DisplayName("I. Final-copy behavior remains intact: quota passes but copy unavailable -> NO_AVAILABLE_COPY")
    void testNoAvailableCopy() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(validReader));
        when(borrowingQuotaPolicy.getQuotaSnapshot(eq(1L), any())).thenReturn(
                BorrowingQuotaPolicy.QuotaSnapshot.builder().activeMembership(true).validPlanQuota(true).remainingQuota(1L).build()
        );
        when(resourceRepository.findById(100L)).thenReturn(Optional.of(validResource));
        when(borrowRequestRepository.existsByReaderIdAndResourceIdAndStatusIn(eq(1L), eq(100L), any())).thenReturn(false);
        when(borrowingRepository.existsActiveBorrowingByReaderIdAndResourceId(1L, 100L)).thenReturn(false);
        when(borrowRequestRepository.countByReaderIdAndStatusIn(eq(1L), any())).thenReturn(0L);
        when(borrowingRepository.countActiveBorrowingsByReaderId(1L)).thenReturn(0L);
        when(circulationPolicy.commitmentLimit()).thenReturn(5L);
        when(physicalItemRepository.countByResourceId(100L)).thenReturn(1L);
        when(physicalItemRepository.findForUpdate(eq(100L), eq(InventoryStatus.ACTIVE), eq(CirculationStatus.AVAILABLE), eq(PageRequest.of(0, 1))))
                .thenReturn(List.of());

        BorrowFlowException ex = assertThrows(BorrowFlowException.class, () -> borrowService.createRequest(1L, 100L));
        assertEquals(BorrowErrorCode.NO_AVAILABLE_COPY.name(), ex.getErrorCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        
        verify(borrowRequestRepository, never()).save(any());
    }
}


    @Test
    @DisplayName("I. Final-copy behavior remains intact: quota passes but copy unavailable -> NO_AVAILABLE_COPY")
    void testNoAvailableCopy() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(validReader));
        when(borrowingQuotaPolicy.getQuotaSnapshot(eq(1L), any())).thenReturn(
                BorrowingQuotaPolicy.QuotaSnapshot.builder().activeMembership(true).validPlanQuota(true).remainingQuota(1L).build()
        );
        when(resourceRepository.findById(100L)).thenReturn(Optional.of(validResource));
        when(borrowRequestRepository.existsByReaderIdAndResourceIdAndStatusIn(eq(1L), eq(100L), any())).thenReturn(false);
        when(borrowingRepository.existsActiveBorrowingByReaderIdAndResourceId(1L, 100L)).thenReturn(false);
        when(borrowRequestRepository.countByReaderIdAndStatusIn(eq(1L), any())).thenReturn(0L);
        when(borrowingRepository.countActiveBorrowingsByReaderId(1L)).thenReturn(0L);
        when(circulationPolicy.commitmentLimit()).thenReturn(5L);
        when(physicalItemRepository.countByResourceId(100L)).thenReturn(1L);
        when(physicalItemRepository.findForUpdate(eq(100L), eq(InventoryStatus.ACTIVE), eq(CirculationStatus.AVAILABLE), eq(PageRequest.of(0, 1))))
                .thenReturn(List.of());

        BorrowFlowException ex = assertThrows(BorrowFlowException.class, () -> borrowService.createRequest(1L, 100L));
        assertEquals(BorrowErrorCode.NO_AVAILABLE_COPY.name(), ex.getErrorCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        
        verify(borrowRequestRepository, never()).save(any());
    }
}
