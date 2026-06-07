package com.codeman.platform.harden.runtime;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * 加固产物启动器(随产物注入,纯 JDK)。被设为 jar 的 Main-Class;运行期:
 * 1) 读取清单中的 Harden-Real-Main / Harden-Mode;
 * 2) 派生 AES 密钥(LICENSE_BIND:由 -Dharden.license 指向的 .lic 内容派生;
 *    PASSPHRASE:由 -Dharden.key/环境变量 HARDEN_KEY 口令派生);
 * 3) 用 {@link HardenClassLoader} 解密加载原始 Main 并执行。
 */
public final class HardenLauncher {

    public static void main(String[] args) throws Exception {
        File self = new File(HardenLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        try (JarFile jar = new JarFile(self)) {
            Manifest mf = jar.getManifest();
            String realMain = mf.getMainAttributes().getValue("Harden-Real-Main");
            String mode = mf.getMainAttributes().getValue("Harden-Mode");
            if (realMain == null) throw new IllegalStateException("加固包缺少 Harden-Real-Main");

            byte[] key = deriveKey(mode);
            HardenClassLoader cl = new HardenClassLoader(jar, key, ClassLoader.getSystemClassLoader());
            Thread.currentThread().setContextClassLoader(cl);
            Class<?> main = Class.forName(realMain, true, cl);
            Method m = main.getMethod("main", String[].class);
            m.invoke(null, (Object) args);
        }
    }

    private static byte[] deriveKey(String mode) throws Exception {
        String material;
        if ("LICENSE_BIND".equals(mode)) {
            String lp = prop("harden.license", "HARDEN_LICENSE");
            if (lp == null) throw new IllegalStateException("本程序已与 License 绑定:请用 -Dharden.license=<.lic 路径> 或环境变量 HARDEN_LICENSE 提供合法授权");
            material = new String(Files.readAllBytes(Paths.get(lp)), StandardCharsets.UTF_8).trim();
        } else {
            String k = prop("harden.key", "HARDEN_KEY");
            if (k == null) throw new IllegalStateException("本程序已加密:请用 -Dharden.key=<口令> 或环境变量 HARDEN_KEY 提供解密口令");
            material = k;
        }
        return MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
    }

    private static String prop(String sys, String env) {
        String v = System.getProperty(sys);
        if (v == null || v.isBlank()) v = System.getenv(env);
        return (v == null || v.isBlank()) ? null : v;
    }
}
