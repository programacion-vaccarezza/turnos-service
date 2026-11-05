package com.example.turnos_service.reservation.domain.ports.in;

public interface HandleAppointmentProcessExpiredUseCase {
    void handleAppointmentProcessExpired(String reservationProcessId, String reason);
}
