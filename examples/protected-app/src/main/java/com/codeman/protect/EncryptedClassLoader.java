package com.codeman.protect;

/** 自定义类加载器：从内存中的（已解密）字节码定义业务类。 */
public class EncryptedClassLoader extends ClassLoader {

    public EncryptedClassLoader(ClassLoader parent) {
        super(parent);
    }

    public Class<?> defineFromBytes(String name, byte[] bytes) {
        return defineClass(name, bytes, 0, bytes.length);
    }
}
