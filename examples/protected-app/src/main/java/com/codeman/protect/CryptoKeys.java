package com.codeman.protect;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * 解密密钥派生（KDF）：把“类解密密钥”绑定到具体 License。
 * key = SHA-256( 产品根密钥 | licenseId | watermark )
 * 因此：没有合法 License、或换一张 License，都派生不出正确密钥 → 无法解密业务字节码。
 * 演示态：根密钥硬编码；生产应托管 KMS/HSM 并配合代码混淆/Native 保护本类。
 */
public final class CryptoKeys {
    private CryptoKeys() {}

    private static final String PRODUCT_ROOT_SECRET = "CMSSOAS::root::v1::do-not-ship-in-plaintext";

    public static byte[] deriveKey(String licenseId, String watermark) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(PRODUCT_ROOT_SECRET.getBytes(StandardCharsets.UTF_8));
            md.update((byte) '|');
            md.update(String.valueOf(licenseId).getBytes(StandardCharsets.UTF_8));
            md.update((byte) '|');
            md.update(String.valueOf(watermark).getBytes(StandardCharsets.UTF_8));
            return Arrays.copyOf(md.digest(), 16); // AES-128
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static IvParameterSpec iv(String licenseId) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(licenseId.getBytes(StandardCharsets.UTF_8));
            return new IvParameterSpec(Arrays.copyOf(h, 16));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    public static byte[] encrypt(byte[] plain, byte[] key, String licenseId) throws Exception {
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), iv(licenseId));
        return c.doFinal(plain);
    }

    public static byte[] decrypt(byte[] enc, byte[] key, String licenseId) throws Exception {
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), iv(licenseId));
        return c.doFinal(enc);
    }
}
