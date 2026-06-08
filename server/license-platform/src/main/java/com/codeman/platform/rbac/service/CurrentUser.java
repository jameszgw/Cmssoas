package com.codeman.platform.rbac.service;

/** 当前登录用户上下文（由 JwtAuthFilter 在请求线程内设置）。 */
public final class CurrentUser {

    public record Ctx(Long uid, String username, String role) {}

    private static final ThreadLocal<Ctx> HOLDER = new ThreadLocal<>();

    public static void set(Ctx ctx) { HOLDER.set(ctx); }
    public static Ctx get() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }
    public static boolean isSuperAdmin() {
        Ctx c = HOLDER.get();
        return c != null && "SUPER_ADMIN".equals(c.role());
    }
}
