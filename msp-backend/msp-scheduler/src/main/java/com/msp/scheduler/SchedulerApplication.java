package com.msp.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 调度服务启动类
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.msp.kuscia", "com.msp.scheduler"})
public class SchedulerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }
}