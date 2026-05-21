package com.msp.gateway;

import com.msp.common.security.JwtService;
import com.msp.gateway.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * API网关启动类
 */
@SpringBootApplication
public class MspGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(MspGatewayApplication.class, args);
    }

    @Bean
    public JwtService jwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration:86400000}") long expiration) {
        return new JwtService(secret, expiration);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }
}