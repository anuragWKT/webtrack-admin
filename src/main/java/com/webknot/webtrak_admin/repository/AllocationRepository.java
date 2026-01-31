package com.webknot.webtrak_admin.repository;

import com.webknot.webtrak_admin.entity.Allocation;
import com.webknot.webtrak_admin.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AllocationRepository extends JpaRepository<Allocation, Long> {

    List<Allocation> findByUserId(Long userId);

    List<Allocation> findByProject(Project project);

    Optional<Allocation> findByUserIdAndProjectAndActiveTrue(Long userId, Project project);

    boolean existsByUserIdAndProjectAndActiveTrue(Long userId, Project project);
}
