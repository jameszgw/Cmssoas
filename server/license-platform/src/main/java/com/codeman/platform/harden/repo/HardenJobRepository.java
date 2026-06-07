package com.codeman.platform.harden.repo;

import com.codeman.platform.harden.domain.HardenJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HardenJobRepository extends JpaRepository<HardenJob, Long> {
    List<HardenJob> findAllByOrderByCreatedAtDesc();
}
