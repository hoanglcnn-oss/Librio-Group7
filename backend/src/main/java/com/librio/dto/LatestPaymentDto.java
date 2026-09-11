package com.librio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LatestPaymentDto {
    private Long id;
    private String status;
    private BigDecimal amount;
    private String currency;
    private OffsetDateTime completedAt;
}
