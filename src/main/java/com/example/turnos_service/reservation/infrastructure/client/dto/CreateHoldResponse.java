package com.example.turnos_service.reservation.infrastructure.client.dto;

import java.time.Instant;

public record CreateHoldResponse(String holdId, String reservationProcessId, String status, Instant expiresAt) {
}
