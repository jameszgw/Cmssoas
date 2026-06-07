package com.codeman.platform.online.service;

/** 心跳 nonce 防重放守卫。实现：内存（默认）或 Redis（多实例）。 */
public interface NonceGuard {
    /** 首次出现返回 true 并登记；窗口内重复返回 false。 */
    boolean register(String key);
}
