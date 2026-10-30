package com.example.turnos_service.reservation.infrastructure.persistence.mapper;

import com.example.turnos_service.reservation.domain.model.ProcessedEvent;
import com.example.turnos_service.reservation.infrastructure.persistence.entity.ProcessedEventEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessedEventMapperTest {

    private static final Instant PROCESADO = Instant.parse("2026-10-29T15:00:00Z");

    private final ProcessedEventMapper mapper = new ProcessedEventMapper();

    @Test
    void debeCopiarTodosLosCampos_cuandoConvierteDominioAEntidad() {
        ProcessedEventEntity entity = mapper.toEntity(new ProcessedEvent("event-1", "AppointmentConfirmed", PROCESADO));

        assertThat(entity)
                .extracting(ProcessedEventEntity::getEventId, ProcessedEventEntity::getEventType, ProcessedEventEntity::getProcessedAt)
                .containsExactly("event-1", "AppointmentConfirmed", PROCESADO);
    }

    @Test
    void debeCopiarTodosLosCampos_cuandoConvierteEntidadADominio() {
        ProcessedEvent evento = mapper.toDomainModel(new ProcessedEventEntity("event-1", "AppointmentConfirmed", PROCESADO));

        assertThat(evento)
                .extracting(ProcessedEvent::getEventId, ProcessedEvent::getEventType, ProcessedEvent::getProcessedAt)
                .containsExactly("event-1", "AppointmentConfirmed", PROCESADO);
    }

    @Test
    void debeDevolverNull_cuandoRecibeNull() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomainModel(null)).isNull();
    }
}
