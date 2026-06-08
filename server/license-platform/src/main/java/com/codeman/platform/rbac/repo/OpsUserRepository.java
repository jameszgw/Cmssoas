package com.codeman.platform.rbac.repo;

import com.codeman.platform.rbac.domain.OpsUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OpsUserRepository extends JpaRepository<OpsUser, Long> {
    Optional<OpsUser> findByUsername(String username);
}
