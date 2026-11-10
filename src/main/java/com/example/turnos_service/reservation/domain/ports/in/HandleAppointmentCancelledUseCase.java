package com.example.turnos_service.reservation.domain.ports.in;

// Caso de uso: procesar la cancelación informada por la cátedra
public interface HandleAppointmentCancelledUseCase {
    // Marca la reserva como cancelada
    void handleAppointmentCancelled(String reservationProcessId);
}
