package com.example.turnos_service.reservation.application.exception;

public class PatientNotFoundException extends RuntimeException {

    public PatientNotFoundException(String login) {
        super("No existe el usuario " + login + " en catalogo-service");
    }
}
