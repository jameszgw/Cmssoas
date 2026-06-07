package com.codeman.platform.billing.service;

import com.codeman.platform.billing.domain.Payment;
import org.springframework.stereotype.Service;

/**
 * 沙箱支付渠道(默认):不依赖外部网络,生成可扫码内容与"收银台"链接,
 * 配合 {@code POST /api/payments/{id}/sandbox-confirm} 完成"模拟支付成功"——用于联调/演示全闭环。
 * 生产替换为微信支付/支付宝/Stripe 实现即可(改 {@code app.pay.provider})。
 */
@Service
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public String channel() { return "MOCK"; }

    @Override
    public boolean sandbox() { return true; }

    @Override
    public PrepayResult create(Payment p) {
        // 二维码原文(真实渠道为微信/支付宝返回的 code_url);此处用自有协议占位
        String qr = "CMSSOAS-PAY://sandbox?no=" + p.getPaymentNo() + "&amt=" + p.getAmount();
        String url = "/pay/" + p.getPaymentNo();
        return new PrepayResult(qr, url, "MOCK-" + p.getPaymentNo());
    }

    @Override
    public String verifyNotify(String rawBody, java.util.Map<String, String> headers) {
        // 沙箱:回调体即支付单号(真实渠道在此验签并解析金额/状态)
        return rawBody == null ? null : rawBody.trim();
    }
}
