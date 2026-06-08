package com.codeman.protect;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.LocalDate;
import java.util.*;

/**
 * 构建期保护工具：
 *  1) 生成 Ed25519 密钥对（演示；生产私钥在 KMS）
 *  2) 签发两张合法 License（license.lic 与 license-other.lic，密钥相同、内容不同）
 *  3) 用「license.lic 派生的密钥」加密 SecretAlgorithm.class → .class.enc
 *  4) 删除交付物中的明文 SecretAlgorithm.class
 *
 * 用法: BuildProtected <classesDir> <outDir>
 */
public class BuildProtected {

    static final ObjectMapper M = new ObjectMapper();
    static final Base64.Encoder B64U = Base64.getUrlEncoder().withoutPadding();
    static final String SECRET_CLASS = "com/codeman/protect/secret/SecretAlgorithm.class";

    public static void main(String[] args) throws Exception {
        Path classesDir = Path.of(args[0]);
        Path outDir = Path.of(args[1]);
        Files.createDirectories(outDir);

        KeyPair kp = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        Files.writeString(outDir.resolve("public.key"),
                Base64.getEncoder().encodeToString(kp.getPublic().getEncoded()));

        String lic1 = signLicense(kp.getPrivate(), "LIC-DEMO-0001", "WM-AAAA1111");
        String lic2 = signLicense(kp.getPrivate(), "LIC-DEMO-0002", "WM-BBBB2222");
        Files.writeString(outDir.resolve("license.lic"), lic1);
        Files.writeString(outDir.resolve("license-other.lic"), lic2);

        // 用 license.lic 派生密钥加密 SecretAlgorithm.class
        Path plain = classesDir.resolve(SECRET_CLASS);
        byte[] classBytes = Files.readAllBytes(plain);
        byte[] key = CryptoKeys.deriveKey("LIC-DEMO-0001", "WM-AAAA1111");
        byte[] enc = CryptoKeys.encrypt(classBytes, key, "LIC-DEMO-0001");
        Files.write(outDir.resolve("SecretAlgorithm.class.enc"), enc);

        // 删除明文 class（交付物中不再包含可反编译的业务类）
        Files.delete(plain);

        System.out.println("[build] 公钥 + 两张 License 已生成；SecretAlgorithm.class 已加密并移除明文");
        System.out.println("[build] 加密密钥绑定 license.lic（LIC-DEMO-0001 / WM-AAAA1111）");
    }

    static String signLicense(PrivateKey priv, String licenseId, String watermark) throws Exception {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("schemaVersion", 2);
        c.put("licenseId", licenseId);
        c.put("licenseVersion", 1);
        c.put("tenantCode", "T-DEMO");
        c.put("customer", "演示客户");
        c.put("edition", "ENTERPRISE");
        c.put("mode", "OFFLINE");
        c.put("modules", List.of("RISK", "REPORT"));
        c.put("features", Map.of("REPORT.EXPORT", true, "MAX_USERS", 100));
        c.put("appVersionRange", ">=2.0.0 <3.0.0");
        c.put("notBefore", LocalDate.now().minusDays(1).toString());
        c.put("notAfter", LocalDate.now().plusYears(1).toString());
        c.put("concurrency", 5);
        c.put("status", "ACTIVE");
        c.put("watermark", watermark);
        c.put("issuedAt", java.time.LocalDateTime.now().toString());
        byte[] payload = M.writeValueAsString(c).getBytes(StandardCharsets.UTF_8);
        Signature s = Signature.getInstance("Ed25519");
        s.initSign(priv);
        s.update(payload);
        return B64U.encodeToString(payload) + "." + B64U.encodeToString(s.sign());
    }
}
