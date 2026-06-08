package com.codeman.platform.online.service;

import com.codeman.platform.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 默认：内存 + TTL（单实例/开发）。 */
@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryNonceGuard implements NonceGuard {

    private final Map<String, Long> seen = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public InMemoryNonceGuard(AppProperties props) {
        this.ttlMillis = props.getOnline().getNonceTtlSec() * 1000L;
    }

    @Override
    public boolean register(String key) {
        long now = System.currentTimeMillis();
        if (seen.size() > 10_000) seen.entrySet().removeIf(e -> e.getValue() < now);
        return seen.putIfAbsent(key, now + ttlMillis) == null;
    }
}
