package com.codeman.platform.tenant;

import com.codeman.platform.tenant.service.TenantSchemaService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 多租户「按 Schema 隔离」：开通两个租户后，各自 Schema 物理隔离、互不可见。 */
class TenantSchemaServiceTest {

    private TenantSchemaService svc(EmbeddedDatabase db) {
        return new TenantSchemaService(new JdbcTemplate(db));
    }

    @Test
    void schemaPerTenantIsolation() {
        EmbeddedDatabase db = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
        try {
            TenantSchemaService s = svc(db);
            s.provision("T-100001", "ENTERPRISE");
            s.provision("T-100002", "PROFESSIONAL");

            assertEquals("T_100001", s.schemaOf("T-100001"));
            assertEquals(3, s.roleCount("T-100001"));
            assertEquals(3, s.roleCount("T-100002"));

            String v1 = planOf(s.settings("T-100001"));
            String v2 = planOf(s.settings("T-100002"));
            assertEquals("ENTERPRISE", v1, "甲租户 plan 隔离");
            assertEquals("PROFESSIONAL", v2, "乙租户 plan 隔离");

            // 隔离：甲 Schema 内不含乙的 tenantCode
            boolean leaked = s.settings("T-100001").stream()
                    .anyMatch(m -> "T-100002".equals(String.valueOf(m.get("V"))) || "T-100002".equals(String.valueOf(m.get("v"))));
            assertFalse(leaked, "甲 Schema 不应包含乙的数据");
        } finally {
            db.shutdown();
        }
    }

    private String planOf(List<Map<String, Object>> settings) {
        for (Map<String, Object> m : settings) {
            Object k = m.getOrDefault("K", m.get("k"));
            if ("plan".equals(k)) return String.valueOf(m.getOrDefault("V", m.get("v")));
        }
        return null;
    }
}
