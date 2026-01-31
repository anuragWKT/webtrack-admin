package com.webknot.webtrak_admin.dto;

import com.webknot.webtrak_admin.enums.AllocationType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AllocationRequest {

    @NotNull(message = "User id is required")
    private Long userId;

    @NotBlank(message = "Project code is required")
    @Size(max = 50, message = "Project code must be at most 50 characters")
    private String projectCode;

    @NotNull(message = "Allocation type is required")
    private AllocationType allocationType;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private LocalDate lockedInDate;

    @NotNull(message = "Hours per day is required")
    @DecimalMin(value = "0.1", message = "Hours per day must be greater than 0")
    @DecimalMax(value = "8.0", message = "Hours per day must be 8 or less")
    private Double hoursPerDay;

    @Size(max = 50, message = "Allocation role must be at most 50 characters")
    private String allocationRole;

    @NotNull(message = "Manager flag is required")
    private Boolean manager;
}
