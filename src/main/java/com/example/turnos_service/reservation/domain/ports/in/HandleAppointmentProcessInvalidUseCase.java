package com.example.turnos_service.reservation.domain.ports.in;

public interface HandleAppointmentProcessInvalidUseCase {
    void handleAppointmentProcessInvalid(String reservationProcessId, String reason);
}
