package com.example.turnos_service.reservation.domain.ports.in;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;

public interface GetReservationUseCase {
    ReservationProcess getReservation(String ownerLogin, Long processId);
}
