package com.example.turnos_service.reservation.domain.ports.in;

public interface HandleAppointmentConfirmedUseCase {
    void handleAppointmentConfirmed(String reservationProcessId, Long reservationId);
}
