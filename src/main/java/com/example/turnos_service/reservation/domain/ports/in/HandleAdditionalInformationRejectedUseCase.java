package com.example.turnos_service.reservation.domain.ports.in;

import java.time.Instant;

// Caso de uso: procesar el rechazo del teléfono
public interface HandleAdditionalInformationRejectedUseCase {
    // Registra el rechazo y el nuevo vencimiento
    void handleAdditionalInformationRejected(String reservationProcessId, String reason, Instant expiresAt);
}
