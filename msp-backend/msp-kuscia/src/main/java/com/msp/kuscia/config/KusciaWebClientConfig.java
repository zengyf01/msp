package com.msp.kuscia.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

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
        HttpClient httpClient = HttpClient.create()
            .secure(sslContextSpec -> {
                try {
                    SslContext sslContext = createSkipVerifySslContext();
                    sslContextSpec.sslContext(sslContext);
                } catch (Exception e) {
                    // 忽略
                }
            });

        WebClient.Builder builder = WebClient.builder()
            .baseUrl(kusciaMasterUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        if (kusciaToken != null && !kusciaToken.isEmpty()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + kusciaToken);
        }

        return builder.build();
    }

    private SslContext createSkipVerifySslContext() throws Exception {
        return SslContextBuilder.forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .build();
    }
}