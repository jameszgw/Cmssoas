package com.cmssoas.platform.tenant.repo;

import com.cmssoas.platform.tenant.domain.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysUserRepository extends JpaRepository<SysUser, Long> {
}
