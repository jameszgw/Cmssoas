package com.codeman.platform.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 真实 MySQL 上应用 db/mysql 方言迁移(仅 CI 提供 MySQL 服务时运行)。
 * 通过环境变量 MYSQL_TEST_URL / MYSQL_TEST_USER / MYSQL_TEST_PASSWORD 注入;本地无该变量则自动跳过,
 * 不影响常规 `mvn test`。
 */
@EnabledIfEnvironmentVariable(named = "MYSQL_TEST_URL", matches = ".+")
class MysqlRealMigrationTest {

    @Test
    void migrationsApplyOnRealMysql() {
        String url = System.getenv("MYSQL_TEST_URL");
        String user = System.getenv().getOrDefault("MYSQL_TEST_USER", "root");
        String pwd = System.getenv().getOrDefault("MYSQL_TEST_PASSWORD", "");

        Flyway fw = Flyway.configure()
                .dataSource(url, user, pwd)
                .locations("classpath:db/mysql")
                .cleanDisabled(false)
                .load();
        fw.clean();                         // 保证幂等(CI 全新库)
        MigrateResult r = fw.migrate();
        assertTrue(r.success, "MySQL 方言迁移应在真实 MySQL 成功应用");
        assertEquals(18, r.migrationsExecuted, "应应用全部 18 个迁移");
        assertEquals("18", fw.info().current().getVersion().getVersion(), "当前版本应为 17");
    }
}
