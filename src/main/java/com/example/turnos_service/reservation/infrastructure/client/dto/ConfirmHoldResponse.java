package com.example.turnos_service.reservation.infrastructure.client.dto;

import java.time.Instant;

// Respuesta de la cátedra al confirmar un hold
public record ConfirmHoldResponse(String reservationProcessId, String status, Instant expiresAt) {
}
