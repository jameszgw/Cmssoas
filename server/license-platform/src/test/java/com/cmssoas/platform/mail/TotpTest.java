package com.cmssoas.platform.mail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TotpTest {

    @Test
    void base32RoundTrip() {
        byte[] data = "hello-cmssoas-2026".getBytes();
        String enc = Totp.base32Encode(data);
        assertArrayEquals(data, Totp.base32Decode(enc));
    }

    @Test
    void otpauthUriContainsSecretAndIssuer() {
        Totp totp = new Totp();
        String secret = totp.generateSecret();
        String uri = totp.otpauthUri(secret, "admin@x.com");
        assertTrue(uri.startsWith("otpauth://totp/"));
        assertTrue(uri.contains("secret=" + secret));
        assertTrue(uri.contains("issuer=CMSSOAS"));
    }

    @Test
    void verifyRejectsBadCode() {
        Totp totp = new Totp();
        String secret = totp.generateSecret();
        assertFalse(totp.verify(secret, "000000"));
        assertFalse(totp.verify(secret, "abc"));
        assertFalse(totp.verify(secret, null));
    }
}
