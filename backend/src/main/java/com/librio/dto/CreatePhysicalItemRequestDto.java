package com.librio.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePhysicalItemRequestDto {

    @NotBlank(message = "Barcode is required")
    private String barcode;

    @NotBlank(message = "Location is required")
    private String location;
}
