package com.example.turnos_service.shared.infrastructure.client.catalog.dto;

// Credenciales que se envían a catalogo-service para pedir el token de servicio
public record ServiceTokenRequest(String clientId, String clientSecret) {
}
