package com.webknot.webtrak_admin.controller;

import com.webknot.webtrak_admin.dto.AllocationRequest;
import com.webknot.webtrak_admin.dto.AllocationResponse;
import com.webknot.webtrak_admin.dto.ProjectAllocationSummaryResponse;
import com.webknot.webtrak_admin.dto.UserAllocationSummaryResponse;
import com.webknot.webtrak_admin.enums.AllocationType;
import com.webknot.webtrak_admin.service.AllocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/allocations")
@RequiredArgsConstructor
public class AllocationController {

    private final AllocationService allocationService;

    @PostMapping
    public ResponseEntity<AllocationResponse> createAllocation(@Valid @RequestBody AllocationRequest request) {
        return new ResponseEntity<>(allocationService.createAllocation(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AllocationResponse> updateAllocation(@PathVariable Long id,
                                                               @Valid @RequestBody AllocationRequest request) {
        return ResponseEntity.ok(allocationService.updateAllocation(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateAllocation(@PathVariable Long id) {
        allocationService.deactivateAllocation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<AllocationResponse>> listAllocations(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String projectCode,
            @RequestParam(required = false) AllocationType allocationType,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(allocationService.listAllocations(userId, userEmail, projectCode, allocationType, role, search, active, page, size));
    }

    @GetMapping("/expiring")
    public ResponseEntity<Page<AllocationResponse>> listExpiringAllocations(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(allocationService.listExpiringAllocations(days, page, size));
    }

    @GetMapping("/summary/project")
    public ResponseEntity<ProjectAllocationSummaryResponse> getProjectSummary(@RequestParam String projectCode) {
        return ResponseEntity.ok(allocationService.getProjectSummary(projectCode));
    }

    @GetMapping("/summary/user")
    public ResponseEntity<UserAllocationSummaryResponse> getUserSummary(@RequestParam Long userId) {
        return ResponseEntity.ok(allocationService.getUserSummary(userId));
    }
}
