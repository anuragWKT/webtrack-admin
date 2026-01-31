package com.webknot.webtrak_admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserAllocationSummaryResponse {
    private Long userId;
    private long projectCount;
    private double capacityPerWeek;
}
