package com.msp.common.core;

/**
 * 错误码定义
 */
public final class ErrorCode {
    public static final String SUCCESS = "200";
    public static final String BAD_REQUEST = "400";
    public static final String UNAUTHORIZED = "401";
    public static final String FORBIDDEN = "403";
    public static final String NOT_FOUND = "404";
    public static final String INTERNAL_ERROR = "500";
    public static final String SERVICE_UNAVAILABLE = "503";

    // 业务错误码
    public static final String TASK_NOT_FOUND = "TASK_001";
    public static final String TASK_RUNNING = "TASK_002";
    public static final String INVALID_TASK_STATUS = "TASK_003";
    public static final String NODE_NOT_FOUND = "NODE_001";
    public static final String NODE_OFFLINE = "NODE_002";
    public static final String DATA_SOURCE_ERROR = "DATA_001";
    public static final String ALGORITHM_ERROR = "ALGO_001";

    private ErrorCode() {}
}