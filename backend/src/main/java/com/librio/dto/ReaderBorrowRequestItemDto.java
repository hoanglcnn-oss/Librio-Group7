package com.librio.dto;

import com.librio.domain.BorrowRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class ReaderBorrowRequestItemDto {
    private Long id;
    private ResourceSummaryDto resource;
    private BorrowRequestStatus status;
    private OffsetDateTime requestedAt;
    private OffsetDateTime readyAt;
    private OffsetDateTime expiresAt;
    private OffsetDateTime fulfilledAt;
    private OffsetDateTime rejectedAt;
    private OffsetDateTime statusUpdatedAt;
}
