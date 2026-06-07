package com.codeman.platform.mail;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * 基于时间的一次性口令 TOTP（RFC 6238，SHA1 / 30s / 6 位），及 Base32 编解码。
 * 自实现，无第三方依赖；用于超管激活时绑定 MFA。
 */
@Component
public class Totp {

    private static final String B32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DIGITS = 6;
    private static final int PERIOD = 30;
    private static final String ISSUER = "CMSSOAS";

    /** 生成 20 字节随机密钥的 Base32 表示。 */
    public String generateSecret() {
        byte[] buf = new byte[20];
        RANDOM.nextBytes(buf);
        return base32Encode(buf);
    }

    /** 构造 otpauth:// 链接，供认证器扫码。 */
    public String otpauthUri(String secretBase32, String account) {
        String label = enc(ISSUER + ":" + account);
        return "otpauth://totp/" + label
                + "?secret=" + secretBase32
                + "&issuer=" + enc(ISSUER)
                + "&algorithm=SHA1&digits=" + DIGITS + "&period=" + PERIOD;
    }

    /** 校验 6 位验证码（容许 ±1 个时间窗，应对时钟漂移）。 */
    public boolean verify(String secretBase32, String code) {
        if (secretBase32 == null || code == null) return false;
        String c = code.trim();
        if (!c.matches("\\d{6}")) return false;
        byte[] key = base32Decode(secretBase32);
        long counter = System.currentTimeMillis() / 1000L / PERIOD;
        for (long w = -1; w <= 1; w++) {
            if (c.equals(generateCode(key, counter + w))) return true;
        }
        return false;
    }

    private String generateCode(byte[] key, long counter) {
        try {
            byte[] data = new byte[8];
            long v = counter;
            for (int i = 7; i >= 0; i--) { data[i] = (byte) (v & 0xff); v >>= 8; }
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] h = mac.doFinal(data);
            int offset = h[h.length - 1] & 0x0f;
            int binary = ((h[offset] & 0x7f) << 24)
                    | ((h[offset + 1] & 0xff) << 16)
                    | ((h[offset + 2] & 0xff) << 8)
                    | (h[offset + 3] & 0xff);
            int otp = binary % (int) Math.pow(10, DIGITS);
            return String.format("%0" + DIGITS + "d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("TOTP 计算失败", e);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bits = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                sb.append(B32.charAt((buffer >> (bits - 5)) & 0x1f));
                bits -= 5;
            }
        }
        if (bits > 0) sb.append(B32.charAt((buffer << (5 - bits)) & 0x1f));
        return sb.toString();
    }

    static byte[] base32Decode(String s) {
        s = s.replace("=", "").toUpperCase();
        int buffer = 0, bits = 0;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (char c : s.toCharArray()) {
            int idx = B32.indexOf(c);
            if (idx < 0) continue;
            buffer = (buffer << 5) | idx;
            bits += 5;
            if (bits >= 8) {
                out.write((buffer >> (bits - 8)) & 0xff);
                bits -= 8;
            }
        }
        return out.toByteArray();
    }
}
