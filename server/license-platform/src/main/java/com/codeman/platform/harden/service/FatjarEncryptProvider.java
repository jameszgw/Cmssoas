package com.codeman.platform.harden.service;

import com.codeman.platform.common.ApiException;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/** 关键类 AES 加密 + 口令(胖包/类加密):运行期需提供口令 -Dharden.key 解密。 */
@Component
public class FatjarEncryptProvider implements HardenProvider {

    @Override
    public HardenTechnique technique() { return HardenTechnique.FATJAR_ENCRYPT; }

    @Override
    public boolean available() { return true; }

    @Override
    public String apply(Path in, Path out, HardenContext ctx) throws Exception {
        if (ctx.passphrase == null || ctx.passphrase.isBlank())
            throw ApiException.badRequest("FATJAR_ENCRYPT 需提供口令(passphrase)");
        byte[] key = HardenCrypto.deriveKey(ctx.passphrase);
        return EncryptCore.encrypt(in, out, key, "PASSPHRASE", ctx) + "(运行期 -Dharden.key 指定该口令)";
    }
}
