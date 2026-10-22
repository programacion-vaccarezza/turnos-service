package com.example.turnos_service.reservation.domain.ports.in;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;

import java.time.LocalDate;
import java.time.LocalTime;

public interface InitiateReservationUseCase {
    ReservationProcess initiateReservation(String ownerLogin, Long professionalId, LocalDate date, LocalTime startTime);
}
