package com.cmssoas.platform.online.service;

import com.cmssoas.platform.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 生产：Redis SET NX EX，支持多实例水平扩展。 */
@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class RedisNonceGuard implements NonceGuard {

    private final StringRedisTemplate redis;
    private final Duration ttl;

    public RedisNonceGuard(StringRedisTemplate redis, AppProperties props) {
        this.redis = redis;
        this.ttl = Duration.ofSeconds(props.getOnline().getNonceTtlSec());
    }

    @Override
    public boolean register(String key) {
        Boolean ok = redis.opsForValue().setIfAbsent("nonce:" + key, "1", ttl);
        return Boolean.TRUE.equals(ok);
    }
}
