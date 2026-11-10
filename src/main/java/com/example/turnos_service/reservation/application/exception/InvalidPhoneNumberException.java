package com.example.turnos_service.reservation.application.exception;

// El teléfono no tiene un formato válido
public class InvalidPhoneNumberException extends RuntimeException {

    public InvalidPhoneNumberException() {
        super("El teléfono no tiene un formato válido");
    }
}
