package com.librio.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VnpayPaymentRequestDto {
    @NotNull
    private Long planId;
}