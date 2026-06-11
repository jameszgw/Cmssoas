package com.codeman.platform.cmrule;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CmRuleEngine(规则引擎)商业版本常量 —— 与 CmRuleEngine 仓库 rule-engine-server/src/capabilities.js 保持逐键一致:
 * 能力键 = CAPABILITY_KEYS,四档预设 = PRESETS(COMMUNITY/PROFESSIONAL/ENTERPRISE/ULTIMATE)。
 * License claims 的 edition + features(能力覆盖表)原样喂给服务端 resolve(edition, overrides);
 * 签发时把「档位预设 ∪ 合同微调」固化进 features,显式键优先于客户端预设 —— 即便两侧版本漂移,
 * 已签发的 License 行为也不变。
 */
public final class CmruleEditions {
    private CmruleEditions() {}

    public static final String PRODUCT_CODE = "CMRULE";

    /** 当前 CmRuleEngine 1.x 产品线。 */
    public static final String DEFAULT_VERSION_RANGE = ">=1.0.0 <2.0.0";

    /** 全部可控能力键(顺序与 CmRuleEngine CAPABILITY_KEYS 一致,便于 UI 对照)。 */
    public static final List<String> CAPABILITY_KEYS = List.of(
            // 基础设计 / 低代码(四档全含)
            "ruleChainCore", "zeroCodeWizard", "lintCheck", "nodeSearch",
            // 专业版起:模板库 / 决策建模 / 规则流 / 版本管理 / 编排基础 / 审计查询
            "templateLibrary", "decisionTable", "ruleFlow", "edgeChains", "versionHistory",
            "orchestrationBasic", "auditQuery",
            // 企业版起:影响分析 / 审计导出 / 高级编排 / 平台增强
            "versionDiff", "auditExport", "orchestrationAdvanced", "dbDialects", "haCluster",
            // 旗舰版独占:AI / 分布式事务 / 信创
            "aiRuleGen", "distributedTx", "xinChuang");

    /** 四档预设:只列「相对旗舰版全开」被关闭的键(= CmRuleEngine PRESETS 同名预设)。 */
    public static final Map<String, Map<String, Boolean>> PRESETS;

    /** 各档位 License 的 modules 字段(产品目录 CMRULE 的模块编码;信息性,门禁以 features 为准)。 */
    public static final Map<String, List<String>> EDITION_MODULES;

    static {
        Map<String, Boolean> community = new LinkedHashMap<>();
        for (String k : List.of("templateLibrary", "decisionTable", "ruleFlow", "edgeChains",
                "versionHistory", "orchestrationBasic", "auditQuery", "versionDiff", "auditExport",
                "orchestrationAdvanced", "dbDialects", "haCluster", "aiRuleGen", "distributedTx", "xinChuang")) {
            community.put(k, false);
        }
        Map<String, Boolean> professional = new LinkedHashMap<>();
        for (String k : List.of("versionDiff", "auditExport", "orchestrationAdvanced",
                "dbDialects", "haCluster", "aiRuleGen", "distributedTx", "xinChuang")) {
            professional.put(k, false);
        }
        Map<String, Boolean> enterprise = new LinkedHashMap<>();
        for (String k : List.of("aiRuleGen", "distributedTx", "xinChuang")) {
            enterprise.put(k, false);
        }

        // 注意:Map.copyOf 不保序,档位/能力键展示顺序有意义,统一用 unmodifiable LinkedHashMap
        Map<String, Map<String, Boolean>> presets = new LinkedHashMap<>();
        presets.put("COMMUNITY", java.util.Collections.unmodifiableMap(community));
        presets.put("PROFESSIONAL", java.util.Collections.unmodifiableMap(professional));
        presets.put("ENTERPRISE", java.util.Collections.unmodifiableMap(enterprise));
        presets.put("ULTIMATE", Map.of());
        PRESETS = java.util.Collections.unmodifiableMap(presets);

        Map<String, List<String>> modules = new LinkedHashMap<>();
        modules.put("COMMUNITY", List.of("CORE", "LOWCODE"));
        modules.put("PROFESSIONAL", List.of("CORE", "LOWCODE", "DECISION", "FLOW", "VERSION", "ORCH", "AUDITLOG"));
        modules.put("ENTERPRISE", List.of("CORE", "LOWCODE", "DECISION", "FLOW", "VERSION", "ORCH", "AUDITLOG", "PLATFORM"));
        modules.put("ULTIMATE", List.of("CORE", "LOWCODE", "DECISION", "FLOW", "VERSION", "ORCH", "AUDITLOG", "PLATFORM", "AI"));
        EDITION_MODULES = java.util.Collections.unmodifiableMap(modules);
    }

    /** 与 CmRuleEngine capabilities.js resolve 同语义:全键开 → 套档位预设 → 按 overrides 微调,产出全键布尔表。 */
    public static Map<String, Boolean> resolve(String edition, Map<String, Boolean> overrides) {
        Map<String, Boolean> out = new LinkedHashMap<>();
        for (String k : CAPABILITY_KEYS) out.put(k, true);
        Map<String, Boolean> preset = PRESETS.getOrDefault(edition, Map.of());
        preset.forEach((k, v) -> { if (out.containsKey(k)) out.put(k, v); });
        if (overrides != null) {
            overrides.forEach((k, v) -> { if (out.containsKey(k) && v != null) out.put(k, v); });
        }
        return out;
    }
}
