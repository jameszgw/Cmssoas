package com.codeman.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Mail mail = new Mail();
    private final Activation activation = new Activation();
    private final Online online = new Online();
    private final Ai ai = new Ai();
    private final Pay pay = new Pay();
    private final Harden harden = new Harden();

    public Mail getMail() { return mail; }
    public Activation getActivation() { return activation; }
    public Online getOnline() { return online; }
    public Ai getAi() { return ai; }
    public Pay getPay() { return pay; }
    public Harden getHarden() { return harden; }

    /** 在线代码加固(上传 jar→加固→下载)。与构建/打包加固并存。 */
    public static class Harden {
        /** 加固工作目录(存放上传源 jar 与产物)。 */
        private String workDir = "./var/harden";
        /** 单个上传 jar 上限(MB)。 */
        private int maxFileMb = 200;

        public String getWorkDir() { return workDir; }
        public void setWorkDir(String v) { this.workDir = v; }
        public int getMaxFileMb() { return maxFileMb; }
        public void setMaxFileMb(int v) { this.maxFileMb = v; }
    }

    /** 在线支付/收款(通用,不绑定渠道)。默认沙箱;换渠道改 {@code provider}。 */
    public static class Pay {
        /** 当前启用渠道:mock(沙箱) | wechatpay | alipay | stripe。 */
        private String provider = "mock";
        /** 异步回调可达的公网根地址(真实渠道用于回填 notify_url)。 */
        private String notifyBaseUrl = "";

        public String getProvider() { return provider; }
        public void setProvider(String v) { this.provider = v; }
        public String getNotifyBaseUrl() { return notifyBaseUrl; }
        public void setNotifyBaseUrl(String v) { this.notifyBaseUrl = v; }
    }

    private final EInvoice einvoice = new EInvoice();
    public EInvoice getEinvoice() { return einvoice; }

    /** 电子发票(通用,不绑定渠道)。默认沙箱;换渠道改 {@code provider}。 */
    public static class EInvoice {
        /** 当前启用渠道:mock(沙箱) | aisino(航信) | baiwang(百望) ...。 */
        private String provider = "mock";

        public String getProvider() { return provider; }
        public void setProvider(String v) { this.provider = v; }
    }

    /**
     * 智能客服大模型配置(通用、不绑定厂商)。统一走 OpenAI 兼容接口
     * {base-url}/chat/completions，换厂商=改配置不改代码：
     * 云端 DeepSeek / 通义 / 文心 / Kimi / 智谱 / OpenAI / one-api 网关，或本地 Ollama(完全离线)。
     */
    public static class Ai {
        /** 是否启用智能客服。未配置 base-url 时即便启用也会优雅降级为“仅知识库提示”。 */
        private boolean enabled = false;
        /** OpenAI 兼容端点根地址，例：https://api.deepseek.com/v1 或 http://127.0.0.1:11434/v1(Ollama)。 */
        private String baseUrl = "";
        /** API Key(经 Nacos/KMS/env 注入，不落明文)；本地模型可留空。 */
        private String apiKey = "";
        /** 模型名，例：deepseek-chat / qwen-plus / gpt-4o-mini / llama3.1。 */
        private String model = "deepseek-chat";
        /** 上游请求超时(秒)。 */
        private int timeoutSec = 60;
        /** 单会话回传给模型的最大历史消息条数(控制成本与上下文长度)。 */
        private int historyLimit = 12;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String v) { this.baseUrl = v; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String v) { this.apiKey = v; }
        public String getModel() { return model; }
        public void setModel(String v) { this.model = v; }
        public int getTimeoutSec() { return timeoutSec; }
        public void setTimeoutSec(int v) { this.timeoutSec = v; }
        public int getHistoryLimit() { return historyLimit; }
        public void setHistoryLimit(int v) { this.historyLimit = v; }
        /** 是否具备真正可调用的上游(用于决定走模型还是降级)。 */
        public boolean ready() { return enabled && baseUrl != null && !baseUrl.isBlank(); }
    }

    public static class Mail {
        /** log | smtp */
        private String delivery = "log";
        private String from = "no-reply@codeman.com";
        private String fromName = "CODEMAN";
        private String spoolDir = "./var/mail";

        public String getDelivery() { return delivery; }
        public void setDelivery(String delivery) { this.delivery = delivery; }
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public String getFromName() { return fromName; }
        public void setFromName(String fromName) { this.fromName = fromName; }
        public String getSpoolDir() { return spoolDir; }
        public void setSpoolDir(String spoolDir) { this.spoolDir = spoolDir; }
    }

    public static class Activation {
        private String baseUrl = "http://localhost:5173/activate";
        private int ttlHours = 24;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public int getTtlHours() { return ttlHours; }
        public void setTtlHours(int ttlHours) { this.ttlHours = ttlHours; }
    }

    /** 在线授权（SDK 通道）参数。 */
    public static class Online {
        private int heartbeatSec = 30;   // 心跳间隔
        private int graceSec = 120;      // 断网宽限期：超过则席位释放/降级
        private int nonceTtlSec = 300;   // nonce 防重放窗口

        public int getHeartbeatSec() { return heartbeatSec; }
        public void setHeartbeatSec(int v) { this.heartbeatSec = v; }
        public int getGraceSec() { return graceSec; }
        public void setGraceSec(int v) { this.graceSec = v; }
        public int getNonceTtlSec() { return nonceTtlSec; }
        public void setNonceTtlSec(int v) { this.nonceTtlSec = v; }
    }
}
