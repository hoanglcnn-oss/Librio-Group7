package com.librio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VnpayPaymentResponseDto {
    private String paymentUrl;
}