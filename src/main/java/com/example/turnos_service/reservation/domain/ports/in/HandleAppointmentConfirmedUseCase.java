package com.example.turnos_service.reservation.domain.ports.in;

// Caso de uso: procesar la confirmación de la reserva
public interface HandleAppointmentConfirmedUseCase {
    // Marca la reserva como confirmada
    void handleAppointmentConfirmed(String reservationProcessId, Long reservationId);
}
