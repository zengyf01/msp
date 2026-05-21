package com.msp.common.core;

/**
 * 异常基类
 */
public class MspException extends RuntimeException {
    private final String code;

    public MspException(String code, String message) {
        super(message);
        this.code = code;
    }

    public MspException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}