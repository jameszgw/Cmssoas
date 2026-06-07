package com.codeman.platform.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 校验 db/mysql 的 MySQL 方言脚本可被干净应用。
 * 用 H2 的 MySQL 兼容模式(MODE=MySQL)运行 Flyway——验证 AUTO_INCREMENT / DATETIME / BOOLEAN 等
 * 转换后的 DDL 语法与全 17 个迁移可顺序应用(强语法/可应用性烟测;真机 MySQL 仍建议跑一次)。
 */
class MysqlMigrationTest {

    @Test
    void mysqlDialectMigrationsApplyCleanly() {
        String url = "jdbc:h2:mem:mysqlmig;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
        Flyway fw = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/mysql")
                .load();
        MigrateResult r = fw.migrate();
        assertTrue(r.success, "MySQL 方言迁移应成功应用");
        assertEquals(18, r.migrationsExecuted, "应应用全部 18 个迁移");
        assertEquals("18", fw.info().current().getVersion().getVersion(), "当前版本应为 17");
    }
}
