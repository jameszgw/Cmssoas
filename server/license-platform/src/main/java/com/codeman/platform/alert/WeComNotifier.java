package com.codeman.platform.alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 企业微信群机器人告警：把到期/异常告警推送到 webhook（markdown 消息）。
 * 配置 app.alert.wecom-webhook 为机器人地址；留空则禁用（仅日志）。
 */
@Component
public class WeComNotifier {

    private static final Logger log = LoggerFactory.getLogger(WeComNotifier.class);

    private final String webhook;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public WeComNotifier(@Value("${app.alert.wecom-webhook:}") String webhook) {
        this.webhook = webhook;
    }

    public boolean enabled() {
        return webhook != null && !webhook.isBlank();
    }

    /** 发送 markdown 告警；失败仅记录日志，不影响主流程。 */
    public void sendMarkdown(String markdown) {
        if (!enabled()) {
            log.info("[wecom] 未配置 webhook，跳过推送：{}", markdown.replaceAll("\\s+", " "));
            return;
        }
        String body = "{\"msgtype\":\"markdown\",\"markdown\":{\"content\":" + jsonString(markdown) + "}}";
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(webhook))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 == 2) log.info("[wecom] 告警已推送（{}）", resp.statusCode());
            else log.warn("[wecom] 推送失败 status={} body={}", resp.statusCode(), resp.body());
        } catch (Exception e) {
            log.warn("[wecom] 推送异常：{}", e.getMessage());
        }
    }

    private static String jsonString(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> b.append(c);
            }
        }
        return b.append("\"").toString();
    }
}
