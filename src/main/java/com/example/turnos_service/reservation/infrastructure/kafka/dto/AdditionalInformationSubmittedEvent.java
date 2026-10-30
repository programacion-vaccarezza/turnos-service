package com.example.turnos_service.reservation.infrastructure.kafka.dto;

import java.time.Instant;

/**
 * Evento que turnos-service publica con el teléfono (anexo §15.5). Todos los campos son obligatorios;
 * eventType y schemaVersion deben tener exactamente estos valores.
 */
public record AdditionalInformationSubmittedEvent(
        String eventId,
        String eventType,
        String groupId,
        String reservationProcessId,
        String holdId,
        String requestEventId,
        Information information,
        Instant occurredAt,
        Integer schemaVersion) {

    public static final String EVENT_TYPE = "AdditionalInformationSubmitted";
    public static final int SCHEMA_VERSION = 1;

    public record Information(String phoneNumber) {
    }
}
