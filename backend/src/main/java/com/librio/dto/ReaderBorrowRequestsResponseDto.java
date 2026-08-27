package com.librio.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReaderBorrowRequestsResponseDto {
    private List<ReaderBorrowRequestItemDto> activeRequests;
    private List<ReaderBorrowRequestItemDto> recentOutcomes;
}
