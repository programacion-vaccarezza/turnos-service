package com.example.turnos_service.reservation.infrastructure.kafka.dto;

import java.time.Instant;

public record AppointmentKafkaEvent(
        String eventId,
        String eventType,
        String groupId,
        String reservationProcessId,
        String holdId,
        String requestEventId,
        Long reservationId,
        Long professionalId,
        String date,
        String startTime,
        String status,
        String requiredField,
        String reason,
        String message,
        Instant expiresAt,
        Instant occurredAt,
        Integer schemaVersion) {
}
