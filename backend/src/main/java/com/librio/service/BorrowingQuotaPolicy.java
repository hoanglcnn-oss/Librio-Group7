package com.librio.service;

import com.librio.domain.BorrowRequestStatus;
import com.librio.domain.MembershipSubscription;
import com.librio.repository.BorrowRequestRepository;
import com.librio.repository.BorrowingRepository;
import com.librio.repository.MembershipSubscriptionRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BorrowingQuotaPolicy {

    private final MembershipSubscriptionRepository subscriptionRepository;
    private final BorrowingRepository borrowingRepository;
    private final BorrowRequestRepository borrowRequestRepository;

    @Getter
    @Builder
    public static class QuotaSnapshot {
        private final Integer planQuota;
        private final LocalDateTime periodStart;
        private final LocalDateTime periodEnd;
        private final long usedBorrowings;
        private final long activeCommitments;
        private final long remainingQuota;
        private final boolean activeMembership;
        private final boolean validPlanQuota;
    }

    public QuotaSnapshot getQuotaSnapshot(Long readerId, LocalDateTime serverNow) {
        // Find active subscription
        java.util.Optional<MembershipSubscription> activeSubOpt = subscriptionRepository.findActiveByAccountId(readerId, serverNow);
        if (activeSubOpt.isEmpty()) {
            return QuotaSnapshot.builder()
                    .activeMembership(false)
                    .validPlanQuota(false)
                    .planQuota(null)
                    .usedBorrowings(0)
                    .activeCommitments(0)
                    .remainingQuota(0)
                    .build();
        }

        MembershipSubscription subscription = activeSubOpt.get();
        Integer planQuota = subscription.getPlan().getMonthlyBorrowQuota();
        if (planQuota == null) {
            return QuotaSnapshot.builder()
                    .activeMembership(true)
                    .validPlanQuota(false)
                    .planQuota(null)
                    .usedBorrowings(0)
                    .activeCommitments(0)
                    .remainingQuota(0)
                    .build();
        }

        // Calculate cycle
        LocalDateTime cycleStart = subscription.getStartsAt();
        LocalDateTime cycleEnd = cycleStart.plusMonths(1);
        
        while (!serverNow.isBefore(cycleEnd)) {
            cycleStart = cycleEnd;
            cycleEnd = cycleStart.plusMonths(1);
        }

        if (cycleEnd.isAfter(subscription.getExpiresAt())) {
            cycleEnd = subscription.getExpiresAt();
        }

        long usedBorrowings = borrowingRepository.countBorrowingsByReaderIdAndPeriod(readerId, cycleStart, cycleEnd);
        long activeCommitments = borrowRequestRepository.countByReaderIdAndStatusIn(
                readerId, 
                List.of(BorrowRequestStatus.REQUESTED, BorrowRequestStatus.READY_FOR_PICKUP)
        );

        long remaining = planQuota - usedBorrowings - activeCommitments;
        if (remaining < 0) {
            remaining = 0;
        }

        return QuotaSnapshot.builder()
                .activeMembership(true)
                .validPlanQuota(true)
                .planQuota(planQuota)
                .periodStart(cycleStart)
                .periodEnd(cycleEnd)
                .usedBorrowings(usedBorrowings)
                .activeCommitments(activeCommitments)
                .remainingQuota(remaining)
                .build();
    }
}