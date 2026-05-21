package com.msp.kuscia.client;

import com.msp.common.core.TaskRequest;
import com.msp.common.core.TaskStatus;

/**
 * Kuscia API客户端接口
 */
public interface KusciaClient {

    /**
     * 提交SecretFlow任务
     * @param taskId 任务ID
     * @param request 任务请求
     * @return 提交是否成功
     */
    boolean submitTask(String taskId, TaskRequest request);

    /**
     * 查询任务状态
     * @param taskId 任务ID
     * @return 任务状态
     */
    TaskStatus getTaskStatus(String taskId);

    /**
     * 取消任务
     * @param taskId 任务ID
     * @return 是否成功
     */
    boolean cancelTask(String taskId);

    /**
     * 获取任务结果
     * @param taskId 任务ID
     * @return 任务结果
     */
    byte[] getTaskResult(String taskId);
}