package com.example.turnos_service.reservation.application.exception;

// El profesional está deshabilitado
public class ProfessionalDisabledException extends RuntimeException {

    public ProfessionalDisabledException(Long professionalId) {
        super("El profesional con id " + professionalId + " no está habilitado");
    }
}
