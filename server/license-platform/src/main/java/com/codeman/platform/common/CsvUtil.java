package com.codeman.platform.common;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** 简单 CSV 导出工具（带 UTF-8 BOM，Excel 直接打开不乱码）。 */
public final class CsvUtil {
    private CsvUtil() {}

    public static String cell(Object v) {
        String s = v == null ? "" : String.valueOf(v);
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    public static ResponseEntity<byte[]> response(String filename, List<String> header, List<List<Object>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append('﻿'); // BOM
        sb.append(String.join(",", header)).append("\r\n");
        for (List<Object> r : rows) {
            for (int i = 0; i < r.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(cell(r.get(i)));
            }
            sb.append("\r\n");
        }
        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        h.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(h).body(body);
    }
}
