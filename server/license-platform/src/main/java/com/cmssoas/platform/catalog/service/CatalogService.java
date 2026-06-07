package com.cmssoas.platform.catalog.service;

import com.cmssoas.platform.catalog.dto.CatalogDtos.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/** 产品目录读取（模块/功能/版本矩阵），数据来自 Flyway 种子。 */
@Service
public class CatalogService {

    private final JdbcTemplate jdbc;

    public CatalogService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ProductView> products() {
        List<ProductView> result = new ArrayList<>();
        jdbc.query("SELECT code,name FROM product ORDER BY id", (rs, i) -> new String[]{rs.getString(1), rs.getString(2)})
                .forEach(p -> {
                    List<ModuleView> modules = new ArrayList<>();
                    jdbc.query("SELECT code,name FROM module WHERE product_code=? ORDER BY sort",
                            (rs, i) -> new String[]{rs.getString(1), rs.getString(2)}, p[0])
                            .forEach(m -> {
                                List<FeatureView> feats = jdbc.query(
                                        "SELECT code,name FROM feature WHERE module_code=? ORDER BY sort",
                                        (rs, i) -> new FeatureView(rs.getString(1), rs.getString(2)), m[0]);
                                modules.add(new ModuleView(m[0], m[1], feats));
                            });
                    result.add(new ProductView(p[0], p[1], modules));
                });
        return result;
    }

    public MatrixView matrix() {
        List<String> versions = jdbc.queryForList(
                "SELECT version FROM app_version ORDER BY sort", String.class);

        Map<String, Boolean> avail = new HashMap<>();
        jdbc.query("SELECT feature_code,version,available FROM feature_version", rs -> {
            avail.put(rs.getString(1) + "|" + rs.getString(2), rs.getBoolean(3));
        });

        List<MatrixRow> rows = jdbc.query(
                "SELECT m.name, f.name, f.code FROM feature f JOIN module m ON m.code=f.module_code ORDER BY f.sort",
                (rs, i) -> {
                    String code = rs.getString(3);
                    List<Boolean> a = versions.stream().map(v -> avail.getOrDefault(code + "|" + v, false)).toList();
                    return new MatrixRow(rs.getString(1), rs.getString(2), code, a);
                });
        return new MatrixView(versions, rows);
    }
}
