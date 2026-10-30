package com.example.turnos_service.shared.infrastructure.client.catedra.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(CatedraConfiguration.class)
public class CatedraWebClientConfiguration {

    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    public WebClient catedraWebClient(CatedraConfiguration cfg) {
        HttpClient httpClient = HttpClient.create().responseTimeout(RESPONSE_TIMEOUT);
        return WebClient.builder()
                .baseUrl(cfg.url())
                .defaultHeaders(headers -> headers.setBearerAuth(cfg.jwt()))
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
