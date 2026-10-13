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

import java.util.List;
import java.util.Optional;

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

    @InjectMocks
    private BorrowService borrowService;

    private Account institutionalReader;
    private Resource validResource;
    private PhysicalItem availableItem;

    @BeforeEach
    void setUp() {
        institutionalReader = Account.builder()
                .id(1L)
                .role(AccountRole.READER)
                .readerType(ReaderType.INSTITUTIONAL)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        validResource = Resource.builder().id(100L).build();
        availableItem = PhysicalItem.builder().id(1000L).circulationStatus(CirculationStatus.AVAILABLE).build();
    }

    private void mockCommonSuccessPath() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(institutionalReader));
        when(resourceRepository.findById(100L)).thenReturn(Optional.of(validResource));
        when(borrowRequestRepository.existsByReaderIdAndResourceIdAndStatusIn(eq(1L), eq(100L), any())).thenReturn(false);
        when(borrowingRepository.existsActiveBorrowingByReaderIdAndResourceId(1L, 100L)).thenReturn(false);
        when(borrowRequestRepository.countByReaderIdAndStatusIn(eq(1L), any())).thenReturn(0L);
        when(borrowingRepository.countActiveBorrowingsByReaderId(1L)).thenReturn(0L);
        when(circulationPolicy.commitmentLimit()).thenReturn(5);
        when(physicalItemRepository.countByResourceId(100L)).thenReturn(1L);
        when(physicalItemRepository.findForUpdate(eq(100L), eq(InventoryStatus.ACTIVE), eq(CirculationStatus.AVAILABLE), eq(PageRequest.of(0, 1))))
                .thenReturn(List.of(availableItem));
        when(borrowRequestRepository.save(any())).thenAnswer(inv -> {
            BorrowRequest req = inv.getArgument(0);
            req.setId(999L);
            return req;
        });
    }

    @Test
    @DisplayName("1. external reader is rejected with READER_INELIGIBLE")
    void testExternalReaderRejected() {
        Account externalReader = Account.builder()
                .id(2L)
                .role(AccountRole.READER)
                .readerType(ReaderType.EXTERNAL)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(externalReader));

        BorrowFlowException ex = assertThrows(BorrowFlowException.class, () -> borrowService.createRequest(2L, 100L));
        assertEquals(BorrowErrorCode.READER_INELIGIBLE.name(), ex.getCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());

        verify(physicalItemRepository, never()).findForUpdate(any(), any(), any(), any());
        verify(borrowRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("2. disabled reader is rejected with READER_INELIGIBLE")
    void testDisabledReaderRejected() {
        Account disabledReader = Account.builder()
                .id(3L)
                .role(AccountRole.READER)
                .readerType(ReaderType.INSTITUTIONAL)
                .accountStatus(AccountStatus.DISABLED)
                .build();
        when(accountRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(disabledReader));

        BorrowFlowException ex = assertThrows(BorrowFlowException.class, () -> borrowService.createRequest(3L, 100L));
        assertEquals(BorrowErrorCode.READER_INELIGIBLE.name(), ex.getCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());

        verify(physicalItemRepository, never()).findForUpdate(any(), any(), any(), any());
        verify(borrowRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. institutional reader creates request without membership or quota check")
    void testInstitutionalReaderSuccessWithoutQuotaOrMembership() {
        mockCommonSuccessPath();

        borrowService.createRequest(1L, 100L);

        assertEquals(CirculationStatus.RESERVED, availableItem.getCirculationStatus());
        verify(borrowRequestRepository).save(any());
    }

    @Test
    @DisplayName("4. commitment limit still applies independently of membership/quota")
    void testCommitmentLimitStillApplies() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(institutionalReader));
        when(resourceRepository.findById(100L)).thenReturn(Optional.of(validResource));
        when(borrowRequestRepository.existsByReaderIdAndResourceIdAndStatusIn(eq(1L), eq(100L), any())).thenReturn(false);
        when(borrowingRepository.existsActiveBorrowingByReaderIdAndResourceId(1L, 100L)).thenReturn(false);

        when(borrowRequestRepository.countByReaderIdAndStatusIn(eq(1L), any())).thenReturn(3L);
        when(borrowingRepository.countActiveBorrowingsByReaderId(1L)).thenReturn(2L);
        when(circulationPolicy.commitmentLimit()).thenReturn(5);

        BorrowFlowException ex = assertThrows(BorrowFlowException.class, () -> borrowService.createRequest(1L, 100L));
        assertEquals(BorrowErrorCode.BORROWING_LIMIT_REACHED.name(), ex.getCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());

        verify(physicalItemRepository, never()).findForUpdate(any(), any(), any(), any());
        verify(borrowRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("5. reader eligible but no available copy -> NO_AVAILABLE_COPY")
    void testNoAvailableCopy() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(institutionalReader));
        when(resourceRepository.findById(100L)).thenReturn(Optional.of(validResource));
        when(borrowRequestRepository.existsByReaderIdAndResourceIdAndStatusIn(eq(1L), eq(100L), any())).thenReturn(false);
        when(borrowingRepository.existsActiveBorrowingByReaderIdAndResourceId(1L, 100L)).thenReturn(false);

        when(borrowRequestRepository.countByReaderIdAndStatusIn(eq(1L), any())).thenReturn(0L);
        when(borrowingRepository.countActiveBorrowingsByReaderId(1L)).thenReturn(0L);
        when(circulationPolicy.commitmentLimit()).thenReturn(5);

        when(physicalItemRepository.countByResourceId(100L)).thenReturn(1L);
        when(physicalItemRepository.findForUpdate(eq(100L), eq(InventoryStatus.ACTIVE), eq(CirculationStatus.AVAILABLE), eq(PageRequest.of(0, 1))))
                .thenReturn(List.of());

        BorrowFlowException ex = assertThrows(BorrowFlowException.class, () -> borrowService.createRequest(1L, 100L));
        assertEquals(BorrowErrorCode.NO_AVAILABLE_COPY.name(), ex.getCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());

        verify(borrowRequestRepository, never()).save(any());
    }
}