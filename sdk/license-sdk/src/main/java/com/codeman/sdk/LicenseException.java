package com.codeman.sdk;

/** License 校验失败异常（验签失败 / 过期 / 格式错误等）。 */
public class LicenseException extends RuntimeException {
    public LicenseException(String message) { super(message); }
    public LicenseException(String message, Throwable cause) { super(message, cause); }
}
