package com.example.turnos_service.reservation.infrastructure.kafka.dto;

import java.time.Instant;

/**
 * Evento del topic de acciones de la cátedra (anexo §15.4 a §15.10).
 * Un solo record con todos los campos posibles de los distintos tipos de evento:
 * los que un tipo no trae quedan en null. Cada handler usa solo los campos de su tipo.
 */
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
