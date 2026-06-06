package com.cmssoas.platform.rbac.web;

import com.cmssoas.platform.rbac.service.CurrentUser;
import com.cmssoas.platform.rbac.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 运营后台鉴权过滤器：保护 /api/**（放行 /api/auth/**、OPTIONS、SDK 通道与静态资源）。
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;

    public JwtAuthFilter(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if (!path.startsWith("/api/") || path.equals("/api/auth/login") || "OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(req, resp);
            return;
        }
        String h = req.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) {
            unauthorized(resp, "需要登录");
            return;
        }
        try {
            Claims c = jwt.parse(h.substring(7));
            Long uid = c.get("uid", Number.class).longValue();
            CurrentUser.set(new CurrentUser.Ctx(uid, c.getSubject(), c.get("role", String.class)));
            chain.doFilter(req, resp);
        } catch (Exception e) {
            unauthorized(resp, "登录已失效，请重新登录");
        } finally {
            CurrentUser.clear();
        }
    }

    private void unauthorized(HttpServletResponse resp, String msg) throws IOException {
        resp.setStatus(401);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"status\":401,\"message\":\"" + msg + "\"}");
    }
}
