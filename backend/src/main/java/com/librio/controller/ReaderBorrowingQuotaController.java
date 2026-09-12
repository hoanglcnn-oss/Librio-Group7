package com.librio.controller;

import com.librio.domain.Account;
import com.librio.dto.BorrowingQuotaDto;
import com.librio.exception.BorrowErrorCode;
import com.librio.exception.BorrowFlowException;
import com.librio.security.CurrentAccountService;
import com.librio.service.BorrowingQuotaPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/me/borrowing-quota")
@RequiredArgsConstructor
public class ReaderBorrowingQuotaController {

    private final CurrentAccountService currentAccountService;
    private final BorrowingQuotaPolicy borrowingQuotaPolicy;

    @GetMapping
    public ResponseEntity<BorrowingQuotaDto> getQuota() {
        Account reader = currentAccountService.getCurrentAccount();
        LocalDateTime now = LocalDateTime.now();
        BorrowingQuotaPolicy.QuotaSnapshot snapshot = borrowingQuotaPolicy.getQuotaSnapshot(reader.getId(), now);

        if (!snapshot.isActiveMembership()) {
            throw new BorrowFlowException(BorrowErrorCode.ACTIVE_MEMBERSHIP_REQUIRED.name(), HttpStatus.FORBIDDEN, "Active membership required");
        }

        if (!snapshot.isValidPlanQuota()) {
            throw new BorrowFlowException(BorrowErrorCode.INVALID_PLAN_QUOTA.name(), HttpStatus.INTERNAL_SERVER_ERROR, "Membership plan quota is invalid");
        }

        BorrowingQuotaDto dto = BorrowingQuotaDto.builder()
                .planQuota(snapshot.getPlanQuota())
                .usedBorrowings(snapshot.getUsedBorrowings())
                .activeCommitments(snapshot.getActiveCommitments())
                .remainingQuota(snapshot.getRemainingQuota())
                .periodStart(snapshot.getPeriodStart())
                .periodEnd(snapshot.getPeriodEnd())
                .build();

        return ResponseEntity.ok(dto);
    }
}