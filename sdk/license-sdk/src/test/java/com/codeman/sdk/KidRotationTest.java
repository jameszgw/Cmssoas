package com.codeman.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 密钥轮换：多 kid 公钥集，按 License 的 kid 选对应公钥验签。 */
class KidRotationTest {

    private static final ObjectMapper M = new ObjectMapper();
    private static final Base64.Encoder B64U = Base64.getUrlEncoder().withoutPadding();

    private KeyPair ed() throws Exception {
        return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
    }

    private String pub(KeyPair kp) {
        return Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
    }

    private String makeLic(String kid, KeyPair signer) throws Exception {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("licenseId", "LIC-" + kid);
        c.put("status", "ACTIVE");
        c.put("sigAlg", "Ed25519");
        c.put("kid", kid);
        c.put("modules", java.util.List.of("RISK"));
        c.put("appVersionRange", ">=2.0.0 <3.0.0");
        c.put("notBefore", LocalDate.now().minusDays(1).toString());
        c.put("notAfter", LocalDate.now().plusDays(30).toString());
        byte[] payload = M.writeValueAsString(c).getBytes(StandardCharsets.UTF_8);
        Signature s = Signature.getInstance("Ed25519");
        s.initSign(signer.getPrivate());
        s.update(payload);
        return B64U.encodeToString(payload) + "." + B64U.encodeToString(s.sign());
    }

    @Test
    void verifiesByKidAcrossRotation() throws Exception {
        KeyPair kA = ed(), kB = ed();              // 旧密钥 A，新密钥 B（轮换）
        Map<String, String> jwks = Map.of("kA", pub(kA), "kB", pub(kB));

        // 用 A 签的旧 License 与用 B 签的新 License 都应通过
        LicenseClaims cA = LicenseVerifier.verify(makeLic("kA", kA), jwks);
        LicenseClaims cB = LicenseVerifier.verify(makeLic("kB", kB), jwks);
        assertEquals("kA", cA.kid());
        assertEquals("kB", cB.kid());
        assertTrue(cA.isCurrentlyValid());

        // kid 与密钥不匹配（用 A 的密钥冒充 kB）应验签失败
        assertThrows(LicenseException.class, () -> LicenseVerifier.verify(makeLic("kB", kA), jwks));
        // 未知 kid 应拒绝
        assertThrows(LicenseException.class, () -> LicenseVerifier.verify(makeLic("kX", kA), jwks));
    }
}
