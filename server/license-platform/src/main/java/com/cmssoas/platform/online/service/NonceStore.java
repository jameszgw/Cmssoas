package com.cmssoas.platform.online.service;

import com.cmssoas.platform.config.AppProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 心跳 nonce 防重放存储。
 * 演示态：内存 + TTL；生产应替换为 Redis（SET key NX EX ttl），以支持多实例与水平扩展。
 */
@Component
public class NonceStore {

    private final Map<String, Long> seen = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public NonceStore(AppProperties props) {
        this.ttlMillis = props.getOnline().getNonceTtlSec() * 1000L;
    }

    /** 首次出现返回 true 并登记；重复（窗口内）返回 false。 */
    public boolean register(String key) {
        long now = System.currentTimeMillis();
        cleanup(now);
        Long prev = seen.putIfAbsent(key, now + ttlMillis);
        return prev == null;
    }

    private void cleanup(long now) {
        if (seen.size() > 10_000) {
            seen.entrySet().removeIf(e -> e.getValue() < now);
        }
    }
}
