package com.example.turnos_service.reservation.application.usecases;

import com.example.turnos_service.reservation.application.service.ReservationEventStatusUpdater;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.function.Consumer;

import static com.example.turnos_service.reservation.domain.model.ReservationStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/** Cada caso de uso de resultado pide al updater su estado destino y aplica solo los datos de su evento. */
@ExtendWith(MockitoExtension.class)
class ReservationResultUseCasesTest {

    private static final String PROCESO = "process-a92b47d0";
    private static final Instant VENCE_ANTERIOR = Instant.parse("2026-09-10T14:40:00Z");
    private static final Instant VENCE_NUEVO = Instant.parse("2026-09-10T14:45:00Z");

    @Mock
    ReservationEventStatusUpdater statusUpdater;

    @Captor
    ArgumentCaptor<Consumer<ReservationProcess>> cambiosCaptor;

    @Test
    void debeConfirmarYGuardarElReservationIdLimpiandoElMotivo_cuandoLaCatedraConfirma() {
        new HandleAppointmentConfirmedUseCaseImpl(statusUpdater).handleAppointmentConfirmed(PROCESO, 8251L);

        verify(statusUpdater).apply(eq(PROCESO), eq(CONFIRMED), eq("AppointmentConfirmed"), cambiosCaptor.capture());
        ReservationProcess process = procesoConMotivo("INVALID_PHONE");
        cambiosCaptor.getValue().accept(process);
        assertThat(process)
                .extracting(ReservationProcess::getReservationId, ReservationProcess::getFailureReason)
                .containsExactly(8251L, null);
    }

    @Test
    void debePasarAPhoneRejectedConElMotivoYElNuevoVencimiento_cuandoLaCatedraRechazaElTelefono() {
        new HandleAdditionalInformationRejectedUseCaseImpl(statusUpdater)
                .handleAdditionalInformationRejected(PROCESO, "INVALID_PHONE", VENCE_NUEVO);

        verify(statusUpdater).apply(eq(PROCESO), eq(PHONE_REJECTED), eq("AdditionalInformationRejected"), cambiosCaptor.capture());
        ReservationProcess process = procesoConMotivo(null);
        cambiosCaptor.getValue().accept(process);
        assertThat(process)
                .extracting(ReservationProcess::getFailureReason, ReservationProcess::getExpiresAt)
                .containsExactly("INVALID_PHONE", VENCE_NUEVO);
    }

    @Test
    void debeConservarElVencimiento_cuandoElRechazoNoTraeExpiresAt() {
        new HandleAdditionalInformationRejectedUseCaseImpl(statusUpdater)
                .handleAdditionalInformationRejected(PROCESO, "INVALID_PHONE", null);

        verify(statusUpdater).apply(eq(PROCESO), eq(PHONE_REJECTED), eq("AdditionalInformationRejected"), cambiosCaptor.capture());
        ReservationProcess process = procesoConMotivo(null);
        cambiosCaptor.getValue().accept(process);
        assertThat(process.getExpiresAt()).isEqualTo(VENCE_ANTERIOR);
    }

    @Test
    void debePasarAExpiredConElMotivo_cuandoLaCatedraVenceElProceso() {
        new HandleAppointmentProcessExpiredUseCaseImpl(statusUpdater).handleAppointmentProcessExpired(PROCESO, "PHONE_NOT_RECEIVED");

        verify(statusUpdater).apply(eq(PROCESO), eq(EXPIRED), eq("AppointmentProcessExpired"), cambiosCaptor.capture());
        ReservationProcess process = procesoConMotivo(null);
        cambiosCaptor.getValue().accept(process);
        assertThat(process.getFailureReason()).isEqualTo("PHONE_NOT_RECEIVED");
    }

    @Test
    void debePasarAInvalidConElMotivo_cuandoLaCatedraInvalidaElProceso() {
        new HandleAppointmentProcessInvalidUseCaseImpl(statusUpdater).handleAppointmentProcessInvalid(PROCESO, "HOLD_MISMATCH");

        verify(statusUpdater).apply(eq(PROCESO), eq(INVALID), eq("AppointmentProcessInvalid"), cambiosCaptor.capture());
        ReservationProcess process = procesoConMotivo(null);
        cambiosCaptor.getValue().accept(process);
        assertThat(process.getFailureReason()).isEqualTo("HOLD_MISMATCH");
    }

    private static ReservationProcess procesoConMotivo(String motivo) {
        return ReservationProcess.builder().id(7L).reservationProcessId(PROCESO)
                .status(PHONE_SUBMITTED).expiresAt(VENCE_ANTERIOR).failureReason(motivo).build();
    }
}
