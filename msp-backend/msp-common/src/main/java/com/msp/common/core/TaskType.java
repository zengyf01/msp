package com.msp.common.core;

/**
 * 任务类型枚举
 */
public enum TaskType {
    PSI,              // 隐私集合求交
    MPC,              // 安全多方计算
    FEDERATED_LEARNING, // 联邦学习
    CUSTOM_CODE,      // 自定义代码
    VERTICAL_FL,      // 纵向联邦学习
    COMPOUND_TASK,    // 复合任务
    COMPONENT_DAG     // 组件DAG
}