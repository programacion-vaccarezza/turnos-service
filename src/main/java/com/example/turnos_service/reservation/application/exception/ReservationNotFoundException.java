package com.example.turnos_service.reservation.application.exception;

/**
 * El proceso no existe o no pertenece al usuario. En los dos casos se responde igual,
 * para no revelar procesos de otros usuarios (enunciado §9).
 */
public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(Long processId) {
        super("No existe el proceso de reserva " + processId + " para el usuario");
    }
}
