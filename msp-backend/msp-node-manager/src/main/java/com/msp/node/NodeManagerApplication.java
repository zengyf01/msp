package com.msp.node;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 节点管理服务启动类
 */
@SpringBootApplication
@EnableScheduling
public class NodeManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(NodeManagerApplication.class, args);
    }
}