package com.librio.dto;

import com.librio.domain.BorrowRequestStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BorrowRequestDto {
    private Long id;
    private Long readerId;
    private Long resourceId;
    private Long physicalItemId;
    private BorrowRequestStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime readyAt;
    private LocalDateTime expiresAt;
    private LocalDateTime fulfilledAt;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
}
