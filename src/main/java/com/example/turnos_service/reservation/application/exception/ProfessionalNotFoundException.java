package com.example.turnos_service.reservation.application.exception;

public class ProfessionalNotFoundException extends RuntimeException {

    public ProfessionalNotFoundException(Long professionalId) {
        super("No existe el profesional con id: " + professionalId);
    }
}
