package com.example.turnos_service.reservation.domain.ports.in;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;

public interface CancelReservationUseCase {
    ReservationProcess cancelReservation(String ownerLogin, Long processId, String reason);
}
