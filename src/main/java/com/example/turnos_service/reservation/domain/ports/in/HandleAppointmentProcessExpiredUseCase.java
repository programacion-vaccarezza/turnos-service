package com.example.turnos_service.reservation.domain.ports.in;

// Caso de uso: procesar el vencimiento del proceso
public interface HandleAppointmentProcessExpiredUseCase {
    // Marca el proceso como vencido
    void handleAppointmentProcessExpired(String reservationProcessId, String reason);
}
