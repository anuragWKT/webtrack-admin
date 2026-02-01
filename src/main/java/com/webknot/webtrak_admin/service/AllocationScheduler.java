package com.webknot.webtrak_admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AllocationScheduler {

    private final AllocationService allocationService;

    @Scheduled(cron = "0 0 0 * * *")
    public void deactivateExpiredAllocations() {
        allocationService.deactivateExpiredAllocations();
    }
}
