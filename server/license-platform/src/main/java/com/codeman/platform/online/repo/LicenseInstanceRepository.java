package com.codeman.platform.online.repo;

import com.codeman.platform.online.domain.LicenseInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LicenseInstanceRepository extends JpaRepository<LicenseInstance, Long> {

    Optional<LicenseInstance> findByLicenseIdAndInstanceId(String licenseId, String instanceId);

    List<LicenseInstance> findAllByOrderByLastHeartbeatDesc();

    List<LicenseInstance> findByStatus(String status);

    /** 在线席位计数：状态 ACTIVE 且心跳在宽限期内。 */
    long countByLicenseIdAndStatusAndLastHeartbeatAfter(String licenseId, String status, LocalDateTime after);
}
