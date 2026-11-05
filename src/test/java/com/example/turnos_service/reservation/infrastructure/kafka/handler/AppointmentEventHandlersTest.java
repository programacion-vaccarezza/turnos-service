package com.example.turnos_service.reservation.infrastructure.kafka.handler;

import com.example.turnos_service.reservation.application.service.ReservationService;
import com.example.turnos_service.reservation.infrastructure.kafka.dto.AppointmentKafkaEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/** Cada handler declara su eventType (anexo §15) y pasa a la fachada solo los campos de su tipo de evento. */
@ExtendWith(MockitoExtension.class)
class AppointmentEventHandlersTest {

    private static final String PROCESO = "process-a92b47d0";
    private static final Instant VENCE = Instant.parse("2026-09-10T14:45:00Z");

    @Mock
    ReservationService reservationService;

    @Test
    void debePasarElEventIdComoRequestEventId_cuandoLlegaAdditionalInformationRequested() {
        AdditionalInformationRequestedHandler handler = new AdditionalInformationRequestedHandler(reservationService);

        handler.handle(evento("request-event-8a31", "AdditionalInformationRequested", null, null));

        assertThat(handler.eventType()).isEqualTo("AdditionalInformationRequested");
        verify(reservationService).handleAdditionalInformationRequested(PROCESO, "request-event-8a31", VENCE);
    }

    @Test
    void debePasarElReservationId_cuandoLlegaAppointmentConfirmed() {
        AppointmentConfirmedHandler handler = new AppointmentConfirmedHandler(reservationService);

        handler.handle(evento("confirmation-event-91ac", "AppointmentConfirmed", 8251L, null));

        assertThat(handler.eventType()).isEqualTo("AppointmentConfirmed");
        verify(reservationService).handleAppointmentConfirmed(PROCESO, 8251L);
    }

    @Test
    void debePasarElMotivoYElVencimiento_cuandoLlegaAdditionalInformationRejected() {
        AdditionalInformationRejectedHandler handler = new AdditionalInformationRejectedHandler(reservationService);

        handler.handle(evento("rejection-event-1", "AdditionalInformationRejected", null, "INVALID_PHONE"));

        assertThat(handler.eventType()).isEqualTo("AdditionalInformationRejected");
        verify(reservationService).handleAdditionalInformationRejected(PROCESO, "INVALID_PHONE", VENCE);
    }

    @Test
    void debePasarElMotivo_cuandoLlegaAppointmentProcessExpired() {
        AppointmentProcessExpiredHandler handler = new AppointmentProcessExpiredHandler(reservationService);

        handler.handle(evento("expiration-event-1", "AppointmentProcessExpired", null, "PHONE_NOT_RECEIVED"));

        assertThat(handler.eventType()).isEqualTo("AppointmentProcessExpired");
        verify(reservationService).handleAppointmentProcessExpired(PROCESO, "PHONE_NOT_RECEIVED");
    }

    @Test
    void debePasarElMotivo_cuandoLlegaAppointmentProcessInvalid() {
        AppointmentProcessInvalidHandler handler = new AppointmentProcessInvalidHandler(reservationService);

        handler.handle(evento("invalid-event-1", "AppointmentProcessInvalid", null, "HOLD_MISMATCH"));

        assertThat(handler.eventType()).isEqualTo("AppointmentProcessInvalid");
        verify(reservationService).handleAppointmentProcessInvalid(PROCESO, "HOLD_MISMATCH");
    }

    private static AppointmentKafkaEvent evento(String eventId, String eventType, Long reservationId, String reason) {
        return new AppointmentKafkaEvent(eventId, eventType, "proyecto-franco-vaccarezza", PROCESO, "hold-6c0c2f41",
                null, reservationId, 15L, "2026-09-14", "09:30", null, "phoneNumber", reason, null, VENCE,
                Instant.parse("2026-09-10T14:35:00Z"), 1);
    }
}
