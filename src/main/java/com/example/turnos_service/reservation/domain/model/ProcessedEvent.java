package com.example.turnos_service.reservation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Evento de Kafka ya procesado. Su {@code eventId} es la clave de idempotencia (anexo §15.1).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    private String eventId;
    private String eventType;
    private Instant processedAt;
}
