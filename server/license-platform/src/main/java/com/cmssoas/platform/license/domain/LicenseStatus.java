package com.cmssoas.platform.license.domain;

public enum LicenseStatus {
    ACTIVE,    // 生效中
    REVOKED,   // 已吊销（进入 CRL）
    EXPIRED,   // 已过期
    SUSPENDED  // 暂停
}
