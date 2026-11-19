package com.example.turnos_service.reservation.domain.ports.in;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;

// Caso de uso: consultar una reserva propia
public interface GetReservationUseCase {
    // Devuelve el estado de la reserva
    ReservationProcess getReservation(String ownerLogin, Long processId);
}
