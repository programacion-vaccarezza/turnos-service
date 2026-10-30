package com.example.turnos_service.reservation.infrastructure.kafka.consumer;

import com.example.turnos_service.reservation.application.service.EventIdempotencyService;
import com.example.turnos_service.reservation.infrastructure.kafka.dto.AppointmentKafkaEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentActionsConsumerTest {

    @Mock
    AppointmentEventHandler confirmedHandler;

    @Mock
    AppointmentEventHandler expiredHandler;

    @Mock
    EventIdempotencyService eventIdempotencyService;

    @Captor
    ArgumentCaptor<Runnable> efectoCaptor;

    private AppointmentActionsConsumer consumer;

    @BeforeEach
    void setUp() {
        when(confirmedHandler.eventType()).thenReturn("AppointmentConfirmed");
        when(expiredHandler.eventType()).thenReturn("AppointmentProcessExpired");
        consumer = new AppointmentActionsConsumer(List.of(confirmedHandler, expiredHandler), eventIdempotencyService);
        // Simula un evento nuevo: el servicio de idempotencia ejecuta el efecto.
        lenient().when(eventIdempotencyService.processOnce(anyString(), anyString(), any(Runnable.class))).thenAnswer(invocation -> {
            invocation.<Runnable>getArgument(2).run();
            return true;
        });
    }

    @Test
    void debeDespacharAlHandlerDeSuTipo_cuandoLlegaUnEvento() {
        AppointmentKafkaEvent evento = evento("event-1", "AppointmentConfirmed");

        consumer.receive(evento, 0, 10L);

        verify(confirmedHandler).handle(evento);
        verify(expiredHandler, never()).handle(any());
    }

    @Test
    void debeProcesarElEventoAtravesDeLaIdempotencia_cuandoLlegaUnEvento() {
        AppointmentKafkaEvent evento = evento("event-1", "AppointmentProcessExpired");

        consumer.receive(evento, 0, 11L);

        verify(eventIdempotencyService).processOnce(eq("event-1"), eq("AppointmentProcessExpired"), efectoCaptor.capture());
        verify(expiredHandler).handle(evento);
    }

    @Test
    void noDebeEjecutarElHandler_cuandoElEventIdYaFueProcesado() {
        when(eventIdempotencyService.processOnce(anyString(), anyString(), any(Runnable.class))).thenReturn(false);

        consumer.receive(evento("event-1", "AppointmentConfirmed"), 0, 12L);

        verify(confirmedHandler, never()).handle(any());
    }

    @Test
    void debeIgnorarElEvento_cuandoSuTipoNoTieneHandler() {
        consumer.receive(evento("event-1", "AppointmentRescheduled"), 0, 13L);

        verifyNoInteractions(eventIdempotencyService);
        verify(confirmedHandler, never()).handle(any());
        verify(expiredHandler, never()).handle(any());
    }

    @Test
    void debeIgnorarElEvento_cuandoNoTraeEventId() {
        consumer.receive(evento(null, "AppointmentConfirmed"), 0, 14L);

        verifyNoInteractions(eventIdempotencyService);
        verify(confirmedHandler, never()).handle(any());
    }

    @Test
    void debeIgnorarElEvento_cuandoNoTraeEventType() {
        consumer.receive(evento("event-1", null), 0, 15L);

        verifyNoInteractions(eventIdempotencyService);
    }

    private static AppointmentKafkaEvent evento(String eventId, String eventType) {
        return new AppointmentKafkaEvent(eventId, eventType, "proyecto-franco-vaccarezza", "process-a92b47d0",
                null, null, null, null, null, null, null, null, null, null, null, null, 1);
    }
}
