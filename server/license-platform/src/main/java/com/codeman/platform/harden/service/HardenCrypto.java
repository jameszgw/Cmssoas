package com.codeman.platform.harden.service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * 加固加密工具:AES-256-GCM(12 字节随机 IV 前置),密钥由密钥材料 SHA-256 派生。
 * 与运行期 {@code HardenLauncher/HardenClassLoader} 的解密实现严格一致。
 */
public final class HardenCrypto {
    private HardenCrypto() {}

    private static final SecureRandom RND = new SecureRandom();

    /** 由材料(License .lic 原文 或 口令)派生 32 字节 AES 密钥。 */
    public static byte[] deriveKey(String material) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(material.trim().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("派生密钥失败", e);
        }
    }

    /** 加密:返回 [12B IV][GCM 密文+tag]。 */
    public static byte[] encrypt(byte[] plain, byte[] key) {
        try {
            byte[] iv = new byte[12];
            RND.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] ct = c.doFinal(plain);
            byte[] out = new byte[12 + ct.length];
            System.arraycopy(iv, 0, out, 0, 12);
            System.arraycopy(ct, 0, out, 12, ct.length);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("加密失败", e);
        }
    }
}
