package com.librio.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CockpitActiveOperationDto {
    private String type; // "BORROWING" or "BORROW_REQUEST"
    private Long id;
    private String status; // present for BORROW_REQUEST e.g. "REQUESTED", "READY_FOR_PICKUP"
    private Long borrowRequestId; // present for BORROWING
    private ReaderSummaryDto reader;
    private OffsetDateTime requestedAt; // present for BORROW_REQUEST
    private OffsetDateTime statusUpdatedAt; // present for BORROW_REQUEST
    private OffsetDateTime expiresAt; // present for BORROW_REQUEST
    private OffsetDateTime borrowedAt; // present for BORROWING
    private OffsetDateTime dueAt; // present for BORROWING
    private Boolean overdue; // present for BORROWING
}
