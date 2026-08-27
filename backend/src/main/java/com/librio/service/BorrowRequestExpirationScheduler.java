package com.librio.service;

import com.librio.config.CirculationPolicyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BorrowRequestExpirationScheduler implements SchedulingConfigurer {

    private final BorrowService borrowService;
    private final CirculationPolicyProperties circulationPolicy;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        PeriodicTrigger trigger = new PeriodicTrigger(circulationPolicy.expirationScanInterval());
        trigger.setFixedRate(false);
        taskRegistrar.addTriggerTask(borrowService::expireDueRequests, trigger);
    }
}
