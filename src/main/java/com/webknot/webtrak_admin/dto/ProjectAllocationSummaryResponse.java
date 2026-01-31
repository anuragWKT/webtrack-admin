package com.webknot.webtrak_admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectAllocationSummaryResponse {
    private String projectCode;
    private String projectName;
    private long employeeCount;
    private double capacityPerWeek;
}
