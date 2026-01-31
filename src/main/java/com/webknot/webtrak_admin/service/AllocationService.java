package com.webknot.webtrak_admin.service;

import com.webknot.webtrak_admin.client.AuthClient;
import com.webknot.webtrak_admin.dto.AllocationRequest;
import com.webknot.webtrak_admin.dto.AllocationResponse;
import com.webknot.webtrak_admin.dto.AuthUserResponse;
import com.webknot.webtrak_admin.entity.Allocation;
import com.webknot.webtrak_admin.entity.Project;
import com.webknot.webtrak_admin.enums.AllocationType;
import com.webknot.webtrak_admin.enums.ProjectType;
import com.webknot.webtrak_admin.exception.BadRequestException;
import com.webknot.webtrak_admin.exception.ConflictException;
import com.webknot.webtrak_admin.exception.ResourceNotFoundException;
import com.webknot.webtrak_admin.repository.AllocationRepository;
import com.webknot.webtrak_admin.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AllocationService {

    private final AllocationRepository allocationRepository;
    private final ProjectRepository projectRepository;
    private final AuthClient authClient;

    public AllocationResponse createAllocation(AllocationRequest request) {
        Project project = getProjectByCode(request.getProjectCode());
        validateUserExists(request.getUserId());
        validateAllocationRules(request, project);

        if (allocationRepository.existsByUserIdAndProjectAndActiveTrue(request.getUserId(), project)) {
            throw new ConflictException("Active allocation already exists for this user and project");
        }

        Allocation allocation = new Allocation();
        allocation.setUserId(request.getUserId());
        allocation.setProject(project);
        allocation.setAllocationType(request.getAllocationType());
        allocation.setStartDate(request.getStartDate());
        allocation.setEndDate(request.getEndDate());
        allocation.setHoursPerDay(request.getHoursPerDay());
        allocation.setAllocationRole(request.getAllocationRole());
        allocation.setManager(Boolean.TRUE.equals(request.getManager()));
        allocation.setActive(true);

        Allocation saved = allocationRepository.save(allocation);
        return mapToResponse(saved);
    }

    public AllocationResponse updateAllocation(Long id, AllocationRequest request) {
        Long allocationId = Objects.requireNonNull(id, "id must not be null");
        Allocation existing = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found: " + id));

        Project project = getProjectByCode(request.getProjectCode());
        validateUserExists(request.getUserId());
        validateAllocationRules(request, project);

        if (existing.isActive()) {
            existing.setActive(false);
            allocationRepository.save(existing);
        }

        Allocation allocation = new Allocation();
        allocation.setUserId(request.getUserId());
        allocation.setProject(project);
        allocation.setAllocationType(request.getAllocationType());
        allocation.setStartDate(LocalDate.now());
        allocation.setEndDate(request.getEndDate());
        allocation.setHoursPerDay(request.getHoursPerDay());
        allocation.setAllocationRole(request.getAllocationRole());
        allocation.setManager(Boolean.TRUE.equals(request.getManager()));
        allocation.setActive(true);

        Allocation saved = allocationRepository.save(allocation);
        return mapToResponse(saved);
    }

    public void deactivateAllocation(Long id) {
        Long allocationId = Objects.requireNonNull(id, "id must not be null");
        Allocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found: " + id));
        allocation.setActive(false);
        allocationRepository.save(allocation);
    }

    public Page<AllocationResponse> listAllocations(Long userId,
                                                   String userEmail,
                                                   String projectCode,
                                                   AllocationType allocationType,
                                                   String role,
                                                   Boolean active,
                                                   int page,
                                                   int size) {
        Long resolvedUserId = userId != null
            ? userId
            : (StringUtils.hasText(userEmail) ? resolveUserIdByEmail(userEmail) : null);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Specification<Allocation> spec = Specification.allOf();

        if (resolvedUserId != null) {
            Long finalResolvedUserId = resolvedUserId;
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), finalResolvedUserId));
        }
        if (StringUtils.hasText(projectCode)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.join("project").get("code"), projectCode));
        }
        if (allocationType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("allocationType"), allocationType));
        }
        if (StringUtils.hasText(role)) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("allocationRole")), "%" + role.toLowerCase() + "%"));
        }
        if (active != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), active));
        }

        return allocationRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    private Project getProjectByCode(String code) {
        return projectRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found for code: " + code));
    }

    private void validateAllocationRules(AllocationRequest request, Project project) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }
        if (request.getHoursPerDay() == null || request.getHoursPerDay() <= 0 || request.getHoursPerDay() > 8) {
            throw new BadRequestException("Hours per day must be between 0 and 8");
        }
        if (project.getType() == ProjectType.STAFFING
                && request.getAllocationType() != AllocationType.STAFFING
                && request.getAllocationType() != AllocationType.LOCKED) {
            throw new BadRequestException("STAFFING projects can only have STAFFING or LOCKED allocations");
        }
        if (request.getAllocationType() == AllocationType.STAFFING && project.getType() != ProjectType.STAFFING) {
            throw new BadRequestException("STAFFING allocation type is only allowed for STAFFING projects");
        }
    }

    private void validateUserExists(Long userId) {
        try {
            authClient.getUserById(userId);
        } catch (Exception ex) {
            throw new BadRequestException("User not found for id: " + userId);
        }
    }

    private Long resolveUserIdByEmail(String email) {
        try {
            AuthUserResponse user = authClient.getUserByEmail(email);
            return user.getId();
        } catch (Exception ex) {
            throw new BadRequestException("User not found for email: " + email);
        }
    }

    private AllocationResponse mapToResponse(Allocation allocation) {
        return AllocationResponse.builder()
                .id(allocation.getId())
                .userId(allocation.getUserId())
                .projectCode(allocation.getProject().getCode())
                .projectName(allocation.getProject().getName())
                .allocationType(allocation.getAllocationType())
                .startDate(allocation.getStartDate())
                .endDate(allocation.getEndDate())
                .hoursPerDay(allocation.getHoursPerDay())
                .allocationRole(allocation.getAllocationRole())
                .manager(allocation.isManager())
                .active(allocation.isActive())
                .createdAt(allocation.getCreatedAt())
                .updatedAt(allocation.getUpdatedAt())
                .build();
    }
}
