package com.codeman.platform.contract.repo;

import com.codeman.platform.contract.domain.ContractTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, Long> {
    List<ContractTemplate> findAllByOrderByCreatedAtDesc();
}
