package com.codeman.platform.cmprint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CmPrint(打印模板设计器)商业版本常量 —— 与 CmPrint 仓库 src/core/capabilities.js 保持逐键一致:
 * 能力键 = CAPABILITY_KEYS,三档预设 = EDITIONS(community/professional/enterprise)。
 * License claims 的 edition + features(能力覆盖表)原样喂给前端 resolveEdition(edition, overrides);
 * 签发时把「档位预设 ∪ 合同微调」固化进 features,显式键优先于前端预设 —— 即便两侧版本漂移,
 * 已签发的 License 行为也不变。
 */
public final class CmprintEditions {
    private CmprintEditions() {}

    public static final String PRODUCT_CODE = "CMPRINT";

    /** 当前 CmPrint 0.5.x 产品线。 */
    public static final String DEFAULT_VERSION_RANGE = ">=0.5.0 <1.0.0";

    /** 全部可控能力键(顺序与 CmPrint CAPABILITY_KEYS 一致,便于 UI 对照)。 */
    public static final List<String> CAPABILITY_KEYS = List.of(
            // 顶部:语言/纸张
            "language", "paperSelect", "customPaper", "pageMargin", "theme",
            // 数据 / 编辑 / 导出
            "dataSource", "editJson", "export",
            "exportPdf", "exportPdfText", "sharePdf", "exportWord", "exportExcel", "exportImage", "exportJson",
            "subTemplate", "templateGallery", "save",
            // 视图 / 预览 / 打印
            "zoom", "rotate", "grid", "preview", "print", "directPrint",
            // 面板级弹层
            "pageNumber", "watermark", "overprint", "calibration",
            // 字体 / 对齐 / 历史 / 结构
            "fontControls", "align", "undoRedo", "history", "shortcuts", "layers", "deleteEl",
            // 左右面板
            "palette", "propertyPanel");

    /** 三档预设:只列「相对企业版全开」被关闭的键(= CmPrint EDITIONS 同名预设)。 */
    public static final Map<String, Map<String, Boolean>> PRESETS;

    /** 各档位 License 的 modules 字段(产品目录 CMPRINT 的模块编码;信息性,门禁以 features 为准)。 */
    public static final Map<String, List<String>> EDITION_MODULES;

    static {
        Map<String, Boolean> community = new LinkedHashMap<>();
        for (String k : List.of("exportPdf", "exportPdfText", "sharePdf", "exportWord", "exportExcel",
                "exportImage", "directPrint", "overprint", "calibration", "watermark",
                "subTemplate", "templateGallery", "theme")) {
            community.put(k, false);
        }
        Map<String, Boolean> professional = new LinkedHashMap<>();
        professional.put("directPrint", false);
        professional.put("templateGallery", false);

        // 注意:Map.copyOf 不保序,档位/能力键展示顺序有意义,统一用 unmodifiable LinkedHashMap
        Map<String, Map<String, Boolean>> presets = new LinkedHashMap<>();
        presets.put("COMMUNITY", java.util.Collections.unmodifiableMap(community));
        presets.put("PROFESSIONAL", java.util.Collections.unmodifiableMap(professional));
        presets.put("ENTERPRISE", Map.of());
        PRESETS = java.util.Collections.unmodifiableMap(presets);

        Map<String, List<String>> modules = new LinkedHashMap<>();
        modules.put("COMMUNITY", List.of("DESIGN", "DATA", "OUTPUT", "TEMPLATE"));
        modules.put("PROFESSIONAL", List.of("DESIGN", "DATA", "OUTPUT", "EXPORT", "TEMPLATE"));
        modules.put("ENTERPRISE", List.of("DESIGN", "DATA", "OUTPUT", "EXPORT", "TEMPLATE"));
        EDITION_MODULES = java.util.Collections.unmodifiableMap(modules);
    }

    /** 与前端 resolveEdition 同语义:全键开 → 套档位预设 → 按 overrides 微调,产出全键布尔表。 */
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
