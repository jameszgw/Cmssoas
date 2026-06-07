package com.cmssoas.platform.billing.service;

import com.cmssoas.platform.billing.domain.Payment;

/**
 * 支付渠道薄抽象(provider-agnostic)。默认实现 {@link MockPaymentProvider}(沙箱,离线可跑全闭环);
 * 接微信支付/支付宝/Stripe 时新增实现并配置 {@code app.pay.provider} 即可,业务层无感知。
 */
public interface PaymentProvider {

    /** 渠道标识,如 MOCK / WECHATPAY / ALIPAY / STRIPE。 */
    String channel();

    /** 是否为沙箱渠道(支持"模拟支付成功"以便联调/演示)。 */
    default boolean sandbox() { return false; }

    /** 下单:向渠道创建支付,回填二维码内容/收银台链接/渠道交易号。 */
    PrepayResult create(Payment p);

    /**
     * 校验并解析异步回调,返回业务支付单号(payment_no)表示"已支付";返回 null 表示验签失败/未支付。
     * 真实渠道在此做签名验证。
     */
    String verifyNotify(String rawBody, java.util.Map<String, String> headers);

    /** 下单结果。 */
    record PrepayResult(String qrContent, String payUrl, String providerTxnId) {}
}
