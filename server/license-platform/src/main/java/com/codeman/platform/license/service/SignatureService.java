package com.codeman.platform.license.service;

/**
 * License 签名信任根抽象。可插拔实现：
 *   - Ed25519（默认，JDK 原生）
 *   - SM2（国密，BouncyCastle）
 * 通过配置 app.license.sign-algo = ed25519 | sm2 切换。
 */
public interface SignatureService {

    /** 用服务端私钥签名。 */
    byte[] sign(byte[] data);

    /** 用服务端公钥验签。 */
    boolean verify(byte[] data, byte[] signature);

    /** 公钥（Base64，X.509 编码），供客户端 SDK 内置验签。 */
    String publicKeyBase64();

    /** 算法名（Ed25519 / SM2），写入 License 声明并供 SDK 选择验签算法。 */
    String algorithm();

    /** 密钥标识（kid），写入 License 声明；支持多公钥轮换（SDK 按 kid 选公钥验签）。 */
    String kid();
}
