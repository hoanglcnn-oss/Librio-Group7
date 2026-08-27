package com.librio.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class LibrarianBorrowingDto {
    private Long id;
    private Long borrowRequestId;
    private ReaderSummaryDto reader;
    private ResourceSummaryDto resource;
    private Long physicalItemId;
    private OffsetDateTime borrowedAt;
    private OffsetDateTime dueDate;
}
