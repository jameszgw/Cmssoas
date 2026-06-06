package com.cmssoas.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Mail mail = new Mail();
    private final Activation activation = new Activation();
    private final Online online = new Online();

    public Mail getMail() { return mail; }
    public Activation getActivation() { return activation; }
    public Online getOnline() { return online; }

    public static class Mail {
        /** log | smtp */
        private String delivery = "log";
        private String from = "no-reply@cmssoas.com";
        private String fromName = "CMSSOAS";
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
