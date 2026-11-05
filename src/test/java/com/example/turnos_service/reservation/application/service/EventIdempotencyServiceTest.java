package com.example.turnos_service.reservation.application.service;

import com.example.turnos_service.reservation.domain.model.ProcessedEvent;
import com.example.turnos_service.reservation.domain.ports.out.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventIdempotencyServiceTest {

    private static final String EVENT_ID = "request-event-8a31";
    private static final String EVENT_TYPE = "AdditionalInformationRequested";

    @Mock
    ProcessedEventRepository processedEventRepository;

    @Captor
    ArgumentCaptor<ProcessedEvent> processedEventCaptor;

    @InjectMocks
    EventIdempotencyService service;

    private final AtomicInteger efectosAplicados = new AtomicInteger();

    @Test
    void debeAplicarElEfectoYRegistrarElEventId_cuandoElEventoEsNuevo() {
        when(processedEventRepository.existsByEventId(EVENT_ID)).thenReturn(false);

        boolean procesado = service.processOnce(EVENT_ID, EVENT_TYPE, efectosAplicados::incrementAndGet);

        assertThat(procesado).isTrue();
        assertThat(efectosAplicados).hasValue(1);
        verify(processedEventRepository).save(processedEventCaptor.capture());
        assertThat(processedEventCaptor.getValue())
                .extracting(ProcessedEvent::getEventId, ProcessedEvent::getEventType)
                .containsExactly(EVENT_ID, EVENT_TYPE);
        assertThat(processedEventCaptor.getValue().getProcessedAt()).isNotNull();
    }

    @Test
    void debeIgnorarEvento_cuandoEventIdYaFueProcesado() {
        when(processedEventRepository.existsByEventId(EVENT_ID)).thenReturn(true);

        boolean procesado = service.processOnce(EVENT_ID, EVENT_TYPE, efectosAplicados::incrementAndGet);

        assertThat(procesado).isFalse();
        assertThat(efectosAplicados).hasValue(0);
        verify(processedEventRepository, times(0)).save(any());
    }

    @Test
    void debeAplicarElEfectoUnaSolaVez_cuandoElMismoEventoLlegaDosVeces() {
        when(processedEventRepository.existsByEventId(EVENT_ID)).thenReturn(false, true);

        boolean primera = service.processOnce(EVENT_ID, EVENT_TYPE, efectosAplicados::incrementAndGet);
        boolean segunda = service.processOnce(EVENT_ID, EVENT_TYPE, efectosAplicados::incrementAndGet);

        assertThat(primera).isTrue();
        assertThat(segunda).isFalse();
        assertThat(efectosAplicados).hasValue(1);
        verify(processedEventRepository, times(1)).save(any());
    }

    @Test
    void noDebeRegistrarElEventId_cuandoElEfectoFalla() {
        when(processedEventRepository.existsByEventId(EVENT_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.processOnce(EVENT_ID, EVENT_TYPE, () -> {
            throw new IllegalStateException("falló el efecto");
        })).isInstanceOf(IllegalStateException.class);

        verify(processedEventRepository, never()).save(any());
    }
}
