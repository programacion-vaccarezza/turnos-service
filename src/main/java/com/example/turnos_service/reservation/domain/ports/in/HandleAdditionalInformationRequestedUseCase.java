package com.example.turnos_service.reservation.domain.ports.in;

import java.time.Instant;

// Caso de uso: procesar el pedido de teléfono
public interface HandleAdditionalInformationRequestedUseCase {
    // Registra el pedido de teléfono
    void handleAdditionalInformationRequested(String reservationProcessId, String requestEventId, Instant expiresAt);
}
