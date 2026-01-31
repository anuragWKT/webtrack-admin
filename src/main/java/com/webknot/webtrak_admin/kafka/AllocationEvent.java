package com.webknot.webtrak_admin.kafka;

import com.webknot.webtrak_admin.enums.AllocationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationEvent {
    private String eventType;
    private Long allocationId;
    private Long userId;
    private String projectCode;
    private AllocationType allocationType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double hoursPerDay;
    private boolean manager;
    private boolean active;
    private LocalDateTime eventTime;
}
