package com.cmssoas.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/**
 * License 验签器：用内置公钥验证 .lic 签名。客户端只持有公钥 —— 无法伪造合法 License。
 * 支持两种算法（由 .lic 内 sigAlg 声明自动选择）：
 *   - Ed25519（默认，JDK 原生）
 *   - SM2（国密，BouncyCastle）
 */
public class LicenseVerifier {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Base64.Decoder B64U = Base64.getUrlDecoder();
    private static final String BC = BouncyCastleProvider.PROVIDER_NAME;

    static {
        if (Security.getProvider(BC) == null) Security.addProvider(new BouncyCastleProvider());
    }

    private final byte[] publicKeyDer;

    /** @param publicKeyBase64 X.509 编码公钥的 Base64（由平台 /api/licenses/public-key 提供）。 */
    public LicenseVerifier(String publicKeyBase64) {
        this.publicKeyDer = Base64.getDecoder().decode(publicKeyBase64.trim());
    }

    /**
     * 验签并解析 .lic（格式：base64url(payload).base64url(signature)）。
     * @throws LicenseException 签名无效或格式错误（即“伪造/被篡改”）。
     */
    public LicenseClaims verify(String lic) {
        Parsed p = parse(lic);
        check(p.payload, p.sig, alg(p.claims), publicKeyDer);
        return new LicenseClaims(p.claims);
    }

    /**
     * 多公钥（kid）验签：用于密钥轮换。按 License 内 kid 选对应公钥。
     * @param keysByKid kid -> 公钥 Base64（来自平台 /api/licenses/public-keys，JWKS 风格）。
     */
    public static LicenseClaims verify(String lic, Map<String, String> keysByKid) {
        Parsed p = parse(lic);
        Object kid = p.claims.get("kid");
        if (kid == null) throw new LicenseException("License 缺少 kid，无法选择公钥");
        String pub = keysByKid.get(String.valueOf(kid));
        if (pub == null) throw new LicenseException("未知 kid：" + kid + "（无对应公钥）");
        check(p.payload, p.sig, alg(p.claims), Base64.getDecoder().decode(pub.trim()));
        return new LicenseClaims(p.claims);
    }

    // ---- 内部 ----
    private record Parsed(byte[] payload, byte[] sig, Map<String, Object> claims) {}

    private static Parsed parse(String lic) {
        if (lic == null) throw new LicenseException("License 为空");
        String[] parts = lic.trim().split("\\.");
        if (parts.length != 2) throw new LicenseException("License 格式错误");
        byte[] payload, sig;
        try {
            payload = B64U.decode(parts[0]);
            sig = B64U.decode(parts[1]);
        } catch (Exception e) {
            throw new LicenseException("License 编码错误", e);
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = MAPPER.readValue(new String(payload, StandardCharsets.UTF_8), Map.class);
            return new Parsed(payload, sig, m);
        } catch (Exception e) {
            throw new LicenseException("claims 解析失败", e);
        }
    }

    private static String alg(Map<String, Object> claims) {
        return String.valueOf(claims.getOrDefault("sigAlg", "Ed25519"));
    }

    private static void check(byte[] payload, byte[] sig, String alg, byte[] pubDer) {
        try {
            PublicKey pk;
            Signature s;
            if ("SM2".equalsIgnoreCase(alg)) {
                pk = KeyFactory.getInstance("EC", BC).generatePublic(new X509EncodedKeySpec(pubDer));
                s = Signature.getInstance("SM3withSM2", BC);
            } else {
                pk = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(pubDer));
                s = Signature.getInstance("Ed25519");
            }
            s.initVerify(pk);
            s.update(payload);
            if (!s.verify(sig)) {
                throw new LicenseException("签名无效：License 可能被伪造或篡改");
            }
        } catch (LicenseException le) {
            throw le;
        } catch (Exception e) {
            throw new LicenseException("验签失败", e);
        }
    }
}
