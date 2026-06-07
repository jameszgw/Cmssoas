package com.codeman.platform.features;

import com.codeman.platform.harden.domain.HardenJob;
import com.codeman.platform.harden.service.HardenService;
import com.codeman.platform.harden.service.HardenTechnique;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.tools.ToolProvider;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.jar.*;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 在线代码加固:口令式类加密(FATJAR_ENCRYPT)端到端——编译微型 jar → 提交 → 异步完成 →
 * 校验产物含 enc/ 密文与注入的运行期启动器,且清单入口改为 HardenLauncher。
 * (不跑 ProGuard,保证 CI 快速;ProGuard 路径由真机/手动验证。)
 */
@SpringBootTest
class HardenIntegrationTest {

    @Autowired HardenService harden;

    @Test
    void passphraseEncryptProducesRunnableArtifact() throws Exception {
        Path dir = Files.createTempDirectory("hardentest");
        Path src = dir.resolve("demo");
        Files.createDirectories(src);
        Files.writeString(src.resolve("Demo.java"),
                "package demo; public class Demo { public static void main(String[] a){ System.out.println(\"hi\"); } }");
        Path out = dir.resolve("out");
        Files.createDirectories(out);
        int rc = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-d", out.toString(), src.resolve("Demo.java").toString());
        assertEquals(0, rc, "示例编译应成功");

        Path jar = dir.resolve("app.jar");
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mf.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "demo.Demo");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar), mf)) {
            jos.putNextEntry(new ZipEntry("demo/Demo.class"));
            jos.write(Files.readAllBytes(out.resolve("demo/Demo.class")));
            jos.closeEntry();
        }

        HardenJob job;
        try (InputStream in = Files.newInputStream(jar)) {
            job = harden.submit(null, List.of(HardenTechnique.FATJAR_ENCRYPT), null, "pw123", null, "app.jar", in);
        }
        // 等待异步完成
        String st = "QUEUED";
        for (int i = 0; i < 100; i++) {
            st = harden.get(job.getId()).getStatus();
            if ("DONE".equals(st) || "FAILED".equals(st)) break;
            Thread.sleep(100);
        }
        HardenJob done = harden.get(job.getId());
        assertEquals("DONE", done.getStatus(), "加固应完成,message=" + done.getMessage());

        try (JarFile hj = new JarFile(harden.artifact(job.getId()).toFile())) {
            assertNotNull(hj.getEntry("enc/demo/Demo.class.enc"), "应有加密后的类");
            assertNull(hj.getEntry("demo/Demo.class"), "明文类应被移除");
            assertNotNull(hj.getEntry("com/codeman/platform/harden/runtime/HardenLauncher.class"), "应注入启动器");
            assertEquals("com.codeman.platform.harden.runtime.HardenLauncher",
                    hj.getManifest().getMainAttributes().getValue(Attributes.Name.MAIN_CLASS), "入口应改为启动器");
            assertEquals("demo.Demo", hj.getManifest().getMainAttributes().getValue("Harden-Real-Main"));
            assertEquals("PASSPHRASE", hj.getManifest().getMainAttributes().getValue("Harden-Mode"));
        }
    }
}
