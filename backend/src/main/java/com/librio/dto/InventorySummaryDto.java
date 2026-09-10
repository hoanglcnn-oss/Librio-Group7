package com.librio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventorySummaryDto {
    private long totalCopies;
    private long availableCopies;
    private long reservedCopies;
    private long borrowedCopies;
    private long attentionCopies;
}
