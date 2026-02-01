package com.webknot.webtrak_admin.dto;

import com.webknot.webtrak_admin.enums.ProjectType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectResponse {
    private Long id;
    private String code;
    private String name;
    private ProjectType type;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
