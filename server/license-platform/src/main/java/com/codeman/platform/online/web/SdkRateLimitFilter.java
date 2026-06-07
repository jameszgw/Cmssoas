package com.codeman.platform.online.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SDK 在线通道限流/防刷：按客户端 IP 的每分钟固定窗口计数，超限返回 429。
 * 演示态：单实例内存计数；多实例生产应换 Redis 计数（INCR + EXPIRE）。
 */
@Component
@Order(1)
public class SdkRateLimitFilter extends OncePerRequestFilter {

    private final int limitPerMin;
    private final Map<String, Window> buckets = new ConcurrentHashMap<>();

    public SdkRateLimitFilter(@Value("${app.sdk.rate-limit-per-min:120}") int limitPerMin) {
        this.limitPerMin = limitPerMin;
    }

    private static final class Window {
        volatile long minute;
        final AtomicInteger count = new AtomicInteger();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if (!path.startsWith("/sdk/")) {
            chain.doFilter(req, resp);
            return;
        }
        String key = clientIp(req);
        long nowMin = System.currentTimeMillis() / 60_000;
        Window w = buckets.computeIfAbsent(key, k -> new Window());
        synchronized (w) {
            if (w.minute != nowMin) { w.minute = nowMin; w.count.set(0); }
            if (w.count.incrementAndGet() > limitPerMin) {
                resp.setStatus(429);
                resp.setHeader("Retry-After", "60");
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().write("{\"status\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
                return;
            }
        }
        chain.doFilter(req, resp);
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
