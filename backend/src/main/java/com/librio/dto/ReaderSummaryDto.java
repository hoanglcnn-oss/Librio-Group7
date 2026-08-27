package com.librio.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReaderSummaryDto {
    private Long id;
    private String displayName;
}
