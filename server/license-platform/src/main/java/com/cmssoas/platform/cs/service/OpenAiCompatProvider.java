package com.cmssoas.platform.cs.service;

import com.cmssoas.platform.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/**
 * 通用 OpenAI 兼容对话实现：纯 JDK HttpClient，零厂商 SDK、零额外依赖。
 *
 * <p>调用 {@code POST {base-url}/chat/completions}，开启 {@code stream:true}，
 * 逐行读取上游 SSE(每行 {@code data: {...}})，取 {@code choices[0].delta.content} 增量转发。
 * 兼容 OpenAI / DeepSeek / 通义 / 文心 / Kimi / 智谱 / one-api 网关 / 本地 Ollama。
 * 换厂商只改 {@code app.ai.base-url/api-key/model} 三项配置。
 */
@Service
public class OpenAiCompatProvider implements ChatProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatProvider.class);

    private final AppProperties props;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    public OpenAiCompatProvider(AppProperties props) {
        this.props = props;
    }

    @Override
    public boolean ready() {
        return props.getAi().ready();
    }

    @Override
    public String stream(List<Msg> messages, Consumer<String> onToken) throws Exception {
        AppProperties.Ai ai = props.getAi();
        String url = ai.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";

        ObjectNode body = json.createObjectNode();
        body.put("model", ai.getModel());
        body.put("stream", true);
        ArrayNode arr = body.putArray("messages");
        for (Msg m : messages) {
            ObjectNode o = arr.addObject();
            o.put("role", m.role());
            o.put("content", m.content());
        }

        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(ai.getTimeoutSec()))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body), StandardCharsets.UTF_8));
        if (ai.getApiKey() != null && !ai.getApiKey().isBlank()) {
            req.header("Authorization", "Bearer " + ai.getApiKey());
        }

        HttpResponse<java.io.InputStream> resp = client.send(req.build(),
                HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() / 100 != 2) {
            String err = new String(resp.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new IllegalStateException("上游返回 " + resp.statusCode() + "：" + brief(err));
        }

        StringBuilder full = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty() || !line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) break;
                String delta = extractDelta(data);
                if (delta != null && !delta.isEmpty()) {
                    full.append(delta);
                    onToken.accept(delta);
                }
            }
        }
        return full.toString();
    }

    /** 从单帧 SSE JSON 中取 choices[0].delta.content(兼容个别厂商用 message.content 的情况)。 */
    private String extractDelta(String data) {
        try {
            JsonNode node = json.readTree(data);
            JsonNode choice = node.path("choices").path(0);
            JsonNode c = choice.path("delta").path("content");
            if (c.isMissingNode() || c.isNull()) c = choice.path("message").path("content");
            return c.isTextual() ? c.asText() : null;
        } catch (Exception e) {
            log.debug("跳过无法解析的 SSE 帧: {}", brief(data));
            return null;
        }
    }

    private String brief(String s) {
        s = s == null ? "" : s.replaceAll("\\s+", " ").trim();
        return s.length() > 200 ? s.substring(0, 200) + "…" : s;
    }
}
