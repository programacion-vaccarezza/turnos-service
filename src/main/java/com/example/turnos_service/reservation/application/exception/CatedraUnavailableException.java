package com.example.turnos_service.reservation.application.exception;

/**
 * No se pudo completar la operación con la cátedra por un problema de comunicación
 * (sin conexión, timeout o error 5xx). No prueba que la operación haya fallado (anexo §18.4).
 */
public class CatedraUnavailableException extends RuntimeException {

    public CatedraUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
