package com.example.turnos_service.reservation.application.exception;

public class InvalidPhoneNumberException extends RuntimeException {

    public InvalidPhoneNumberException() {
        super("El teléfono no tiene un formato válido");
    }
}
