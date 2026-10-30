package com.example.turnos_service.shared.infrastructure.client.catedra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Datos de la cuenta técnica del proyecto ante la cátedra.
 * {@code groupId} identifica la integración y se envía en AdditionalInformationSubmitted (anexo §15.5).
 */
@ConfigurationProperties(prefix = "catedra")
public record CatedraConfiguration(String url, String jwt, String groupId) {
}
