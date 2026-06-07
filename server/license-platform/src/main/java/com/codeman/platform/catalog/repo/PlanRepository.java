package com.codeman.platform.catalog.repo;

import com.codeman.platform.catalog.domain.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findAllByOrderBySortAsc();
    Optional<Plan> findByCode(String code);
}
