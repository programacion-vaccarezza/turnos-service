package com.example.turnos_service.reservation.application.exception;

// No se pudo publicar el teléfono en Kafka
public class PhoneSubmissionFailedException extends RuntimeException {

    public PhoneSubmissionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
