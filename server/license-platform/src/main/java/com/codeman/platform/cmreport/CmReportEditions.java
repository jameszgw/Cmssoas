package com.codeman.platform.cmreport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CmReport 报表平台的「商业版本 → 能力清单」矩阵(平台侧副本)。
 * 与 CmReport 产品内 {@code com.codeman.report.license.EditionCapabilities} 保持一致;
 * 能力码即产品 Capability 码表。版本由低到高累进:community ⊂ lite ⊂ pro ⊂ enterprise ⊂ ultimate。
 * 注意:License 令牌只携带 edition + 附加能力(addons),版本基线能力由产品侧按 edition 推导;
 * 本矩阵用于运营台 UI 展示与校验(如附加包必须是产品已知能力码)。
 */
public final class CmReportEditions {

    /** 版本顺序(由低到高)。 */
    public static final List<String> EDITIONS = List.of("community", "lite", "pro", "enterprise", "ultimate");

    /** 各版本「新增」能力(相对上一档;展示用)。 */
    private static final Map<String, List<String>> INCREMENT = new LinkedHashMap<>();

    /** 可作为附加包单独叠加的能力码(在版本基线之上;与产品 License.addons 对应)。 */
    public static final List<String> ADDONS = List.of(
            "ai.pack", "xinchuang", "connector.etl", "cube", "selfbi", "datamodel");

    /** 限额键(与产品 LicenseManager.withinLimit 对应;<=0 表示不限)。 */
    public static final List<String> LIMIT_KEYS = List.of("concurrency", "nodes", "users", "instances");

    static {
        INCREMENT.put("community", List.of(
                "designer", "mobile.h5", "embed.url", "theme.i18n"));
        INCREMENT.put("lite", List.of(
                "crosstab.chart", "version.watermark"));
        INCREMENT.put("pro", List.of(
                "cond.style.template", "datasource.multi", "olap.preagg", "query.param.drill",
                "dashboard", "export.full", "schedule", "embed.sdk", "perm.datalevel", "dataset.query"));
        INCREMENT.put("enterprise", List.of(
                "dashboard.linkage", "screen.realtime", "writeback", "alert", "whitelabel.sso",
                "perm.rbac.rowcol.audit", "ha.cluster", "xinchuang", "gov.desens.catalog"));
        INCREMENT.put("ultimate", List.of(
                "selfbi", "datamodel", "cube", "gov.metric", "gov.lineage.quality", "ai.pack", "connector.etl"));
    }

    private CmReportEditions() {}

    /** 是否合法版本码。 */
    public static boolean isEdition(String s) {
        return s != null && EDITIONS.contains(s.trim().toLowerCase());
    }

    /** 某版本的累计能力集(含所有更低档;运营台展示/比对用)。 */
    public static List<String> capabilitiesOf(String edition) {
        String e = edition == null ? "community" : edition.trim().toLowerCase();
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (String tier : EDITIONS) {
            out.addAll(INCREMENT.get(tier));
            if (tier.equals(e)) {
                break;
            }
        }
        return out;
    }

    /** 全矩阵(edition → 累计能力集),供 /api/cmreport/editions 下发 UI。 */
    public static Map<String, List<String>> matrix() {
        Map<String, List<String>> m = new LinkedHashMap<>();
        for (String e : EDITIONS) {
            m.put(e, capabilitiesOf(e));
        }
        return m;
    }
}
