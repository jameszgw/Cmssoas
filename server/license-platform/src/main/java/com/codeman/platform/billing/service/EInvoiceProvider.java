package com.codeman.platform.billing.service;

import com.codeman.platform.billing.domain.TaxInvoice;

/**
 * 电子发票渠道薄抽象(provider-agnostic)。默认 {@link MockEInvoiceProvider}(沙箱);
 * 接航信/百望/税务开放平台时新增实现并配置 {@code app.einvoice.provider} 即可,业务层无感知。
 */
public interface EInvoiceProvider {

    /** 渠道标识,如 MOCK / AISINO / BAIWANG。 */
    String provider();

    /** 开票:向渠道申请开具,返回发票代码/号码/PDF。 */
    IssueResult issue(TaxInvoice ti);

    /** 开票结果。 */
    record IssueResult(String invoiceCode, String invoiceSerial, String pdfUrl) {}
}
