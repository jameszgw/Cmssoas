package com.cmssoas.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/**
 * License 验签器：用内置公钥（Ed25519）验证 .lic 签名。
 * 客户端只持有公钥 —— 无法伪造合法 License。
 */
public class LicenseVerifier {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Base64.Decoder B64U = Base64.getUrlDecoder();

    private final PublicKey publicKey;

    /** @param publicKeyBase64 X.509 编码公钥的 Base64（由平台 /api/licenses/public-key 提供）。 */
    public LicenseVerifier(String publicKeyBase64) {
        try {
            byte[] der = Base64.getDecoder().decode(publicKeyBase64.trim());
            this.publicKey = KeyFactory.getInstance("Ed25519")
                    .generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new LicenseException("公钥加载失败", e);
        }
    }

    /**
     * 验签并解析 .lic（格式：base64url(payload).base64url(signature)）。
     * @throws LicenseException 签名无效或格式错误（即“伪造/被篡改”）。
     */
    public LicenseClaims verify(String lic) {
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
            Signature s = Signature.getInstance("Ed25519");
            s.initVerify(publicKey);
            s.update(payload);
            if (!s.verify(sig)) {
                throw new LicenseException("签名无效：License 可能被伪造或篡改");
            }
        } catch (LicenseException le) {
            throw le;
        } catch (Exception e) {
            throw new LicenseException("验签失败", e);
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = MAPPER.readValue(new String(payload, StandardCharsets.UTF_8), Map.class);
            return new LicenseClaims(claims);
        } catch (Exception e) {
            throw new LicenseException("claims 解析失败", e);
        }
    }
}
