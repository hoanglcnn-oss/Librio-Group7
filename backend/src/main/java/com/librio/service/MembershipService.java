package com.librio.service;

import com.librio.domain.Account;
import com.librio.domain.MembershipPlan;
import com.librio.domain.MembershipSubscription;
import com.librio.domain.PaymentStatus;
import com.librio.domain.PaymentTransaction;
import com.librio.dto.LatestPaymentDto;
import com.librio.dto.MembershipPaymentRequestDto;
import com.librio.dto.MembershipPlanDto;
import com.librio.dto.ReaderMembershipDto;
import com.librio.exception.BorrowFlowException;
import com.librio.repository.AccountRepository;
import com.librio.repository.MembershipPlanRepository;
import com.librio.repository.MembershipSubscriptionRepository;
import com.librio.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipPlanRepository membershipPlanRepository;
    private final MembershipSubscriptionRepository membershipSubscriptionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public List<MembershipPlanDto> getActivePlans() {
        return membershipPlanRepository.findByActiveTrueOrderByIdAsc().stream()
                .map(this::toPlanDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReaderMembershipDto getCurrentMembership(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new BorrowFlowException("ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND, "Account not found"));

        LocalDateTime now = LocalDateTime.now();
        Optional<MembershipSubscription> latestSub = membershipSubscriptionRepository.findFirstByAccountIdOrderByStartsAtDescIdDesc(account.getId());
        Optional<PaymentTransaction> latestTx = paymentTransactionRepository.findFirstByAccountIdOrderByCompletedAtDescIdDesc(account.getId());

        return buildMembershipDto(account, latestSub, latestTx, now);
    }

    @Transactional
    public ReaderMembershipDto processPayment(String email, MembershipPaymentRequestDto request) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new BorrowFlowException("ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND, "Account not found"));

        // 1. Lock account to enforce activation boundary and serialize concurrent attempts for the same account
        Account lockedAccount = accountRepository.findByIdForUpdate(account.getId())
                .orElseThrow(() -> new BorrowFlowException("ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND, "Account not found"));

        // 2. Re-check membership eligibility
        if (!lockedAccount.isMembershipEligible()) {
            throw new BorrowFlowException("MEMBERSHIP_NOT_ELIGIBLE", HttpStatus.FORBIDDEN, "Account is not eligible for membership");
        }

        // 3. Validate outcome parameter
        String outcome = request.getOutcome() != null ? request.getOutcome().trim().toUpperCase() : "";
        if (!"SUCCESS".equals(outcome) && !"FAILED".equals(outcome)) {
            throw new BorrowFlowException("INVALID_PAYMENT_OUTCOME", HttpStatus.BAD_REQUEST, "Invalid payment outcome: " + request.getOutcome());
        }

        // 4. Validate plan
        if (request.getPlanId() == null) {
            throw new BorrowFlowException("MEMBERSHIP_PLAN_NOT_FOUND", HttpStatus.NOT_FOUND, "Membership plan not found");
        }

        MembershipPlan plan = membershipPlanRepository.findById(request.getPlanId())
                .orElseThrow(() -> new BorrowFlowException("MEMBERSHIP_PLAN_NOT_FOUND", HttpStatus.NOT_FOUND, "Membership plan not found"));

        if (!plan.isActive()) {
            throw new BorrowFlowException("MEMBERSHIP_PLAN_NOT_FOUND", HttpStatus.NOT_FOUND, "Membership plan is inactive");
        }

        // 5. Re-check effective active subscription inside the lock
        LocalDateTime now = LocalDateTime.now();
        Optional<MembershipSubscription> activeSub = membershipSubscriptionRepository.findActiveByAccountId(lockedAccount.getId(), now);
        if (activeSub.isPresent()) {
            throw new BorrowFlowException("ACTIVE_MEMBERSHIP_EXISTS", HttpStatus.CONFLICT, "Account already has an active membership subscription");
        }

        // 6. Process payment lifecycle
        if ("FAILED".equals(outcome)) {
            PaymentTransaction failedTx = PaymentTransaction.builder()
                    .account(lockedAccount)
                    .plan(plan)
                    .amount(plan.getPriceAmount())
                    .currency(plan.getCurrency())
                    .status(PaymentStatus.FAILED)
                    .createdAt(now)
                    .completedAt(now)
                    .build();
            paymentTransactionRepository.save(failedTx);

            return getCurrentMembership(email);
        }

        PaymentTransaction successTx = PaymentTransaction.builder()
                .account(lockedAccount)
                .plan(plan)
                .amount(plan.getPriceAmount())
                .currency(plan.getCurrency())
                .status(PaymentStatus.SUCCESS)
                .createdAt(now)
                .completedAt(now)
                .build();
        PaymentTransaction savedTx = paymentTransactionRepository.save(successTx);

        MembershipSubscription subscription = MembershipSubscription.builder()
                .account(lockedAccount)
                .plan(plan)
                .paymentTransaction(savedTx)
                .startsAt(now)
                .expiresAt(now.plusMonths(plan.getDurationMonths()))
                .build();
        MembershipSubscription savedSub = membershipSubscriptionRepository.save(subscription);

        return buildMembershipDto(lockedAccount, Optional.of(savedSub), Optional.of(savedTx), now);
    }

    private ReaderMembershipDto buildMembershipDto(
            Account account,
            Optional<MembershipSubscription> subOpt,
            Optional<PaymentTransaction> txOpt,
            LocalDateTime now
    ) {
        String status = "NONE";
        MembershipPlanDto planDto = null;
        OffsetDateTime startsAt = null;
        OffsetDateTime expiresAt = null;

        if (subOpt.isPresent()) {
            MembershipSubscription sub = subOpt.get();
            if (sub.getStartsAt() != null && sub.getExpiresAt() != null) {
                if (!sub.getStartsAt().isAfter(now) && sub.getExpiresAt().isAfter(now)) {
                    status = "ACTIVE";
                } else if (!sub.getExpiresAt().isAfter(now)) {
                    status = "EXPIRED";
                }
            }
            planDto = toPlanDto(sub.getPlan());
            startsAt = toOffsetDateTime(sub.getStartsAt());
            expiresAt = toOffsetDateTime(sub.getExpiresAt());
        }

        LatestPaymentDto latestPayment = txOpt.map(this::toLatestPaymentDto).orElse(null);

        return ReaderMembershipDto.builder()
                .status(status)
                .membershipEligible(account.isMembershipEligible())
                .plan(planDto)
                .startsAt(startsAt)
                .expiresAt(expiresAt)
                .latestPayment(latestPayment)
                .build();
    }

    private MembershipPlanDto toPlanDto(MembershipPlan plan) {
        if (plan == null) {
            return null;
        }
        return MembershipPlanDto.builder()
                .id(plan.getId())
                .code(plan.getCode())
                .name(plan.getName())
                .durationMonths(plan.getDurationMonths())
                .priceAmount(plan.getPriceAmount())
                .currency(plan.getCurrency())
                .monthlyBorrowQuota(plan.getMonthlyBorrowQuota())
                .build();
    }

    private LatestPaymentDto toLatestPaymentDto(PaymentTransaction tx) {
        if (tx == null) {
            return null;
        }
        return LatestPaymentDto.builder()
                .id(tx.getId())
                .status(tx.getStatus() != null ? tx.getStatus().name() : null)
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .completedAt(toOffsetDateTime(tx.getCompletedAt()))
                .build();
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime ldt) {
        if (ldt == null) {
            return null;
        }
        return ldt.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
