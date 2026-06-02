package com.msp.common.core;

/**
 * 设备类型枚举
 */
public enum DeviceType {
    PYU,   // Python计算单元
    SPU,   // 安全处理单元 (MPC)
    HEU,   // 同态加密单元
    TEEU,  // 可信执行环境单元
    PSI,   // 隐私集合求交
    MPC,   // 安全多方计算
    FEDERATED_LEARNING  // 联邦学习
}