package com.cmssoas.platform.contract.repo;

import com.cmssoas.platform.contract.domain.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    List<Contract> findAllByOrderByCreatedAtDesc();
}
