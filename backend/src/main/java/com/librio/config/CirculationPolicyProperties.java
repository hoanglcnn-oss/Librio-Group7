package com.librio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "librio.circulation")
public record CirculationPolicyProperties(
        int commitmentLimit,
        Duration requestExpiration,
        Duration pickupExpiration,
        Duration loanPeriod,
        Duration expirationScanInterval
) {
    public CirculationPolicyProperties {
        if (commitmentLimit <= 0) {
            commitmentLimit = 3;
        }
        requestExpiration = requestExpiration == null ? Duration.ofDays(1) : requestExpiration;
        pickupExpiration = pickupExpiration == null ? Duration.ofDays(3) : pickupExpiration;
        loanPeriod = loanPeriod == null ? Duration.ofDays(14) : loanPeriod;
        expirationScanInterval = expirationScanInterval == null ? Duration.ofSeconds(60) : expirationScanInterval;
    }
}
