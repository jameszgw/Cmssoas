package com.codeman.protect;

import com.codeman.sdk.LicenseClaims;
import com.codeman.sdk.LicenseException;
import com.codeman.sdk.LicenseVerifier;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 受保护应用启动器：
 *   验签 License → 派生解密密钥 → 解密并加载 SecretAlgorithm → 运行。
 * 没有合法 License、被篡改、或换一张 License，都会失败（fail-closed）。
 *
 * 用法: ProtectedLauncher <outDir> <licFileName>
 */
public class ProtectedLauncher {

    public static void main(String[] args) {
        Path outDir = Path.of(args[0]);
        String licFile = args.length >= 2 ? args[1] : "license.lic";
        try {
            String pub = Files.readString(outDir.resolve("public.key")).trim();
            String lic = Files.readString(outDir.resolve(licFile)).trim();

            // 1) 验签（伪造/篡改在此被拦截）
            LicenseClaims claims;
            try {
                claims = new LicenseVerifier(pub).verify(lic);
            } catch (LicenseException e) {
                System.out.println("⛔ 启动失败：License 验签未通过 -> " + e.getMessage());
                System.exit(1); return;
            }
            if (!claims.isCurrentlyValid()) {
                System.out.println("⛔ 启动失败：License 不在有效期或非 ACTIVE");
                System.exit(1); return;
            }
            System.out.println("✅ License 验签通过：" + claims.licenseId() + " / " + claims.customer());

            // 2) 由 License 派生解密密钥
            byte[] key = CryptoKeys.deriveKey(claims.licenseId(), claims.watermark());

            // 3) 解密业务字节码
            byte[] enc = Files.readAllBytes(outDir.resolve("SecretAlgorithm.class.enc"));
            byte[] classBytes;
            try {
                classBytes = CryptoKeys.decrypt(enc, key, claims.licenseId());
            } catch (Exception e) {
                System.out.println("⛔ 启动失败：业务代码解密失败 —— 解密密钥与该 License 不匹配（代码与授权绑定）");
                System.exit(2); return;
            }

            // 4) 加载并运行
            Class<?> clazz;
            try {
                clazz = new EncryptedClassLoader(ProtectedLauncher.class.getClassLoader())
                        .defineFromBytes("com.codeman.protect.secret.SecretAlgorithm", classBytes);
            } catch (Throwable t) {
                System.out.println("⛔ 启动失败：解密后的字节码非法（密钥不匹配导致）");
                System.exit(2); return;
            }
            Object inst = clazz.getDeclaredConstructor().newInstance();
            Method m = clazz.getMethod("compute", int.class);
            Object result = m.invoke(inst, 42);
            System.out.println("🟢 受保护业务逻辑已运行：" + result);
        } catch (Exception e) {
            System.out.println("启动异常：" + e);
            System.exit(3);
        }
    }
}
