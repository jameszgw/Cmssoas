package com.codeman.platform.license.service;

import java.security.MessageDigest;

/** 签名密钥工具：由公钥派生稳定的 kid（前 16 位十六进制）。 */
public final class SignatureKeys {
    private SignatureKeys() {}

    public static String kidOf(byte[] publicKeyEncoded) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(publicKeyEncoded);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) sb.append(String.format("%02x", h[i]));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
