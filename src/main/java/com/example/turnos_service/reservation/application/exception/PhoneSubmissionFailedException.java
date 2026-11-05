package com.example.turnos_service.reservation.application.exception;

/**
 * No se pudo publicar el teléfono en Kafka. El proceso no cambia de estado y el usuario puede reintentar.
 */
public class PhoneSubmissionFailedException extends RuntimeException {

    public PhoneSubmissionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
