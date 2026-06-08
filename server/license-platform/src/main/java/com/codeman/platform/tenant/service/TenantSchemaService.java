package com.codeman.platform.tenant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 多租户「按 Schema 隔离」落地：每个租户独立 Schema，物理隔离其业务数据。
 * 开通时真实创建 Schema + 建表 + 写入种子（替代原占位步骤）；查询按租户 Schema 路由。
 * H2 / PostgreSQL 均支持 CREATE SCHEMA 与 schema 限定名访问。
 */
@Service
public class TenantSchemaService {

    private static final Logger log = LoggerFactory.getLogger(TenantSchemaService.class);

    private final JdbcTemplate jdbc;

    public TenantSchemaService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 由租户编码推导 Schema 名（仅字母数字下划线，防注入）。如 T-100483 → T_100483。 */
    public String schemaOf(String tenantCode) {
        String s = tenantCode.toUpperCase().replaceAll("[^A-Z0-9]", "_");
        // Schema 名须以字母开头；以数字开头时补前缀，避免重复 T_
        return Character.isDigit(s.charAt(0)) ? "T_" + s : s;
    }

    /** 步骤2：创建独立 Schema（物理隔离）。 */
    public void createSchema(String tenantCode) {
        String schema = schemaOf(tenantCode);
        jdbc.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
        log.info("[tenant-schema] 已创建 Schema {}", schema);
    }

    /** 步骤3：在租户 Schema 内建租户级业务表（等价于对新库执行迁移）。 */
    public void migrate(String tenantCode) {
        String schema = schemaOf(tenantCode);
        jdbc.execute("CREATE TABLE IF NOT EXISTS " + schema + ".tenant_setting (" +
                "k VARCHAR(64) PRIMARY KEY, v VARCHAR(256) NOT NULL, updated_at TIMESTAMP NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS " + schema + ".tenant_role (" +
                "code VARCHAR(32) PRIMARY KEY, name VARCHAR(64) NOT NULL)");
    }

    /** 步骤4：写入默认种子（角色/默认配置），各租户互不可见。 */
    public void seed(String tenantCode, String planCode) {
        String schema = schemaOf(tenantCode);
        LocalDateTime now = LocalDateTime.now();
        for (Object[] r : new Object[][]{
                {"plan", planCode}, {"tenantCode", tenantCode}, {"locale", "zh-CN"}, {"theme", "tech"}}) {
            jdbc.update("MERGE INTO " + schema + ".tenant_setting (k,v,updated_at) KEY(k) VALUES (?,?,?)",
                    r[0], r[1], now);
        }
        for (String[] role : new String[][]{
                {"ADMIN", "管理员"}, {"OPERATOR", "操作员"}, {"VIEWER", "只读"}}) {
            jdbc.update("MERGE INTO " + schema + ".tenant_role (code,name) KEY(code) VALUES (?,?)",
                    role[0], role[1]);
        }
        log.info("[tenant-schema] {} 种子数据写入完成", schema);
    }

    /** 一次性完成开通的隔离编排（Schema + 表 + 种子）。 */
    public void provision(String tenantCode, String planCode) {
        createSchema(tenantCode);
        migrate(tenantCode);
        seed(tenantCode, planCode);
    }

    /** 读取某租户 Schema 内的配置（证明按租户路由 + 隔离）。 */
    public List<Map<String, Object>> settings(String tenantCode) {
        return jdbc.queryForList("SELECT k, v FROM " + schemaOf(tenantCode) + ".tenant_setting ORDER BY k");
    }

    /** 租户级角色数（用于校验种子）。 */
    public int roleCount(String tenantCode) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + schemaOf(tenantCode) + ".tenant_role", Integer.class);
        return n == null ? 0 : n;
    }
}
