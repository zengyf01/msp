package com.msp.scheduler.service;

import com.msp.common.core.*;

import java.util.List;

/**
 * 任务调度核心接口
 */
public interface TaskScheduler {

    /**
     * 提交任务
     * @param request 任务请求
     * @return 任务ID
     */
    String submitTask(TaskRequest request);

    /**
     * 查询任务状态
     * @param taskId 任务ID
     * @return 任务状态
     */
    TaskStatus queryStatus(String taskId);

    /**
     * 取消任务
     * @param taskId 任务ID
     * @return 是否成功
     */
    boolean cancelTask(String taskId);

    /**
     * 删除任务
     * @param taskId 任务ID
     */
    void deleteTask(String taskId);

    /**
     * 重试任务（创建新任务）
     * @param taskId 原任务ID
     * @return 新任务ID
     */
    String retryTask(String taskId);

    /**
     * 获取任务详情
     * @param taskId 任务ID
     * @return 任务详情
     */
    Task getTask(String taskId);

    /**
     * 列出任务（分页）
     * @param statusFilter 状态过滤
     * @param typeFilter 类型过滤
     * @param page 页码
     * @param size 每页大小
     * @return 分页任务列表
     */
    Page<Task> listTasks(TaskStatus statusFilter, TaskType typeFilter, int page, int size);
}