package com.codeman.platform.tenant.repo;

import com.codeman.platform.tenant.domain.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysUserRepository extends JpaRepository<SysUser, Long> {
}
