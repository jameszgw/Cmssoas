package com.cmssoas.platform.contract.repo;

import com.cmssoas.platform.contract.domain.ContractTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, Long> {
    List<ContractTemplate> findAllByOrderByCreatedAtDesc();
}
