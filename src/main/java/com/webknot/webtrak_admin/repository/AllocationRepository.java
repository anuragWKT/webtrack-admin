package com.webknot.webtrak_admin.repository;

import com.webknot.webtrak_admin.entity.Allocation;
import com.webknot.webtrak_admin.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AllocationRepository extends JpaRepository<Allocation, Long>, JpaSpecificationExecutor<Allocation> {

    List<Allocation> findByUserId(Long userId);

    List<Allocation> findByUserIdAndActiveTrue(Long userId);

    List<Allocation> findByUserIdAndManagerTrueAndActiveTrue(Long userId);

    List<Allocation> findByProject(Project project);

    List<Allocation> findByProjectAndActiveTrue(Project project);

    Optional<Allocation> findByUserIdAndProjectAndActiveTrue(Long userId, Project project);

    boolean existsByUserIdAndProjectAndActiveTrue(Long userId, Project project);

    List<Allocation> findByActiveTrueAndEndDateBefore(LocalDate date);
}
