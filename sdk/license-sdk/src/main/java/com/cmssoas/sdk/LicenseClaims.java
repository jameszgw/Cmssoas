package com.cmssoas.sdk;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** 已验签的 License 内容，提供功能/版本/有效期门禁判断。 */
public class LicenseClaims {

    private final Map<String, Object> raw;

    @SuppressWarnings("unchecked")
    LicenseClaims(Map<String, Object> raw) {
        this.raw = raw;
    }

    public String licenseId() { return str("licenseId"); }
    public int licenseVersion() { return intv("licenseVersion"); }
    public String tenantCode() { return str("tenantCode"); }
    public String customer() { return str("customer"); }
    public String edition() { return str("edition"); }
    public String mode() { return str("mode"); }
    public String status() { return str("status"); }
    public String watermark() { return str("watermark"); }
    public String appVersionRange() { return str("appVersionRange"); }
    public String notBefore() { return str("notBefore"); }
    public String notAfter() { return str("notAfter"); }

    @SuppressWarnings("unchecked")
    public List<String> modules() {
        Object m = raw.get("modules");
        return m instanceof List ? (List<String>) m : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> features() {
        Object f = raw.get("features");
        return f instanceof Map ? (Map<String, Object>) f : Collections.emptyMap();
    }

    public Map<String, Object> raw() { return raw; }

    // ---------- 门禁 ----------
    /** 当前是否在有效期内且状态为 ACTIVE。 */
    public boolean isCurrentlyValid() {
        if (!"ACTIVE".equalsIgnoreCase(status())) return false;
        LocalDate now = LocalDate.now();
        try {
            if (now.isBefore(LocalDate.parse(notBefore()))) return false;
            if (now.isAfter(LocalDate.parse(notAfter()))) return false;
        } catch (Exception ignore) { return false; }
        return true;
    }

    public boolean hasModule(String module) { return modules().contains(module); }

    /** 功能点是否授权（features[feature] == true）。 */
    public boolean hasFeature(String feature) {
        Object v = features().get(feature);
        return v instanceof Boolean ? (Boolean) v : Boolean.parseBoolean(String.valueOf(v));
    }

    public long quota(String key) {
        Object v = features().get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return 0; }
    }

    /** 被授权软件版本是否落在授权区间内。 */
    public boolean appVersionAllowed(String appVersion) {
        return SemVer.satisfies(appVersion, appVersionRange());
    }

    private String str(String k) { Object v = raw.get(k); return v == null ? null : String.valueOf(v); }
    private int intv(String k) { Object v = raw.get(k); return v instanceof Number ? ((Number) v).intValue() : 0; }
}
