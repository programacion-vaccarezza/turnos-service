package com.example.turnos_service.reservation.domain.ports.in;

import java.time.Instant;

public interface HandleAdditionalInformationRequestedUseCase {
    void handleAdditionalInformationRequested(String reservationProcessId, String requestEventId, Instant expiresAt);
}
