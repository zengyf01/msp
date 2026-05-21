package com.msp.scheduler.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 任务指标服务
 * 提供任务相关的Prometheus指标
 */
@Service
public class TaskMetricsService {

    private final Counter taskSubmittedCounter;
    private final Counter taskCompletedCounter;
    private final Counter taskFailedCounter;
    private final Counter taskCancelledCounter;
    private final Timer taskDurationTimer;
    private final AtomicLong runningTasksGauge;

    public TaskMetricsService(MeterRegistry registry) {
        // 任务提交计数
        this.taskSubmittedCounter = Counter.builder("msp_task_submitted_total")
            .description("Total number of tasks submitted")
            .tag("type", "all")
            .register(registry);

        // 任务完成计数
        this.taskCompletedCounter = Counter.builder("msp_task_completed_total")
            .description("Total number of tasks completed")
            .tag("type", "all")
            .register(registry);

        // 任务失败计数
        this.taskFailedCounter = Counter.builder("msp_task_failed_total")
            .description("Total number of tasks failed")
            .tag("type", "all")
            .register(registry);

        // 任务取消计数
        this.taskCancelledCounter = Counter.builder("msp_task_cancelled_total")
            .description("Total number of tasks cancelled")
            .tag("type", "all")
            .register(registry);

        // 任务执行时长
        this.taskDurationTimer = Timer.builder("msp_task_duration_seconds")
            .description("Task execution duration")
            .register(registry);

        // 运行中任务数
        this.runningTasksGauge = new AtomicLong(0);
        Gauge.builder("msp_task_running", runningTasksGauge, AtomicLong::get)
            .description("Number of currently running tasks")
            .register(registry);
    }

    public void recordTaskSubmitted() {
        taskSubmittedCounter.increment();
    }

    public void recordTaskCompleted() {
        taskCompletedCounter.increment();
        runningTasksGauge.decrementAndGet();
    }

    public void recordTaskFailed() {
        taskFailedCounter.increment();
        runningTasksGauge.decrementAndGet();
    }

    public void recordTaskCancelled() {
        taskCancelledCounter.increment();
        runningTasksGauge.decrementAndGet();
    }

    public void recordTaskStarted() {
        runningTasksGauge.incrementAndGet();
    }

    public Timer.Sample startTimer() {
        return Timer.start();
    }

    public void stopTimer(Timer.Sample sample) {
        sample.stop(taskDurationTimer);
    }
}