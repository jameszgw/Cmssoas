package com.codeman.platform.harden.service;

import java.nio.file.Path;
import java.util.Map;

/**
 * 加固器薄抽象(provider-agnostic)。内置 ProGuard 混淆与 AES 类加密两类实现;
 * 预留商用器(Allatori / ClassFinal / Xjar 等)——新增实现并注册即可,业务流水线无感。
 */
public interface HardenProvider {

    /** 本实现负责的技术。 */
    HardenTechnique technique();

    /** 工具链是否就绪(不就绪则任务标记失败并给出可读原因)。 */
    boolean available();

    /**
     * 处理单步:读取 in jar,产出加固后的 out jar。
     * @param ctx 上下文(密钥材料、加密范围、运行期注入类等)
     * @return 处理摘要(写入任务 message)
     */
    String apply(Path in, Path out, HardenContext ctx) throws Exception;

    /** 加固上下文。 */
    final class HardenContext {
        /** ENCRYPT_BIND:绑定 License 的 .lic 原文(运行期凭此 .lic 派生同一密钥)。 */
        public String licenseLic;
        /** FATJAR_ENCRYPT:口令(运行期凭此口令解密)。 */
        public String passphrase;
        /** 加密范围:仅加密该包前缀(含子包)下的类;为空则自动按 Main-Class 顶层包推断。 */
        public String encryptPrefix;
        /** 运行期需注入产物的类字节(类二进制名→字节),由服务从自身 classpath 提供。 */
        public Map<String, byte[]> runtimeClasses;
    }
}
