package com.example.turnos_service.reservation.domain.ports.in;

public interface HandleAppointmentCancelledUseCase {
    void handleAppointmentCancelled(String reservationProcessId);
}
