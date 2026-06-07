package com.codeman.platform.license;

import com.codeman.platform.license.service.Sm2SignatureService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** 国密 SM2 签名服务：签名 / 验签 / 防篡改 单元测试。 */
class Sm2SignatureServiceTest {

    private Sm2SignatureService newService() throws Exception {
        Path tmp = Files.createTempDirectory("sm2").resolve("sm2.json");
        Sm2SignatureService svc = new Sm2SignatureService(new ObjectMapper(), tmp.toString());
        // 触发 @PostConstruct 的 init()（包级可见）
        Method init = Sm2SignatureService.class.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(svc);
        return svc;
    }

    @Test
    void signAndVerify() throws Exception {
        Sm2SignatureService svc = newService();
        assertEquals("SM2", svc.algorithm());
        byte[] data = "cmssoas-license-payload-国密".getBytes(StandardCharsets.UTF_8);
        byte[] sig = svc.sign(data);
        assertTrue(svc.verify(data, sig), "合法签名应验签通过");
        assertNotNull(svc.publicKeyBase64());
    }

    @Test
    void tamperedRejected() throws Exception {
        Sm2SignatureService svc = newService();
        byte[] data = "original".getBytes(StandardCharsets.UTF_8);
        byte[] sig = svc.sign(data);
        assertFalse(svc.verify("tampered".getBytes(StandardCharsets.UTF_8), sig), "被篡改数据应验签失败");
    }
}
