package com.example.turnos_service.shared.infrastructure.client.catalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Datos de conexión a catalogo-service, leídos de application.properties
@ConfigurationProperties(prefix = "catalogo")
public record CatalogConfiguration(String url, String clientId, String clientSecret) {
}
