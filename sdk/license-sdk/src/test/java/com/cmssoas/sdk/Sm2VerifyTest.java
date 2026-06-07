package com.cmssoas.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 端到端：服务端用 SM2 签发，SDK 用内置公钥按 sigAlg=SM2 验签。 */
class Sm2VerifyTest {

    private static final ObjectMapper M = new ObjectMapper();
    private static final Base64.Encoder B64U = Base64.getUrlEncoder().withoutPadding();

    static { Security.addProvider(new BouncyCastleProvider()); }

    private KeyPair sm2KeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
        kpg.initialize(new ECGenParameterSpec("sm2p256v1"));
        return kpg.generateKeyPair();
    }

    private Map<String, Object> claims() {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("licenseId", "LIC-SM2-1");
        c.put("status", "ACTIVE");
        c.put("modules", List.of("RISK"));
        c.put("features", Map.of("REPORT.EXPORT", true));
        c.put("appVersionRange", ">=2.0.0 <3.0.0");
        c.put("notBefore", LocalDate.now().minusDays(1).toString());
        c.put("notAfter", LocalDate.now().plusDays(30).toString());
        c.put("sigAlg", "SM2");
        return c;
    }

    private String makeLic(Map<String, Object> claims, PrivateKey priv) throws Exception {
        byte[] payload = M.writeValueAsString(claims).getBytes(StandardCharsets.UTF_8);
        Signature s = Signature.getInstance("SM3withSM2", "BC");
        s.initSign(priv);
        s.update(payload);
        return B64U.encodeToString(payload) + "." + B64U.encodeToString(s.sign());
    }

    @Test
    void verifySm2Signed() throws Exception {
        KeyPair kp = sm2KeyPair();
        String pub = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
        String lic = makeLic(claims(), kp.getPrivate());

        LicenseClaims c = new LicenseVerifier(pub).verify(lic);
        assertEquals("LIC-SM2-1", c.licenseId());
        assertTrue(c.isCurrentlyValid());
        assertTrue(c.hasFeature("REPORT.EXPORT"));
    }

    @Test
    void tamperedSm2Rejected() throws Exception {
        KeyPair kp = sm2KeyPair();
        String pub = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
        String lic = makeLic(claims(), kp.getPrivate());
        String tampered = lic.substring(0, lic.indexOf('.')) + "." + B64U.encodeToString("bad".getBytes());
        assertThrows(LicenseException.class, () -> new LicenseVerifier(pub).verify(tampered));
    }
}
