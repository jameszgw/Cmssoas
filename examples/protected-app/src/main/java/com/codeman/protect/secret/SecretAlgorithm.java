package com.codeman.protect.secret;

/**
 * 受保护的“核心知识产权”。其字节码在交付物中被加密，
 * 仅在持有合法 License 时才能被解密加载（解密密钥由 License 派生）。
 */
public class SecretAlgorithm {

    public String compute(int input) {
        // 假装这是不希望被逆向的专有算法
        long acc = 1469598103934665603L;
        for (int i = 1; i <= input; i++) {
            acc ^= (long) i * 31 + 0x9E3779B97F4A7C15L;
            acc *= 1099511628211L;
        }
        return "SECRET-RESULT[" + input + "]=" + Long.toHexString(acc);
    }
}
