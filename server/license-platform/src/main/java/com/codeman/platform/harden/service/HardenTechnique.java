package com.codeman.platform.harden.service;

/** 加固技术(可叠加,按声明顺序执行)。 */
public enum HardenTechnique {
    /** ProGuard 混淆。 */
    OBFUSCATE,
    /** 关键类 AES 加密 + 与 License 绑定(运行期需提供该 .lic 解密)。 */
    ENCRYPT_BIND,
    /** 关键类 AES 加密 + 口令(运行期需提供口令解密;胖包/类加密)。 */
    FATJAR_ENCRYPT
}
