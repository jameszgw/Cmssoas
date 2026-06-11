package com.codeman.platform.cmreport;

import com.codeman.platform.license.domain.License;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CmReport 产品格式的 License 令牌签发器。
 * 产品侧(com.codeman.report.license.LicenseSigner#verifyAndParse)约定:
 * 令牌 = base64url(payloadJson) + "." + base64url(SHA256withRSA 签名);payload 字段:
 * {licenseId, customer, edition, issuedAt(ms), expiresAt(ms;<=0 永久), capabilities, addons, limits, fingerprint}。
 * 其中 edition 为版本基线(产品按 EditionCapabilities 推导能力集),addons/capabilities 为基线之上的附加能力。
 * 附加信息(addons/limits/fingerprint)持久化在平台 License.features JSON 中:
 * {"cmEdition":"pro","addons":[...],"limits":{...},"fingerprint":"..."}。
 */
@Component
public class CmReportLicenseSigner {

    /** 产品码(License.productCode 取该值时走本签发器)。 */
    public static final String PRODUCT_CODE = "CMREPORT";

    private static final Base64.Encoder B64U = Base64.getUrlEncoder().withoutPadding();

    private final CmReportKeyService keyService;
    private final ObjectMapper mapper;

    public CmReportLicenseSigner(CmReportKeyService keyService, ObjectMapper mapper) {
        this.keyService = keyService;
        this.mapper = mapper;
    }

    /** 是否 CmReport 产品行。 */
    public static boolean isCmReport(License l) {
        return l != null && PRODUCT_CODE.equalsIgnoreCase(l.getProductCode());
    }

    /**
     * 依据实体当前字段构建 CmReport 格式 payload、RSA 签名并写回(claimsJson / signature / lic)。
     * 续期/变更复用:每次重签 issuedAt 取当前时间,expiresAt 取 notAfter 当日 23:59:59(本地时区)。
     */
    public void resign(License l) {
        Map<String, Object> features = readJson(l.getFeatures());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("licenseId", l.getLicenseId());
        payload.put("customer", l.getCustomer());
        payload.put("edition", String.valueOf(features.getOrDefault("cmEdition",
                l.getEdition() == null ? "community" : l.getEdition())).toLowerCase());
        payload.put("issuedAt", System.currentTimeMillis());
        payload.put("expiresAt", l.getNotAfter() == null ? 0L
                : l.getNotAfter().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        List<String> addons = toList(features.get("addons"));
        payload.put("capabilities", addons); // 产品侧 capabilities 与 addons 均并入有效能力
        payload.put("addons", addons);
        payload.put("limits", toLimits(features.get("limits"), l.getConcurrency()));
        Object fp = features.get("fingerprint");
        payload.put("fingerprint", fp == null ? "" : String.valueOf(fp));

        String json = writeJson(payload);
        byte[] payloadBytes = json.getBytes(StandardCharsets.UTF_8);
        byte[] sig = keyService.sign(payloadBytes);
        l.setClaimsJson(json);
        l.setSignature(B64U.encodeToString(sig));
        l.setLic(B64U.encodeToString(payloadBytes) + "." + B64U.encodeToString(sig));
    }

    private List<String> toList(Object v) {
        List<String> out = new ArrayList<>();
        if (v instanceof List<?> list) {
            for (Object o : list) {
                if (o != null && !String.valueOf(o).isBlank()) {
                    out.add(String.valueOf(o));
                }
            }
        }
        return out;
    }

    /** 限额:features.limits 优先;未给 concurrency 时回填实体 concurrency(>0)。 */
    private Map<String, Long> toLimits(Object v, Integer concurrency) {
        Map<String, Long> out = new LinkedHashMap<>();
        if (v instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                try {
                    out.put(String.valueOf(e.getKey()), Long.parseLong(String.valueOf(e.getValue()).trim()));
                } catch (NumberFormatException ignore) {
                    // 非数字限额忽略
                }
            }
        }
        if (!out.containsKey("concurrency") && concurrency != null && concurrency > 0) {
            out.put("concurrency", concurrency.longValue());
        }
        return out;
    }

    private String writeJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new IllegalStateException("CmReport License JSON 序列化失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJson(String s) {
        try {
            return s == null || s.isBlank() ? new LinkedHashMap<>() : mapper.readValue(s, LinkedHashMap.class);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }
}
