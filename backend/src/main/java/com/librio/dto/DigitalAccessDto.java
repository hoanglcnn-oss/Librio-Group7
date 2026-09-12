package com.librio.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DigitalAccessDto {
    private Long resourceId;
    private DigitalAccessLevel accessLevel;
    private String previewUrl;
    private String contentUrl;
}
