package com.example.turnos_service.reservation.application.exception;

public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(Long processId) {
        super("No existe el proceso de reserva " + processId + " para el usuario");
    }
}
