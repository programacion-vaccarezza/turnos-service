package com.example.turnos_service.reservation.domain.ports.in;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;

import java.time.LocalDate;
import java.time.LocalTime;

// Caso de uso: iniciar una reserva
public interface InitiateReservationUseCase {
    // Crea el hold y lo confirma en la cátedra
    ReservationProcess initiateReservation(String ownerLogin, Long professionalId, LocalDate date, LocalTime startTime);
}
