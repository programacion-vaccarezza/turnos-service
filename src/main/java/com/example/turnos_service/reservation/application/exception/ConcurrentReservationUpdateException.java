package com.example.turnos_service.reservation.application.exception;

public class ConcurrentReservationUpdateException extends RuntimeException {

    public ConcurrentReservationUpdateException(Long processId, Throwable cause) {
        super("El proceso de reserva " + processId + " fue modificado al mismo tiempo por otro flujo", cause);
    }
}
