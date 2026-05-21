package com.msp.kuscia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Kuscia WebClient配置
 */
@Configuration
public class KusciaWebClientConfig {

    @Value("${kuscia.master.url:http://kuscia-master:8083}")
    private String kusciaMasterUrl;

    @Value("${kuscia.master.token:}")
    private String kusciaToken;

    @Bean
    public WebClient kusciaWebClient() {
        WebClient.Builder builder = WebClient.builder()
            .baseUrl(kusciaMasterUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        if (kusciaToken != null && !kusciaToken.isEmpty()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + kusciaToken);
        }

        return builder.build();
    }
}