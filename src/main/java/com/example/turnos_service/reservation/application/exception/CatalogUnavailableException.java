package com.example.turnos_service.reservation.application.exception;

/**
 * No se pudo obtener información vigente de catalogo-service (sin conexión, error del servidor
 * o token de servicio rechazado). Sin esa información no se inicia la operación (enunciado §8).
 */
public class CatalogUnavailableException extends RuntimeException {

    public CatalogUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
