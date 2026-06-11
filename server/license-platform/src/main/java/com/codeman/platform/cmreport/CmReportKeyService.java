package com.codeman.platform.cmreport;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/**
 * CmReport 产品线的签名信任根:RSA-2048,SHA256withRSA。
 * CmReport 产品内置 X509 公钥(cmreport.license.public-key)做离线验签,
 * 与 CODEMAN 自身的 Ed25519/SM2 信任根相互独立(密钥隔离,互不影响轮换)。
 * 密钥来源优先级与 Ed25519KeyService 一致:env(KMS 注入,私钥不落盘)→ 本地文件 → 自动生成。
 */
@Component
public class CmReportKeyService {

    private static final Logger log = LoggerFactory.getLogger(CmReportKeyService.class);
    private static final String KEY_ALG = "RSA";
    private static final String SIG_ALG = "SHA256withRSA";

    private final ObjectMapper mapper;
    private final Path keyFile;
    private final String envPriv;
    private final String envPub;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public CmReportKeyService(ObjectMapper mapper,
                              @Value("${app.cmreport.key-file:./var/keys/cmreport-rsa.json}") String keyFile,
                              @Value("${app.cmreport.rsa-priv:}") String envPriv,
                              @Value("${app.cmreport.rsa-pub:}") String envPub) {
        this.mapper = mapper;
        this.keyFile = Path.of(keyFile);
        this.envPriv = envPriv;
        this.envPub = envPub;
    }

    @PostConstruct
    void init() throws Exception {
        if (envPriv != null && !envPriv.isBlank() && envPub != null && !envPub.isBlank()) {
            KeyFactory kf = KeyFactory.getInstance(KEY_ALG);
            this.privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(envPriv.trim())));
            this.publicKey = kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(envPub.trim())));
            log.info("[cmreport] 已从外部(KMS/env)加载 RSA 签名密钥(私钥不落盘),kid={}", kid());
        } else if (Files.exists(keyFile)) {
            load();
            log.info("[cmreport] 已加载 RSA 签名密钥:{}", keyFile);
        } else {
            generateAndSave();
            log.info("[cmreport] 已生成并持久化 RSA 签名密钥:{}", keyFile);
        }
    }

    public byte[] sign(byte[] data) {
        try {
            Signature s = Signature.getInstance(SIG_ALG);
            s.initSign(privateKey);
            s.update(data);
            return s.sign();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("CmReport License 签名失败", e);
        }
    }

    public boolean verify(byte[] data, byte[] signature) {
        try {
            Signature s = Signature.getInstance(SIG_ALG);
            s.initVerify(publicKey);
            s.update(data);
            return s.verify(signature);
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    /** X509 公钥 Base64(产品侧 cmreport.license.public-key 直接粘贴)。 */
    public String publicKeyBase64() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /** 密钥标识:SHA256(公钥) 前 16 位十六进制(轮换辨识)。 */
    public String kid() {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(publicKey.getEncoded());
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", d[i]));
            }
            return sb.toString();
        } catch (GeneralSecurityException e) {
            return "unknown";
        }
    }

    public String algorithm() {
        return SIG_ALG;
    }

    private void load() throws Exception {
        Map<?, ?> m = mapper.readValue(Files.readString(keyFile), Map.class);
        KeyFactory kf = KeyFactory.getInstance(KEY_ALG);
        this.privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(
                Base64.getDecoder().decode(String.valueOf(m.get("priv")))));
        this.publicKey = kf.generatePublic(new X509EncodedKeySpec(
                Base64.getDecoder().decode(String.valueOf(m.get("pub")))));
    }

    private void generateAndSave() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance(KEY_ALG);
        g.initialize(2048);
        KeyPair kp = g.generateKeyPair();
        this.privateKey = kp.getPrivate();
        this.publicKey = kp.getPublic();
        Files.createDirectories(keyFile.getParent());
        Files.writeString(keyFile, mapper.writeValueAsString(Map.of(
                "alg", SIG_ALG,
                "priv", Base64.getEncoder().encodeToString(privateKey.getEncoded()),
                "pub", Base64.getEncoder().encodeToString(publicKey.getEncoded()))));
    }
}
