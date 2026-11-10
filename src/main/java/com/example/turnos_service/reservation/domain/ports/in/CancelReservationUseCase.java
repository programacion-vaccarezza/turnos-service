package com.example.turnos_service.reservation.domain.ports.in;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;

// Caso de uso: cancelar una reserva propia
public interface CancelReservationUseCase {
    // Cancela la reserva del usuario
    ReservationProcess cancelReservation(String ownerLogin, Long processId, String reason);
}
