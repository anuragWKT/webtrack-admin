package com.webknot.webtrak_admin.dto;

import com.webknot.webtrak_admin.enums.ProjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProjectRequest {

    @NotBlank(message = "Project code is required")
    @Size(max = 50, message = "Project code must be at most 50 characters")
    private String code;

    @NotBlank(message = "Project name is required")
    @Size(max = 150, message = "Project name must be at most 150 characters")
    private String name;

    @NotNull(message = "Project type is required")
    private ProjectType type;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;
}
