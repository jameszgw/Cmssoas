package com.cmssoas.sdk;

/** 极简语义化版本比较与区间判断（支持 >=, >, <=, <, =, 以空格分隔多个约束）。 */
public final class SemVer {
    private SemVer() {}

    public static int compare(String a, String b) {
        int[] x = parse(a), y = parse(b);
        for (int i = 0; i < 3; i++) {
            if (x[i] != y[i]) return Integer.compare(x[i], y[i]);
        }
        return 0;
    }

    private static int[] parse(String v) {
        String s = v.trim();
        if (s.startsWith("v") || s.startsWith("V")) s = s.substring(1);
        // 去除预发布/构建元数据
        int dash = s.indexOf('-'); if (dash >= 0) s = s.substring(0, dash);
        String[] p = s.split("\\.");
        int[] r = new int[3];
        for (int i = 0; i < 3 && i < p.length; i++) {
            try { r[i] = Integer.parseInt(p[i].replaceAll("[^0-9]", "")); } catch (Exception e) { r[i] = 0; }
        }
        return r;
    }

    /** 判断版本是否满足区间表达式，如 ">=2.0.0 <3.0.0"。空表达式视为始终满足。 */
    public static boolean satisfies(String version, String range) {
        if (range == null || range.isBlank()) return true;
        for (String token : range.trim().split("\\s+")) {
            if (!satisfyOne(version, token)) return false;
        }
        return true;
    }

    private static boolean satisfyOne(String version, String token) {
        String op, ver;
        if (token.startsWith(">=")) { op = ">="; ver = token.substring(2); }
        else if (token.startsWith("<=")) { op = "<="; ver = token.substring(2); }
        else if (token.startsWith(">")) { op = ">"; ver = token.substring(1); }
        else if (token.startsWith("<")) { op = "<"; ver = token.substring(1); }
        else if (token.startsWith("=")) { op = "="; ver = token.substring(1); }
        else { op = "="; ver = token; }
        int c = compare(version, ver);
        return switch (op) {
            case ">=" -> c >= 0;
            case "<=" -> c <= 0;
            case ">" -> c > 0;
            case "<" -> c < 0;
            default -> c == 0;
        };
    }
}
