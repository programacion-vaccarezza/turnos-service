package com.example.turnos_service.reservation.application.exception;

// La reserva no existe o es de otro usuario
public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(Long processId) {
        super("No existe el proceso de reserva " + processId + " para el usuario");
    }
}
