package com.codeman.platform.license.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/**
 * 国密 SM2 签名信任根（BouncyCastle）。
 * 曲线 sm2p256v1 + SM3withSM2；与 Ed25519 实现共用 SignatureService 接口，按配置切换。
 * 启用：app.license.sign-algo=sm2。演示态密钥落盘；生产应入国密 HSM。
 */
@Component
@ConditionalOnProperty(name = "app.license.sign-algo", havingValue = "sm2")
public class Sm2SignatureService implements SignatureService {

    private static final Logger log = LoggerFactory.getLogger(Sm2SignatureService.class);
    private static final String ALG = "SM2";
    private static final String SIG_ALG = "SM3withSM2";
    private static final String BC = BouncyCastleProvider.PROVIDER_NAME;

    static {
        if (Security.getProvider(BC) == null) Security.addProvider(new BouncyCastleProvider());
    }

    private final ObjectMapper mapper;
    private final Path keyFile;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public Sm2SignatureService(ObjectMapper mapper,
                               @Value("${app.license.sm2-key-file:./var/keys/sm2.json}") String keyFile) {
        this.mapper = mapper;
        this.keyFile = Path.of(keyFile);
    }

    @PostConstruct
    void init() throws Exception {
        if (Files.exists(keyFile)) {
            load();
            log.info("[license] 已加载 SM2 国密签名密钥：{}", keyFile);
        } else {
            generateAndSave();
            log.info("[license] 已生成并持久化 SM2 国密签名密钥：{}", keyFile);
        }
    }

    @Override
    public byte[] sign(byte[] data) {
        try {
            Signature s = Signature.getInstance(SIG_ALG, BC);
            s.initSign(privateKey);
            s.update(data);
            return s.sign();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SM2 签名失败", e);
        }
    }

    @Override
    public boolean verify(byte[] data, byte[] signature) {
        try {
            Signature s = Signature.getInstance(SIG_ALG, BC);
            s.initVerify(publicKey);
            s.update(data);
            return s.verify(signature);
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    @Override
    public String publicKeyBase64() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    @Override
    public String algorithm() {
        return ALG;
    }

    @Override
    public String kid() {
        return SignatureKeys.kidOf(publicKey.getEncoded());
    }

    private void generateAndSave() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", BC);
        kpg.initialize(new ECGenParameterSpec("sm2p256v1"));
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
        KeyFactory kf = KeyFactory.getInstance("EC", BC);
        this.privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(m.get("priv"))));
        this.publicKey = kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(m.get("pub"))));
    }
}
