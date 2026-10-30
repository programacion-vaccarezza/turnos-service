package com.example.turnos_service.reservation.infrastructure.persistence.adapter;

import com.example.turnos_service.reservation.domain.model.ProcessedEvent;
import com.example.turnos_service.reservation.infrastructure.persistence.entity.ProcessedEventEntity;
import com.example.turnos_service.reservation.infrastructure.persistence.mapper.ProcessedEventMapper;
import com.example.turnos_service.reservation.infrastructure.persistence.repository.JpaProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaProcessedEventRepositoryAdapterTest {

    private static final String EVENT_ID = "confirmation-event-91ac";
    private static final Instant PROCESADO = Instant.parse("2026-10-29T15:00:00Z");

    @Mock
    JpaProcessedEventRepository jpaRepository;

    @Captor
    ArgumentCaptor<ProcessedEventEntity> entityCaptor;

    private JpaProcessedEventRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaProcessedEventRepositoryAdapter(jpaRepository, new ProcessedEventMapper());
    }

    @Test
    void debeIndicarQueExiste_cuandoElEventIdYaEstaGuardado() {
        when(jpaRepository.existsById(EVENT_ID)).thenReturn(true);

        assertThat(adapter.existsByEventId(EVENT_ID)).isTrue();
    }

    @Test
    void debeIndicarQueNoExiste_cuandoElEventIdNoEstaGuardado() {
        when(jpaRepository.existsById(EVENT_ID)).thenReturn(false);

        assertThat(adapter.existsByEventId(EVENT_ID)).isFalse();
    }

    @Test
    void debeGuardarElEventoConSusDatos_cuandoSeRegistraComoProcesado() {
        ProcessedEvent evento = new ProcessedEvent(EVENT_ID, "AppointmentConfirmed", PROCESADO);
        when(jpaRepository.save(any(ProcessedEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProcessedEvent guardado = adapter.save(evento);

        verify(jpaRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue())
                .extracting(ProcessedEventEntity::getEventId, ProcessedEventEntity::getEventType, ProcessedEventEntity::getProcessedAt)
                .containsExactly(EVENT_ID, "AppointmentConfirmed", PROCESADO);
        assertThat(guardado)
                .extracting(ProcessedEvent::getEventId, ProcessedEvent::getEventType, ProcessedEvent::getProcessedAt)
                .containsExactly(EVENT_ID, "AppointmentConfirmed", PROCESADO);
    }
}
