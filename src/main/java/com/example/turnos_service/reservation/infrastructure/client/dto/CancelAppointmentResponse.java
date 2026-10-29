package com.example.turnos_service.reservation.infrastructure.client.dto;

import java.time.Instant;

public record CancelAppointmentResponse(Long reservationId, String reservationProcessId, String status, Instant cancelledAt) {
}
