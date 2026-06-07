package com.codeman.platform.harden.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.*;
import java.util.zip.ZipEntry;

/**
 * AES 类加密核心:把源 jar 中指定包前缀下的 .class 加密为 {@code enc/<path>.class.enc} 并移除明文,
 * 注入运行期解密启动器/类加载器,并改写清单(Main-Class=HardenLauncher,记录原 Main 与模式)。
 * 产物运行:LICENSE_BIND→ {@code -Dharden.license=<.lic>};PASSPHRASE→ {@code -Dharden.key=<口令>}。
 */
final class EncryptCore {
    private EncryptCore() {}

    static final String LAUNCHER = "com.codeman.platform.harden.runtime.HardenLauncher";

    /** @return 处理摘要 */
    static String encrypt(Path in, Path out, byte[] key, String mode, HardenProvider.HardenContext ctx) throws Exception {
        try (JarFile jar = new JarFile(in.toFile())) {
            Manifest srcMf = jar.getManifest();
            String realMain = srcMf == null ? null : srcMf.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
            if (realMain == null || realMain.isBlank())
                throw new IllegalStateException("源 jar 无 Main-Class,无法加密绑定可执行入口(请上传可执行 jar)");
            String prefix = (ctx.encryptPrefix != null && !ctx.encryptPrefix.isBlank())
                    ? ctx.encryptPrefix.trim() : inferPrefix(realMain);

            Manifest outMf = new Manifest(srcMf);
            Attributes a = outMf.getMainAttributes();
            a.put(Attributes.Name.MAIN_CLASS, LAUNCHER);
            a.putValue("Harden-Real-Main", realMain);
            a.putValue("Harden-Mode", mode);
            a.putValue("Harden-Enc-Prefix", prefix);

            int enc = 0, copied = 0;
            try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(out), outMf)) {
                var entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry e = entries.nextElement();
                    String n = e.getName();
                    if (e.isDirectory()) continue;
                    if (n.equals(JarFile.MANIFEST_NAME)) continue;
                    // 丢弃原签名文件,避免加固后签名校验失败
                    if (n.startsWith("META-INF/") && (n.endsWith(".SF") || n.endsWith(".RSA") || n.endsWith(".DSA") || n.endsWith(".EC"))) continue;

                    byte[] data;
                    try (InputStream is = jar.getInputStream(e)) { data = is.readAllBytes(); }

                    String cls = className(n);
                    if (cls != null && (cls.equals(prefix) || cls.startsWith(prefix + ".")) && !cls.startsWith("com.codeman.platform.harden.runtime.")) {
                        byte[] blob = HardenCrypto.encrypt(data, key);
                        jos.putNextEntry(new ZipEntry("enc/" + n + ".enc"));
                        jos.write(blob);
                        jos.closeEntry();
                        enc++;
                    } else {
                        jos.putNextEntry(new ZipEntry(n));
                        jos.write(data);
                        jos.closeEntry();
                        copied++;
                    }
                }
                // 注入运行期类(纯 JDK,无外部依赖)
                for (Map.Entry<String, byte[]> rc : ctx.runtimeClasses.entrySet()) {
                    jos.putNextEntry(new ZipEntry(rc.getKey().replace('.', '/') + ".class"));
                    jos.write(rc.getValue());
                    jos.closeEntry();
                }
            }
            if (enc == 0) throw new IllegalStateException("未匹配到可加密的类(前缀 " + prefix + "):请用 encryptPrefix 指定业务包");
            return "加密 " + enc + " 个类(前缀 " + prefix + "),保留 " + copied + " 项;入口改为启动器,原 Main=" + realMain;
        }
    }

    /** className: a/b/C.class -> a.b.C;非类返回 null。 */
    private static String className(String entry) {
        if (!entry.endsWith(".class") || entry.equals("module-info.class") || entry.startsWith("META-INF/")) return null;
        return entry.substring(0, entry.length() - 6).replace('/', '.');
    }

    /** 由 Main-Class 推断加密前缀:取包名前 ≤2 段(无包则取首段)。 */
    private static String inferPrefix(String mainClass) {
        int dot = mainClass.lastIndexOf('.');
        String pkg = dot < 0 ? mainClass : mainClass.substring(0, dot);
        String[] segs = pkg.split("\\.");
        int take = Math.min(2, segs.length);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < take; i++) { if (i > 0) sb.append('.'); sb.append(segs[i]); }
        return sb.toString();
    }
}
