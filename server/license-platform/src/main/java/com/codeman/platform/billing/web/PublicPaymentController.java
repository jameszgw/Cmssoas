package com.codeman.platform.billing.web;

import com.codeman.platform.billing.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

/**
 * 支付渠道异步回调(公开,验签在 PaymentProvider 内)。路径不在 /api/** 下,JwtAuthFilter 自动放行。
 * 真实渠道(微信/支付宝/Stripe)将通知 POST 到 {@code /pub/payments/notify/{channel}}。
 */
@RestController
@RequestMapping("/pub/payments")
public class PublicPaymentController {

    private final PaymentService service;

    public PublicPaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/notify/{channel}")
    public String notify(@PathVariable String channel, @RequestBody(required = false) String body,
                         HttpServletRequest req) {
        boolean ok = service.handleNotify(channel, body, headers(req));
        // 多数渠道要求返回特定成功串;沙箱返回 OK/FAIL
        return ok ? "SUCCESS" : "FAIL";
    }

    private java.util.Map<String, String> headers(HttpServletRequest req) {
        var m = new java.util.HashMap<String, String>();
        for (String n : Collections.list(req.getHeaderNames())) m.put(n.toLowerCase(), req.getHeader(n));
        return m;
    }
}
