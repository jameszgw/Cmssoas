package com.codeman.platform.license.repo;

import com.codeman.platform.license.domain.License;
import com.codeman.platform.license.domain.LicenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LicenseRepository extends JpaRepository<License, Long> {
    Optional<License> findByLicenseId(String licenseId);
    List<License> findAllByOrderByCreatedAtDesc();
    List<License> findByStatus(LicenseStatus status);
}
