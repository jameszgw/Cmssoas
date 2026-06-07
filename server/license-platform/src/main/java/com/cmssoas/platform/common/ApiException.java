package com.cmssoas.platform.common;

import org.springframework.http.HttpStatus;

/** 业务异常：携带 HTTP 状态码与可读消息。 */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static ApiException badRequest(String msg) {
        return new ApiException(HttpStatus.BAD_REQUEST, msg);
    }

    public static ApiException notFound(String msg) {
        return new ApiException(HttpStatus.NOT_FOUND, msg);
    }

    public static ApiException gone(String msg) {
        return new ApiException(HttpStatus.GONE, msg);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
