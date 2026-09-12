package com.librio.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class BorrowingQuotaDto {
    private Integer planQuota;
    private Long usedBorrowings;
    private Long activeCommitments;
    private Long remainingQuota;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
}