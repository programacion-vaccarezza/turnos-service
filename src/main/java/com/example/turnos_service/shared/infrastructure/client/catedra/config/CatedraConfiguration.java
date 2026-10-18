package com.example.turnos_service.shared.infrastructure.client.catedra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "catedra")
public record CatedraConfiguration(String url, String jwt) {
}
