package com.example.turnos_service.reservation.application.service;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.ports.out.ReservationProcessRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.example.turnos_service.reservation.domain.model.ReservationStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationEventStatusUpdaterTest {

    private static final String PROCESO = "process-a92b47d0";

    @Mock
    ReservationProcessRepository reservationProcessRepository;

    @Captor
    ArgumentCaptor<ReservationProcess> processCaptor;

    @InjectMocks
    ReservationEventStatusUpdater updater;

    private final AtomicBoolean cambiosAplicados = new AtomicBoolean();

    @Test
    void debeAplicarLosCambiosYGuardarElNuevoEstado_cuandoLaTransicionEsValida() {
        when(reservationProcessRepository.findByReservationProcessId(PROCESO)).thenReturn(Optional.of(proceso(PHONE_SUBMITTED)));

        updater.apply(PROCESO, CONFIRMED, "AppointmentConfirmed", process -> {
            cambiosAplicados.set(true);
            process.setReservationId(8251L);
        });

        verify(reservationProcessRepository).save(processCaptor.capture());
        assertThat(cambiosAplicados).isTrue();
        assertThat(processCaptor.getValue())
                .extracting(ReservationProcess::getStatus, ReservationProcess::getReservationId)
                .containsExactly(CONFIRMED, 8251L);
    }

    @Test
    void debeIgnorarElEvento_cuandoElProcesoNoExisteLocalmente() {
        when(reservationProcessRepository.findByReservationProcessId(PROCESO)).thenReturn(Optional.empty());

        updater.apply(PROCESO, CONFIRMED, "AppointmentConfirmed", process -> cambiosAplicados.set(true));

        assertThat(cambiosAplicados).isFalse();
        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void debeIgnorarElEvento_cuandoElProcesoYaEstaEnEseEstado() {
        when(reservationProcessRepository.findByReservationProcessId(PROCESO)).thenReturn(Optional.of(proceso(CONFIRMED)));

        updater.apply(PROCESO, CONFIRMED, "AppointmentConfirmed", process -> cambiosAplicados.set(true));

        assertThat(cambiosAplicados).isFalse();
        verify(reservationProcessRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"CANCELLED", "EXPIRED", "INVALID", "FAILED"})
    void debeIgnorarElEventoSinReabrir_cuandoElProcesoEstaEnUnEstadoFinal(ReservationStatus estadoFinal) {
        ReservationProcess process = proceso(estadoFinal);
        when(reservationProcessRepository.findByReservationProcessId(PROCESO)).thenReturn(Optional.of(process));

        updater.apply(PROCESO, CONFIRMED, "AppointmentConfirmed", p -> cambiosAplicados.set(true));

        assertThat(cambiosAplicados).isFalse();
        assertThat(process.getStatus()).isEqualTo(estadoFinal);
        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void debeIgnorarElEvento_cuandoLlegaFueraDeOrden() {
        ReservationProcess process = proceso(HOLD_CREATED);
        when(reservationProcessRepository.findByReservationProcessId(PROCESO)).thenReturn(Optional.of(process));

        updater.apply(PROCESO, PHONE_REJECTED, "AdditionalInformationRejected", p -> cambiosAplicados.set(true));

        assertThat(cambiosAplicados).isFalse();
        assertThat(process.getStatus()).isEqualTo(HOLD_CREATED);
        verify(reservationProcessRepository, never()).save(any());
    }

    private static ReservationProcess proceso(ReservationStatus estado) {
        return ReservationProcess.builder().id(7L).reservationProcessId(PROCESO).status(estado).build();
    }
}
