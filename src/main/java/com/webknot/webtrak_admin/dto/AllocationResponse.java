package com.webknot.webtrak_admin.dto;

import com.webknot.webtrak_admin.enums.AllocationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AllocationResponse {
    private Long id;
    private Long userId;
    private String projectCode;
    private String projectName;
    private AllocationType allocationType;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate lockedInDate;
    private Double hoursPerDay;
    private String allocationRole;
    private boolean manager;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
