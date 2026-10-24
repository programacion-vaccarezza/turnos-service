package com.example.turnos_service.reservation.application.exception;

public class ProfessionalDisabledException extends RuntimeException {

    public ProfessionalDisabledException(Long professionalId) {
        super("El profesional con id " + professionalId + " no está habilitado");
    }
}
