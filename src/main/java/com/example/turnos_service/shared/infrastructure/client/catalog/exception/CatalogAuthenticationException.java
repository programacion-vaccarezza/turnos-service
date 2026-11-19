package com.example.turnos_service.shared.infrastructure.client.catalog.exception;

// Error al obtener el token de servicio de catalogo-service
public class CatalogAuthenticationException extends RuntimeException {
    public CatalogAuthenticationException(String message) {
        super(message);
    }
}
