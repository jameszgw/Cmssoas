package com.codeman.platform.license.repo;

import com.codeman.platform.license.domain.LicenseHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LicenseHistoryRepository extends JpaRepository<LicenseHistory, Long> {
    List<LicenseHistory> findByLicenseIdOrderByVersionDesc(String licenseId);
}
