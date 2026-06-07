package com.codeman.platform.harden.runtime;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.util.Arrays;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * 运行期解密类加载器(随加固产物注入,纯 JDK,无外部依赖)。
 * 加固时核心类被 AES-GCM 加密为 {@code enc/<二进制名>/...class.enc};本加载器在 findClass 时
 * 读取密文、用运行期派生的密钥解密后 defineClass。未加密的依赖类仍由父加载器正常加载。
 */
public final class HardenClassLoader extends ClassLoader {

    private final JarFile jar;
    private final byte[] key;

    public HardenClassLoader(JarFile jar, byte[] key, ClassLoader parent) {
        super(parent);
        this.jar = jar;
        this.key = key;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String enc = "enc/" + name.replace('.', '/') + ".class.enc";
        ZipEntry e = jar.getEntry(enc);
        if (e == null) throw new ClassNotFoundException(name);
        try (InputStream in = jar.getInputStream(e)) {
            byte[] blob = in.readAllBytes();
            byte[] cls = decrypt(blob);
            return defineClass(name, cls, 0, cls.length);
        } catch (ClassNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ClassNotFoundException("解密类失败:" + name, ex);
        }
    }

    private byte[] decrypt(byte[] blob) throws Exception {
        byte[] iv = Arrays.copyOfRange(blob, 0, 12);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        return c.doFinal(blob, 12, blob.length - 12);
    }
}
