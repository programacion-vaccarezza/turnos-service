package com.example.turnos_service.reservation.domain.ports.in;

import java.time.Instant;

public interface HandleAdditionalInformationRejectedUseCase {
    void handleAdditionalInformationRejected(String reservationProcessId, String reason, Instant expiresAt);
}
