package com.librio.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DigitalAccessDto {
    private Long resourceId;
    private boolean canRead;
    private String contentUrl;
    private boolean temporaryUrl;
}
