package com.librio.controller;

import com.librio.domain.Account;
import com.librio.exception.BorrowErrorCode;
import com.librio.exception.BorrowFlowException;
import com.librio.security.CurrentAccountService;
import com.librio.service.BorrowingQuotaPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.librio.dto.BorrowingQuotaDto;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReaderBorrowingQuotaControllerTest {

    @Mock
    private CurrentAccountService currentAccountService;

    @Mock
    private BorrowingQuotaPolicy borrowingQuotaPolicy;

    @InjectMocks
    private ReaderBorrowingQuotaController controller;

    private Account validReader;

    @BeforeEach
    void setUp() {
        validReader = Account.builder().id(1L).build();
    }

    @Test
    void getQuota_activeMembership_returnsDto() {
        when(currentAccountService.getCurrentAccount()).thenReturn(validReader);
        
        LocalDateTime start = LocalDateTime.now().minusDays(10);
        LocalDateTime end = start.plusMonths(1);
        
        BorrowingQuotaPolicy.QuotaSnapshot snapshot = BorrowingQuotaPolicy.QuotaSnapshot.builder()
                .activeMembership(true)
                .validPlanQuota(true)
                .planQuota(20)
                .usedBorrowings(7L)
                .activeCommitments(2L)
                .remainingQuota(11L)
                .periodStart(start)
                .periodEnd(end)
                .build();
                
        when(borrowingQuotaPolicy.getQuotaSnapshot(eq(1L), any())).thenReturn(snapshot);

        ResponseEntity<BorrowingQuotaDto> response = controller.getQuota();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        BorrowingQuotaDto dto = response.getBody();
        assertNotNull(dto);
        assertEquals(20, dto.getPlanQuota());
        assertEquals(7L, dto.getUsedBorrowings());
        assertEquals(2L, dto.getActiveCommitments());
        assertEquals(11L, dto.getRemainingQuota());
        assertEquals(start, dto.getPeriodStart());
        assertEquals(end, dto.getPeriodEnd());
    }

    @Test
    void getQuota_noActiveMembership_throwsForbidden() {
        when(currentAccountService.getCurrentAccount()).thenReturn(validReader);
        BorrowingQuotaPolicy.QuotaSnapshot snapshot = BorrowingQuotaPolicy.QuotaSnapshot.builder()
                .activeMembership(false)
                .build();
        when(borrowingQuotaPolicy.getQuotaSnapshot(eq(1L), any())).thenReturn(snapshot);

        BorrowFlowException ex = assertThrows(BorrowFlowException.class, () -> controller.getQuota());
        assertEquals(BorrowErrorCode.ACTIVE_MEMBERSHIP_REQUIRED.name(), ex.getCode());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void getQuota_invalidPlanQuota_throwsInternalServerError() {
        when(currentAccountService.getCurrentAccount()).thenReturn(validReader);
        BorrowingQuotaPolicy.QuotaSnapshot snapshot = BorrowingQuotaPolicy.QuotaSnapshot.builder()
                .activeMembership(true)
                .validPlanQuota(false)
                .build();
        when(borrowingQuotaPolicy.getQuotaSnapshot(eq(1L), any())).thenReturn(snapshot);

        BorrowFlowException ex = assertThrows(BorrowFlowException.class, () -> controller.getQuota());
        assertEquals(BorrowErrorCode.INVALID_PLAN_QUOTA.name(), ex.getCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
    }
}