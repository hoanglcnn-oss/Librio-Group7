package com.librio.service;

import com.librio.domain.BorrowRequestStatus;
import com.librio.domain.MembershipPlan;
import com.librio.domain.MembershipSubscription;
import com.librio.repository.BorrowRequestRepository;
import com.librio.repository.BorrowingRepository;
import com.librio.repository.MembershipSubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowingQuotaPolicyTest {

    @Mock
    private MembershipSubscriptionRepository subscriptionRepository;

    @Mock
    private BorrowingRepository borrowingRepository;

    @Mock
    private BorrowRequestRepository borrowRequestRepository;

    @InjectMocks
    private BorrowingQuotaPolicy policy;

    @Test
    @DisplayName("No active membership returns snapshot indicating no membership")
    void testNoActiveMembership() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 25, 10, 0);
        when(subscriptionRepository.findActiveByAccountId(1L, now)).thenReturn(Optional.empty());

        BorrowingQuotaPolicy.QuotaSnapshot snapshot = policy.getQuotaSnapshot(1L, now);

        assertFalse(snapshot.isActiveMembership());
        assertFalse(snapshot.isValidPlanQuota());
        assertNull(snapshot.getPlanQuota());
        assertEquals(0, snapshot.getRemainingQuota());
    }

    @Test
    @DisplayName("Active membership with null quota returns valid snapshot with no valid plan quota")
    void testActiveMembershipNullQuota() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 25, 10, 0);
        MembershipPlan plan = MembershipPlan.builder().monthlyBorrowQuota(null).build();
        MembershipSubscription sub = MembershipSubscription.builder().plan(plan).build();

        when(subscriptionRepository.findActiveByAccountId(1L, now)).thenReturn(Optional.of(sub));

        BorrowingQuotaPolicy.QuotaSnapshot snapshot = policy.getQuotaSnapshot(1L, now);

        assertTrue(snapshot.isActiveMembership());
        assertFalse(snapshot.isValidPlanQuota());
        assertNull(snapshot.getPlanQuota());
        assertEquals(0, snapshot.getRemainingQuota());
    }

    @Test
    @DisplayName("Calculate first cycle correctly")
    void testFirstCycle() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 25, 10, 0);
        LocalDateTime startsAt = LocalDateTime.of(2026, 9, 20, 10, 0);
        LocalDateTime expiresAt = LocalDateTime.of(2027, 9, 20, 10, 0);

        MembershipPlan plan = MembershipPlan.builder().monthlyBorrowQuota(5).build();
        MembershipSubscription sub = MembershipSubscription.builder()
                .plan(plan)
                .startsAt(startsAt)
                .expiresAt(expiresAt)
                .build();

        when(subscriptionRepository.findActiveByAccountId(1L, now)).thenReturn(Optional.of(sub));
        when(borrowingRepository.countBorrowingsByReaderIdAndPeriod(eq(1L), any(), any())).thenReturn(0L);
        when(borrowRequestRepository.countByReaderIdAndStatusIn(eq(1L), any())).thenReturn(0L);

        BorrowingQuotaPolicy.QuotaSnapshot snapshot = policy.getQuotaSnapshot(1L, now);

        assertTrue(snapshot.isActiveMembership());
        assertTrue(snapshot.isValidPlanQuota());
        assertEquals(5, snapshot.getPlanQuota());
        assertEquals(startsAt, snapshot.getPeriodStart());
        assertEquals(LocalDateTime.of(2026, 10, 20, 10, 0), snapshot.getPeriodEnd());
        assertEquals(5, snapshot.getRemainingQuota());
    }

    @Test
    @DisplayName("Calculate later cycle correctly including exact cycle boundary")
    void testLaterCycle() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 20, 10, 0); // Exact boundary
        LocalDateTime startsAt = LocalDateTime.of(2026, 9, 20, 10, 0);
        LocalDateTime expiresAt = LocalDateTime.of(2027, 9, 20, 10, 0);

        MembershipPlan plan = MembershipPlan.builder().monthlyBorrowQuota(5).build();
        MembershipSubscription sub = MembershipSubscription.builder()
                .plan(plan)
                .startsAt(startsAt)
                .expiresAt(expiresAt)
                .build();

        when(subscriptionRepository.findActiveByAccountId(1L, now)).thenReturn(Optional.of(sub));
        when(borrowingRepository.countBorrowingsByReaderIdAndPeriod(eq(1L), any(), any())).thenReturn(0L);
        when(borrowRequestRepository.countByReaderIdAndStatusIn(eq(1L), any())).thenReturn(0L);

        BorrowingQuotaPolicy.QuotaSnapshot snapshot = policy.getQuotaSnapshot(1L, now);

        assertEquals(LocalDateTime.of(2026, 10, 20, 10, 0), snapshot.getPeriodStart());
        assertEquals(LocalDateTime.of(2026, 11, 20, 10, 0), snapshot.getPeriodEnd());
    }

    @Test
    @DisplayName("Final cycle capped at subscription expiresAt")
    void testFinalCycleCapped() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 15, 10, 0);
        LocalDateTime startsAt = LocalDateTime.of(2026, 9, 20, 10, 0);
        // Expires middle of the month
        LocalDateTime expiresAt = LocalDateTime.of(2026, 10, 18, 10, 0);

        MembershipPlan plan = MembershipPlan.builder().monthlyBorrowQuota(5).build();
        MembershipSubscription sub = MembershipSubscription.builder()
                .plan(plan)
                .startsAt(startsAt)
                .expiresAt(expiresAt)
                .build();

        when(subscriptionRepository.findActiveByAccountId(1L, now)).thenReturn(Optional.of(sub));
        when(borrowingRepository.countBorrowingsByReaderIdAndPeriod(eq(1L), any(), any())).thenReturn(0L);
        when(borrowRequestRepository.countByReaderIdAndStatusIn(eq(1L), any())).thenReturn(0L);

        BorrowingQuotaPolicy.QuotaSnapshot snapshot = policy.getQuotaSnapshot(1L, now);

        assertEquals(startsAt, snapshot.getPeriodStart());
        assertEquals(expiresAt, snapshot.getPeriodEnd());
    }

    @Test
    @DisplayName("Quota calculation with used borrowings and active commitments")
    void testQuotaCalculation() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 25, 10, 0);
        LocalDateTime startsAt = LocalDateTime.of(2026, 9, 20, 10, 0);
        LocalDateTime expiresAt = LocalDateTime.of(2027, 9, 20, 10, 0);

        MembershipPlan plan = MembershipPlan.builder().monthlyBorrowQuota(5).build();
        MembershipSubscription sub = MembershipSubscription.builder()
                .plan(plan)
                .startsAt(startsAt)
                .expiresAt(expiresAt)
                .build();

        when(subscriptionRepository.findActiveByAccountId(1L, now)).thenReturn(Optional.of(sub));
        when(borrowingRepository.countBorrowingsByReaderIdAndPeriod(
                eq(1L), 
                eq(startsAt), 
                eq(LocalDateTime.of(2026, 10, 20, 10, 0)))
        ).thenReturn(3L);
        when(borrowRequestRepository.countByReaderIdAndStatusIn(
                eq(1L), 
                eq(List.of(BorrowRequestStatus.REQUESTED, BorrowRequestStatus.READY_FOR_PICKUP)))
        ).thenReturn(1L);

        BorrowingQuotaPolicy.QuotaSnapshot snapshot = policy.getQuotaSnapshot(1L, now);

        assertEquals(3, snapshot.getUsedBorrowings());
        assertEquals(1, snapshot.getActiveCommitments());
        assertEquals(1, snapshot.getRemainingQuota());
    }
}