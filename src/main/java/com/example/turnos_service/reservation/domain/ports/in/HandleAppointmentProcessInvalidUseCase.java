package com.example.turnos_service.reservation.domain.ports.in;

// Caso de uso: procesar un proceso inválido
public interface HandleAppointmentProcessInvalidUseCase {
    // Marca el proceso como inválido
    void handleAppointmentProcessInvalid(String reservationProcessId, String reason);
}
