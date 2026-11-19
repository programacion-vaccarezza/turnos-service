package com.example.turnos_service.reservation.application.exception;

// No se pudo consultar catalogo-service
public class CatalogUnavailableException extends RuntimeException {

    public CatalogUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
