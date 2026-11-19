package com.example.turnos_service.reservation.application.exception;

// El usuario no existe en catalogo-service
public class PatientNotFoundException extends RuntimeException {

    public PatientNotFoundException(String login) {
        super("No existe el usuario " + login + " en catalogo-service");
    }
}
