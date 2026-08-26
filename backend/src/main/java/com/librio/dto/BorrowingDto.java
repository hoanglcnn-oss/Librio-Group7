package com.librio.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BorrowingDto {
    private Long id;
    private Long borrowRequestId;
    private Long readerId;
    private Long physicalItemId;
    private LocalDateTime borrowedAt;
    private LocalDateTime dueAt;
}
