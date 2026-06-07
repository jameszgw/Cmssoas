package com.codeman.platform.harden.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import proguard.Configuration;
import proguard.ConfigurationParser;
import proguard.ProGuard;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** ProGuard 混淆(库内调用)。保守默认:不裁剪/不优化、保留入口 main 与注解,避免运行期失败。 */
@Component
public class ObfuscateProvider implements HardenProvider {

    private static final Logger log = LoggerFactory.getLogger(ObfuscateProvider.class);

    @Override
    public HardenTechnique technique() { return HardenTechnique.OBFUSCATE; }

    @Override
    public boolean available() {
        try { Class.forName("proguard.ProGuard"); return true; } catch (Throwable t) { return false; }
    }

    @Override
    public String apply(Path in, Path out, HardenContext ctx) throws Exception {
        String javaHome = System.getProperty("java.home");
        List<String> args = new ArrayList<>(List.of(
                "-injars", in.toString(),
                "-outjars", out.toString()));
        // JDK 模块作为 library jars(仅核心,避免引入 java.desktop 等超大模块拖慢;缺失引用以 dontwarn 容错)
        for (String mod : new String[]{"java.base", "java.logging"}) {
            args.add("-libraryjars");
            args.add(javaHome + "/jmods/" + mod + ".jmod");
        }
        args.add("-dontshrink");
        args.add("-dontoptimize");
        args.add("-dontwarn");
        args.add("-ignorewarnings");
        args.add("-keepattributes");
        args.add("*Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable");
        // 保留可执行入口与 Spring Boot 启动器(若有),避免反射入口被改名
        args.add("-keep");
        args.add("public class * { public static void main(java.lang.String[]); }");
        args.add("-keep");
        args.add("class org.springframework.boot.loader.** { *; }");
        // 保留枚举/序列化等运行期反射约定
        args.add("-keepclassmembers");
        args.add("enum * { public static **[] values(); public static ** valueOf(java.lang.String); }");

        Configuration configuration = new Configuration();
        try (ConfigurationParser parser = new ConfigurationParser(args.toArray(new String[0]), System.getProperties())) {
            parser.parse(configuration);
        }
        new ProGuard(configuration).execute();
        log.info("[harden] ProGuard 混淆完成: {}", out.getFileName());
        return "ProGuard 混淆(保留 main/注解,内部类与成员改名)";
    }
}
