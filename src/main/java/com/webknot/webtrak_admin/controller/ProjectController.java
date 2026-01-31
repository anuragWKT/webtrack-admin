package com.webknot.webtrak_admin.controller;

import com.webknot.webtrak_admin.dto.AllocationResponse;
import com.webknot.webtrak_admin.dto.ProjectRequest;
import com.webknot.webtrak_admin.dto.ProjectResponse;
import com.webknot.webtrak_admin.service.AllocationService;
import com.webknot.webtrak_admin.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final AllocationService allocationService;

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request) {
        return new ResponseEntity<>(projectService.createProject(request), HttpStatus.CREATED);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<ProjectResponse>> createProjectsBulk(@Valid @RequestBody List<ProjectRequest> requests) {
        return new ResponseEntity<>(projectService.createProjectsBulk(requests), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<ProjectResponse>> listProjects(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(projectService.listProjects(search, page, size));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ProjectResponse> getProjectByCode(@PathVariable String code) {
        return ResponseEntity.ok(projectService.getProjectByCode(code));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ProjectResponse>> listProjectsForUser(@PathVariable Long userId) {
        return ResponseEntity.ok(projectService.listProjectsForUser(userId));
    }

    @GetMapping("/managed/{userId}")
    public ResponseEntity<List<ProjectResponse>> listProjectsManagedByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(projectService.listProjectsManagedByUser(userId));
    }

    @GetMapping("/{code}/allocations")
    public ResponseEntity<List<AllocationResponse>> listProjectAllocations(@PathVariable String code) {
        return ResponseEntity.ok(allocationService.listAllocationsByProject(code));
    }
}
