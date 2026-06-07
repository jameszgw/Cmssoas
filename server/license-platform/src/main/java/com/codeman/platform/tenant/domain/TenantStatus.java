package com.codeman.platform.tenant.domain;

/** 租户开通状态机。 */
public enum TenantStatus {
    INITIALIZING, // 初始化中（建库/种子/创建超管）
    ACTIVE,       // 已开通（邮件已发，等待管理员激活后正式使用）
    SUSPENDED,    // 暂停
    FAILED        // 开通失败（已补偿回滚）
}
