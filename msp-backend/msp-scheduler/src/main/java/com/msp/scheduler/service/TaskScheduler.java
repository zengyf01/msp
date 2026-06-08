package com.msp.scheduler.service;

import com.msp.common.core.*;

import java.util.List;

/**
 * 任务调度核心接口
 */
public interface TaskScheduler {

    /**
     * 提交任务（保存并立即执行）
     * @param request 任务请求
     * @return 任务ID
     */
    String submitTask(TaskRequest request);

    /**
     * 保存DAG（只保存，不执行）
     * @param request 任务请求
     * @return 任务ID
     */
    String saveDag(TaskRequest request);

    /**
     * 更新已保存的任务（仅允许 CREATED 状态）
     * @param taskId 任务ID
     * @param request 任务请求
     * @return 是否成功
     */
    boolean updateTask(String taskId, TaskRequest request);

    /**
     * 执行已保存的任务
     * @param taskId 任务ID
     */
    void executeTask(String taskId);

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
     * 复制任务（基于原任务定义创建新任务，状态为 CREATED）
     * @param taskId 原任务ID
     * @param newName 新任务名称（可选，为空则自动添加" (副本)"后缀）
     * @return 新任务ID
     */
    String copyTask(String taskId, String newName);

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