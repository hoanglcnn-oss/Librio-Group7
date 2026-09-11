package com.librio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipPlanDto {
    private Long id;
    private String code;
    private String name;
    private Integer durationMonths;
    private BigDecimal priceAmount;
    private String currency;
    private Integer monthlyBorrowQuota;
}
