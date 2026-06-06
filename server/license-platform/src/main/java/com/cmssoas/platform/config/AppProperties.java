package com.cmssoas.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Mail mail = new Mail();
    private final Activation activation = new Activation();

    public Mail getMail() { return mail; }
    public Activation getActivation() { return activation; }

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
}
