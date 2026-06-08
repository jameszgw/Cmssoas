package com.codeman.platform.billing.web;

import com.codeman.platform.billing.domain.Payment;
import com.codeman.platform.billing.service.PaymentService;
import com.codeman.platform.rbac.service.RequirePerm;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 在线支付/收款接口(复用 billing 权限)。 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePerm("billing:view")
    public List<Payment> list() {
        return service.list();
    }

    /** 轮询支付状态用。 */
    @GetMapping("/{id}")
    @RequirePerm("billing:view")
    public Payment get(@PathVariable Long id) {
        return service.get(id);
    }

    /** 对待支付账单发起收款(返回二维码/收银台信息)。 */
    @PostMapping
    @RequirePerm("billing:manage")
    public Payment create(@RequestBody Map<String, Object> body) {
        Long invoiceId = Long.valueOf(String.valueOf(body.get("invoiceId")));
        return service.createForInvoice(invoiceId);
    }

    /** 沙箱"模拟支付成功"(仅沙箱渠道)。 */
    @PostMapping("/{id}/sandbox-confirm")
    @RequirePerm("billing:manage")
    public Payment sandboxConfirm(@PathVariable Long id) {
        return service.sandboxConfirm(id);
    }
}
