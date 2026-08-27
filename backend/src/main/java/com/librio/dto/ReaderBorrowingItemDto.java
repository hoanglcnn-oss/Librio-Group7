package com.librio.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class ReaderBorrowingItemDto {
    private Long id;
    private ResourceSummaryDto resource;
    private OffsetDateTime borrowedAt;
    private OffsetDateTime dueDate;
}
