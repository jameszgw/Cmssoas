package com.cmssoas.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LicenseVerifierTest {

    private static final ObjectMapper M = new ObjectMapper();
    private static final Base64.Encoder B64U = Base64.getUrlEncoder().withoutPadding();

    private KeyPair kp;

    private String makeLic(Map<String, Object> claims, KeyPair signer) throws Exception {
        byte[] payload = M.writeValueAsString(claims).getBytes(StandardCharsets.UTF_8);
        Signature s = Signature.getInstance("Ed25519");
        s.initSign(signer.getPrivate());
        s.update(payload);
        return B64U.encodeToString(payload) + "." + B64U.encodeToString(s.sign());
    }

    private Map<String, Object> claims() {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("licenseId", "LIC-T-1");
        c.put("licenseVersion", 1);
        c.put("customer", "测试");
        c.put("status", "ACTIVE");
        c.put("modules", List.of("RISK", "REPORT"));
        c.put("features", Map.of("REPORT.EXPORT", true, "MAX_USERS", 100));
        c.put("appVersionRange", ">=2.0.0 <3.0.0");
        c.put("notBefore", LocalDate.now().minusDays(1).toString());
        c.put("notAfter", LocalDate.now().plusDays(30).toString());
        c.put("watermark", "WM-1");
        return c;
    }

    private String pub(KeyPair kp) {
        return Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
    }

    LicenseVerifierTest() throws Exception {
        kp = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
    }

    @Test
    void verifyValidLicense() throws Exception {
        String lic = makeLic(claims(), kp);
        LicenseClaims c = new LicenseVerifier(pub(kp)).verify(lic);
        assertEquals("LIC-T-1", c.licenseId());
        assertTrue(c.isCurrentlyValid());
        assertTrue(c.hasModule("RISK"));
        assertTrue(c.hasFeature("REPORT.EXPORT"));
        assertEquals(100, c.quota("MAX_USERS"));
        assertTrue(c.appVersionAllowed("2.4.0"));
        assertFalse(c.appVersionAllowed("3.1.0"));
    }

    @Test
    void tamperedLicenseRejected() throws Exception {
        String lic = makeLic(claims(), kp);
        String tampered = "Z" + lic.substring(1);
        assertThrows(LicenseException.class, () -> new LicenseVerifier(pub(kp)).verify(tampered));
    }

    @Test
    void wrongKeyRejected() throws Exception {
        String lic = makeLic(claims(), kp);
        KeyPair other = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        assertThrows(LicenseException.class, () -> new LicenseVerifier(pub(other)).verify(lic));
    }

    @Test
    void expiredLicenseNotValid() throws Exception {
        Map<String, Object> c = claims();
        c.put("notAfter", LocalDate.now().minusDays(1).toString());
        LicenseClaims parsed = new LicenseVerifier(pub(kp)).verify(makeLic(c, kp));
        assertFalse(parsed.isCurrentlyValid());
    }
}
