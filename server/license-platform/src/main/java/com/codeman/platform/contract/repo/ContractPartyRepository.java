package com.codeman.platform.contract.repo;

import com.codeman.platform.contract.domain.ContractParty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractPartyRepository extends JpaRepository<ContractParty, Long> {
    List<ContractParty> findByContractId(Long contractId);
}
