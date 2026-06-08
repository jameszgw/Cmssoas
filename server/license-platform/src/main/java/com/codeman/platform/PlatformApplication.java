package com.codeman.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

// 仅用 Redis 做缓存/nonce，不用其 Repository，排除其仓库自动扫描以免误判 JPA 仓库
@SpringBootApplication(exclude = RedisRepositoriesAutoConfiguration.class)
@EnableScheduling
public class PlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }
}
