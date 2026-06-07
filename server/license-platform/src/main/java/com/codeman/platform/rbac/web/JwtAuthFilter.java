package com.codeman.platform.rbac.web;

import com.codeman.platform.rbac.service.CurrentUser;
import com.codeman.platform.rbac.service.JwtService;
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
        // 放行：非 /api 资源、登录、租户管理员激活（无账号场景，必须公开）、预检请求
        if (!path.startsWith("/api/")
                || path.equals("/api/auth/login")
                || path.startsWith("/api/activation/")
                || "OPTIONS".equalsIgnoreCase(req.getMethod())) {
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
