package com.example.turnos_service.reservation.application.exception;

// La cátedra todavía no pidió el teléfono
public class PhoneNotRequestedException extends RuntimeException {

    public PhoneNotRequestedException(Long processId) {
        super("La cátedra todavía no pidió el teléfono para el proceso " + processId);
    }
}
