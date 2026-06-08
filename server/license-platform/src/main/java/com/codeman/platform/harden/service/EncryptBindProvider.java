package com.codeman.platform.harden.service;

import com.codeman.platform.common.ApiException;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/** 关键类 AES 加密 + 与 License 绑定:运行期需提供绑定的 .lic 才能解密运行。 */
@Component
public class EncryptBindProvider implements HardenProvider {

    @Override
    public HardenTechnique technique() { return HardenTechnique.ENCRYPT_BIND; }

    @Override
    public boolean available() { return true; }

    @Override
    public String apply(Path in, Path out, HardenContext ctx) throws Exception {
        if (ctx.licenseLic == null || ctx.licenseLic.isBlank())
            throw ApiException.badRequest("ENCRYPT_BIND 需指定绑定的 License(bindLicense)");
        byte[] key = HardenCrypto.deriveKey(ctx.licenseLic);
        return EncryptCore.encrypt(in, out, key, "LICENSE_BIND", ctx) + "(运行期 -Dharden.license 指定该 .lic)";
    }
}
