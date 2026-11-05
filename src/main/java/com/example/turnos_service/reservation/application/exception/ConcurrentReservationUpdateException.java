package com.example.turnos_service.reservation.application.exception;

/**
 * Otro flujo (por ejemplo, un evento de Kafka) modificó el proceso de reserva al mismo tiempo
 * y el guardado se rechazó por el bloqueo optimista (@Version).
 */
public class ConcurrentReservationUpdateException extends RuntimeException {

    public ConcurrentReservationUpdateException(Long processId, Throwable cause) {
        super("El proceso de reserva " + processId + " fue modificado al mismo tiempo por otro flujo", cause);
    }
}
