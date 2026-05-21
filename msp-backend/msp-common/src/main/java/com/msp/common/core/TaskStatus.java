package com.msp.common.core;

/**
 * 任务状态枚举
 */
public enum TaskStatus {
    CREATED,   // 已创建
    PENDING,   // 待执行
    RUNNING,   // 执行中
    COMPLETED, // 已完成
    FAILED,    // 失败
    CANCELLED  // 已取消
}