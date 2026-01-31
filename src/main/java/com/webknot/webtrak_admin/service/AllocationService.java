package com.webknot.webtrak_admin.service;

import com.webknot.webtrak_admin.client.AuthClient;
import com.webknot.webtrak_admin.dto.AllocationRequest;
import com.webknot.webtrak_admin.dto.AllocationResponse;
import com.webknot.webtrak_admin.dto.ProjectAllocationSummaryResponse;
import com.webknot.webtrak_admin.dto.UserAllocationSummaryResponse;
import com.webknot.webtrak_admin.dto.AuthUserResponse;
import com.webknot.webtrak_admin.entity.Allocation;
import com.webknot.webtrak_admin.entity.Project;
import com.webknot.webtrak_admin.enums.AllocationType;
import com.webknot.webtrak_admin.enums.ProjectType;
import com.webknot.webtrak_admin.exception.BadRequestException;
import com.webknot.webtrak_admin.exception.ConflictException;
import com.webknot.webtrak_admin.exception.ResourceNotFoundException;
import com.webknot.webtrak_admin.kafka.AllocationEventProducer;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AllocationService {

    private static final String BENCH_PROJECT_CODE = "BENCH";

    private final AllocationRepository allocationRepository;
    private final ProjectRepository projectRepository;
    private final AuthClient authClient;
    private final AllocationEventProducer allocationEventProducer;

    public AllocationResponse createAllocation(AllocationRequest request) {
        Project project = getProjectByCode(request.getProjectCode());
        validateUserExists(request.getUserId());
        validateAllocationRules(request, project, null);

        if (allocationRepository.existsByUserIdAndProjectAndActiveTrue(request.getUserId(), project)) {
            throw new ConflictException("Active allocation already exists for this user and project");
        }

        ensureCapacity(request.getUserId(), request.getHoursPerDay(), null);

        Allocation allocation = new Allocation();
        allocation.setUserId(request.getUserId());
        allocation.setProject(project);
        allocation.setAllocationType(request.getAllocationType());
        allocation.setStartDate(request.getStartDate());
        allocation.setEndDate(request.getEndDate());
        allocation.setLockedInDate(request.getLockedInDate());
        allocation.setHoursPerDay(request.getHoursPerDay());
        allocation.setAllocationRole(request.getAllocationRole());
        allocation.setManager(Boolean.TRUE.equals(request.getManager()));
        allocation.setActive(true);

        Allocation saved = allocationRepository.save(allocation);
        recomputeBenchAllocationForUser(request.getUserId());
        publishIfNotBench(saved, "ALLOCATION_CREATED");
        return mapToResponse(saved);
    }

    public AllocationResponse updateAllocation(Long id, AllocationRequest request) {
        Long allocationId = Objects.requireNonNull(id, "id must not be null");
        Allocation existing = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found: " + id));

        if (isLockedAndImmutable(existing)) {
            throw new BadRequestException("Cannot update locked allocation before locked-in date");
        }

        Project project = getProjectByCode(request.getProjectCode());
        validateUserExists(request.getUserId());
        validateAllocationRules(request, project, existing);

        ensureCapacity(request.getUserId(), request.getHoursPerDay(), existing.getId());

        if (existing.isActive()) {
            existing.setActive(false);
            allocationRepository.save(existing);
            publishIfNotBench(existing, "ALLOCATION_DEACTIVATED");
        }

        Allocation allocation = new Allocation();
        allocation.setUserId(request.getUserId());
        allocation.setProject(project);
        allocation.setAllocationType(request.getAllocationType());
        allocation.setStartDate(LocalDate.now());
        allocation.setEndDate(request.getEndDate());
        allocation.setLockedInDate(request.getLockedInDate());
        allocation.setHoursPerDay(request.getHoursPerDay());
        allocation.setAllocationRole(request.getAllocationRole());
        allocation.setManager(Boolean.TRUE.equals(request.getManager()));
        allocation.setActive(true);

        Allocation saved = allocationRepository.save(allocation);
        recomputeBenchAllocationForUser(request.getUserId());
        publishIfNotBench(saved, "ALLOCATION_UPDATED");
        return mapToResponse(saved);
    }

    public void deactivateAllocation(Long id) {
        Long allocationId = Objects.requireNonNull(id, "id must not be null");
        Allocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found: " + id));

        if (isLockedAndImmutable(allocation)) {
            throw new BadRequestException("Cannot delete locked allocation before locked-in date");
        }

        allocation.setActive(false);
        allocationRepository.save(allocation);
        recomputeBenchAllocationForUser(allocation.getUserId());
        publishIfNotBench(allocation, "ALLOCATION_DEACTIVATED");
    }

    public Page<AllocationResponse> listAllocations(Long userId,
                                                   String userEmail,
                                                   String projectCode,
                                                   AllocationType allocationType,
                                                   String role,
                                                   String search,
                                                   Boolean active,
                                                   int page,
                                                   int size) {
        Long resolvedUserId = userId != null
                ? userId
                : (StringUtils.hasText(userEmail) ? resolveUserIdByEmail(userEmail) : null);

        if (resolvedUserId == null && StringUtils.hasText(search) && search.contains("@")) {
            resolvedUserId = resolveUserIdByEmail(search);
        }

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
        if (StringUtils.hasText(search) && !search.contains("@")) {
            String normalized = search.toLowerCase(Locale.ROOT);
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.join("project").get("code")), "%" + normalized + "%"),
                    cb.like(cb.lower(root.get("allocationRole")), "%" + normalized + "%")
            ));
        }
        if (active != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), active));
        }

        return allocationRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    public Page<AllocationResponse> listExpiringAllocations(int days, int page, int size) {
        int safeDays = Math.max(days, 1);
        LocalDate end = LocalDate.now().plusDays(safeDays);
        Pageable pageable = PageRequest.of(page, size, Sort.by("endDate").ascending());
        Specification<Allocation> spec = (root, query, cb) -> cb.conjunction();
        spec = spec.and((root, query, cb) -> cb.isTrue(root.get("active")))
            .and((root, query, cb) -> cb.between(root.get("endDate"), LocalDate.now(), end));
        return allocationRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    public List<AllocationResponse> listAllocationsByProject(String projectCode) {
        Project project = getProjectByCode(projectCode);
        return allocationRepository.findByProjectAndActiveTrue(project)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

        public ProjectAllocationSummaryResponse getProjectSummary(String projectCode) {
        Project project = getProjectByCode(projectCode);
        List<Allocation> allocations = allocationRepository.findByProjectAndActiveTrue(project).stream()
            .filter(a -> !a.getEndDate().isBefore(LocalDate.now()))
            .toList();
        long employeeCount = allocations.stream()
            .map(Allocation::getUserId)
            .distinct()
            .count();
        double capacityPerWeek = allocations.stream()
            .mapToDouble(Allocation::getHoursPerDay)
            .sum() * 5;

        return ProjectAllocationSummaryResponse.builder()
            .projectCode(project.getCode())
            .projectName(project.getName())
            .employeeCount(employeeCount)
            .capacityPerWeek(capacityPerWeek)
            .build();
        }

        public UserAllocationSummaryResponse getUserSummary(Long userId) {
        List<Allocation> allocations = allocationRepository.findByUserIdAndActiveTrue(userId).stream()
            .filter(a -> !BENCH_PROJECT_CODE.equalsIgnoreCase(a.getProject().getCode()))
            .filter(a -> !a.getEndDate().isBefore(LocalDate.now()))
            .toList();
        long projectCount = allocations.stream()
            .map(a -> a.getProject().getId())
            .distinct()
            .count();
        double capacityPerWeek = allocations.stream()
            .mapToDouble(Allocation::getHoursPerDay)
            .sum() * 5;

        return UserAllocationSummaryResponse.builder()
            .userId(userId)
            .projectCount(projectCount)
            .capacityPerWeek(capacityPerWeek)
            .build();
        }

    public void deactivateExpiredAllocations() {
        List<Allocation> expired = allocationRepository.findByActiveTrueAndEndDateBefore(LocalDate.now());
        if (expired.isEmpty()) {
            return;
        }
        Set<Long> affectedUsers = new HashSet<>();
        for (Allocation allocation : expired) {
            allocation.setActive(false);
            affectedUsers.add(allocation.getUserId());
        }
        allocationRepository.saveAll(expired);
        for (Allocation allocation : expired) {
            publishIfNotBench(allocation, "ALLOCATION_DEACTIVATED");
        }
        for (Long userId : affectedUsers) {
            recomputeBenchAllocationForUser(userId);
        }
    }

    private Project getProjectByCode(String code) {
        return projectRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found for code: " + code));
    }

    private void validateAllocationRules(AllocationRequest request, Project project, Allocation existing) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }
        if (request.getAllocationType() == AllocationType.LOCKED && request.getLockedInDate() == null) {
            throw new BadRequestException("Locked-in date is required for LOCKED allocations");
        }
        if (request.getLockedInDate() != null && request.getLockedInDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("Locked-in date cannot be before start date");
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
        if (existing != null && isLockedAndImmutable(existing)) {
            throw new BadRequestException("Cannot modify locked allocation before locked-in date");
        }
    }

    private boolean isLockedAndImmutable(Allocation allocation) {
        return allocation.getAllocationType() == AllocationType.LOCKED
                && allocation.getLockedInDate() != null
                && allocation.getLockedInDate().isAfter(LocalDate.now());
    }

    private void ensureCapacity(Long userId, Double hoursPerDay, Long excludeAllocationId) {
        double existingHours = allocationRepository.findByUserIdAndActiveTrue(userId).stream()
                .filter(allocation -> !BENCH_PROJECT_CODE.equalsIgnoreCase(allocation.getProject().getCode()))
                .filter(allocation -> excludeAllocationId == null || !allocation.getId().equals(excludeAllocationId))
                .filter(allocation -> !allocation.getEndDate().isBefore(LocalDate.now()))
                .mapToDouble(Allocation::getHoursPerDay)
                .sum();
        if (existingHours + hoursPerDay > 8) {
            throw new BadRequestException("Total allocated hours per day cannot exceed 8");
        }
    }

    private void recomputeBenchAllocationForUser(Long userId) {
        Project benchProject = getOrCreateBenchProject();
        List<Allocation> active = allocationRepository.findByUserIdAndActiveTrue(userId);

        double nonBenchHours = active.stream()
                .filter(a -> !BENCH_PROJECT_CODE.equalsIgnoreCase(a.getProject().getCode()))
                .filter(a -> !a.getEndDate().isBefore(LocalDate.now()))
                .mapToDouble(Allocation::getHoursPerDay)
                .sum();

        double benchHours = Math.max(0, 8 - nonBenchHours);
        Allocation benchAllocation = active.stream()
                .filter(a -> BENCH_PROJECT_CODE.equalsIgnoreCase(a.getProject().getCode()))
                .findFirst()
                .orElse(null);

        if (benchHours == 0) {
            if (benchAllocation != null) {
                benchAllocation.setActive(false);
                allocationRepository.save(benchAllocation);
            }
            return;
        }

        if (benchAllocation == null) {
            benchAllocation = new Allocation();
            benchAllocation.setUserId(userId);
            benchAllocation.setProject(benchProject);
            benchAllocation.setAllocationType(AllocationType.PART_TIME);
            benchAllocation.setAllocationRole("BENCH");
            benchAllocation.setManager(false);
            benchAllocation.setActive(true);
            benchAllocation.setStartDate(LocalDate.now());
        }

        LocalDate maxEndDate = active.stream()
                .filter(a -> !BENCH_PROJECT_CODE.equalsIgnoreCase(a.getProject().getCode()))
                .map(Allocation::getEndDate)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now().plusYears(1));

        benchAllocation.setEndDate(maxEndDate);
        benchAllocation.setHoursPerDay(benchHours);
        allocationRepository.save(benchAllocation);
    }

    private Project getOrCreateBenchProject() {
        return projectRepository.findByCode(BENCH_PROJECT_CODE)
                .orElseGet(() -> {
                    Project project = new Project();
                    project.setCode(BENCH_PROJECT_CODE);
                    project.setName("Bench");
                    project.setType(ProjectType.IN_HOUSE);
                    project.setDescription("Auto-generated bench project");
                    return projectRepository.save(project);
                });
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
            .lockedInDate(allocation.getLockedInDate())
                .hoursPerDay(allocation.getHoursPerDay())
                .allocationRole(allocation.getAllocationRole())
                .manager(allocation.isManager())
                .active(allocation.isActive())
                .createdAt(allocation.getCreatedAt())
                .updatedAt(allocation.getUpdatedAt())
                .build();
    }

    private void publishIfNotBench(Allocation allocation, String eventType) {
        if (BENCH_PROJECT_CODE.equalsIgnoreCase(allocation.getProject().getCode())) {
            return;
        }
        allocationEventProducer.publishAllocationEvent(allocation, eventType);
    }
}
