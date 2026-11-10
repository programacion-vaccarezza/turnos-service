package com.example.turnos_service.reservation.application.exception;

// No se pudo completar la operación con la cátedra por un problema de comunicación
public class CatedraUnavailableException extends RuntimeException {

    public CatedraUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
