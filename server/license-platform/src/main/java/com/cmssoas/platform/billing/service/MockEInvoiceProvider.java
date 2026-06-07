package com.cmssoas.platform.billing.service;

import com.cmssoas.platform.billing.domain.TaxInvoice;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 沙箱电子发票渠道(默认):不依赖外部税控,生成合规格式的发票代码/号码与 PDF 占位链接,
 * 用于联调/演示开票闭环。生产替换为航信/百望/税务开放平台实现即可(改 app.einvoice.provider)。
 */
@Service
public class MockEInvoiceProvider implements EInvoiceProvider {

    private static final SecureRandom RND = new SecureRandom();

    @Override
    public String provider() { return "MOCK"; }

    @Override
    public IssueResult issue(TaxInvoice ti) {
        // 发票代码 12 位(地区+年份+批次的简化模拟),发票号码 8 位流水
        String code = "0" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + String.format("%04d", RND.nextInt(10000));
        String serial = String.format("%08d", RND.nextInt(100_000_000));
        String pdf = "/einvoice/" + code + "-" + serial + ".pdf";
        return new IssueResult(code, serial, pdf);
    }
}
