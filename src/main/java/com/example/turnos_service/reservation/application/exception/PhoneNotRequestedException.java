package com.example.turnos_service.reservation.application.exception;

public class PhoneNotRequestedException extends RuntimeException {

    public PhoneNotRequestedException(Long processId) {
        super("La cátedra todavía no pidió el teléfono para el proceso " + processId);
    }
}
