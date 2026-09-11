package com.librio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReaderMembershipDto {
    private String status;
    private boolean membershipEligible;
    private MembershipPlanDto plan;
    private OffsetDateTime startsAt;
    private OffsetDateTime expiresAt;
    private LatestPaymentDto latestPayment;
}
