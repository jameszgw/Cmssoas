package com.codeman.platform.license.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/**
 * License 签名信任根：Ed25519 非对称密钥。
 * 私钥仅用于服务端签发；公钥可公开给客户端 SDK 验签。
 * 演示态：密钥持久化到本地文件；生产应托管 KMS/HSM（私钥不出域）。
 */
@Component
@ConditionalOnProperty(name = "app.license.sign-algo", havingValue = "ed25519", matchIfMissing = true)
public class Ed25519KeyService implements SignatureService {

    private static final Logger log = LoggerFactory.getLogger(Ed25519KeyService.class);
    private static final String ALG = "Ed25519";

    private final ObjectMapper mapper;
    private final Path keyFile;
    private final String envPriv;
    private final String envPub;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public Ed25519KeyService(ObjectMapper mapper,
                             @Value("${app.license.key-file:./var/keys/ed25519.json}") String keyFile,
                             @Value("${app.license.ed25519-priv:}") String envPriv,
                             @Value("${app.license.ed25519-pub:}") String envPub) {
        this.mapper = mapper;
        this.keyFile = Path.of(keyFile);
        this.envPriv = envPriv;
        this.envPub = envPub;
    }

    @PostConstruct
    void init() throws Exception {
        // 优先：从外部注入（KMS/Vault/env 提供 PKCS8/X509 Base64），私钥不落盘
        if (envPriv != null && !envPriv.isBlank() && envPub != null && !envPub.isBlank()) {
            KeyFactory kf = KeyFactory.getInstance(ALG);
            this.privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(envPriv.trim())));
            this.publicKey = kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(envPub.trim())));
            log.info("[license] 已从外部(KMS/env)加载 Ed25519 签名密钥（私钥不落盘），kid={}", kid());
        } else if (Files.exists(keyFile)) {
            load();
            log.info("[license] 已加载 Ed25519 签名密钥：{}", keyFile);
        } else {
            generateAndSave();
            log.info("[license] 已生成并持久化 Ed25519 签名密钥：{}", keyFile);
        }
    }

    public byte[] sign(byte[] data) {
        try {
            Signature s = Signature.getInstance(ALG);
            s.initSign(privateKey);
            s.update(data);
            return s.sign();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("License 签名失败", e);
        }
    }

    public boolean verify(byte[] data, byte[] signature) {
        try {
            Signature s = Signature.getInstance(ALG);
            s.initVerify(publicKey);
            s.update(data);
            return s.verify(signature);
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    /** 公钥（X.509，Base64），供 SDK 内置验签。 */
    public String publicKeyBase64() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public String algorithm() {
        return ALG;
    }

    @Override
    public String kid() {
        return SignatureKeys.kidOf(publicKey.getEncoded());
    }

    private void generateAndSave() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALG);
        KeyPair kp = kpg.generateKeyPair();
        this.privateKey = kp.getPrivate();
        this.publicKey = kp.getPublic();
        Files.createDirectories(keyFile.getParent());
        Map<String, String> m = Map.of(
                "alg", ALG,
                "priv", Base64.getEncoder().encodeToString(privateKey.getEncoded()),
                "pub", Base64.getEncoder().encodeToString(publicKey.getEncoded())
        );
        Files.writeString(keyFile, mapper.writeValueAsString(m));
    }

    @SuppressWarnings("unchecked")
    private void load() throws Exception {
        Map<String, String> m = mapper.readValue(Files.readAllBytes(keyFile), Map.class);
        KeyFactory kf = KeyFactory.getInstance(ALG);
        this.privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(m.get("priv"))));
        this.publicKey = kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(m.get("pub"))));
    }
}
