package com.example.turnos_service.reservation.application.exception;

import com.example.turnos_service.reservation.domain.model.ReservationStatus;

public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(ReservationStatus current, ReservationStatus next) {
        super("Transición inválida del proceso de reserva: " + current + " -> " + next);
    }
}
