package com.webknot.webtrak_admin.service;

import com.webknot.webtrak_admin.dto.ProjectRequest;
import com.webknot.webtrak_admin.dto.ProjectResponse;
import com.webknot.webtrak_admin.entity.Project;
import com.webknot.webtrak_admin.exception.ConflictException;
import com.webknot.webtrak_admin.exception.ResourceNotFoundException;
import com.webknot.webtrak_admin.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectResponse createProject(ProjectRequest request) {
        validateUnique(request.getCode(), request.getName());
        Project project = new Project();
        project.setCode(request.getCode().trim());
        project.setName(request.getName().trim());
        project.setType(request.getType());
        project.setDescription(request.getDescription());
        Project saved = projectRepository.save(project);
        return mapToResponse(saved);
    }

    public List<ProjectResponse> createProjectsBulk(List<ProjectRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new ConflictException("Project list cannot be empty");
        }

        Map<String, ProjectRequest> codeMap = requests.stream()
                .collect(Collectors.toMap(r -> normalize(r.getCode()), Function.identity(), (a, b) -> a));
        if (codeMap.size() != requests.size()) {
            throw new ConflictException("Duplicate project codes in request");
        }

        Map<String, ProjectRequest> nameMap = requests.stream()
                .collect(Collectors.toMap(r -> normalize(r.getName()), Function.identity(), (a, b) -> a));
        if (nameMap.size() != requests.size()) {
            throw new ConflictException("Duplicate project names in request");
        }

        for (ProjectRequest request : requests) {
            validateUnique(request.getCode(), request.getName());
        }

        List<Project> projects = new ArrayList<>();
        for (ProjectRequest request : requests) {
            Project project = new Project();
            project.setCode(request.getCode().trim());
            project.setName(request.getName().trim());
            project.setType(request.getType());
            project.setDescription(request.getDescription());
            projects.add(project);
        }

        return projectRepository.saveAll(projects)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ProjectResponse getProjectByCode(String code) {
        Project project = projectRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found for code: " + code));
        return mapToResponse(project);
    }

    public Page<ProjectResponse> listProjects(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Project> projects;
        if (StringUtils.hasText(search)) {
            projects = projectRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(search, search, pageable);
        } else {
            projects = projectRepository.findAll(pageable);
        }
        return projects.map(this::mapToResponse);
    }

    private void validateUnique(String code, String name) {
        if (projectRepository.existsByCode(code)) {
            throw new ConflictException("Project code already exists");
        }
        if (projectRepository.existsByName(name)) {
            throw new ConflictException("Project name already exists");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private ProjectResponse mapToResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .code(project.getCode())
                .name(project.getName())
                .type(project.getType())
                .description(project.getDescription())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
