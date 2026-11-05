package com.example.turnos_service.reservation.application.usecases;

import com.example.turnos_service.reservation.application.service.EventIdempotencyService;
import com.example.turnos_service.reservation.application.service.ReservationEventStatusUpdater;
import com.example.turnos_service.reservation.domain.model.ProcessedEvent;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.ports.out.ProcessedEventRepository;
import com.example.turnos_service.reservation.domain.ports.out.ReservationProcessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.example.turnos_service.reservation.domain.model.ReservationStatus.CANCELLED;
import static com.example.turnos_service.reservation.domain.model.ReservationStatus.CONFIRMED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandleAppointmentCancelledUseCaseImplTest {

    private static final String PROCESO = "process-1";
    private static final String EVENTO = "cancellation-event-1";
    private static final String TIPO = "AppointmentCancelled";

    @Mock
    ReservationProcessRepository reservationProcessRepository;

    @Mock
    ProcessedEventRepository processedEventRepository;

    HandleAppointmentCancelledUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new HandleAppointmentCancelledUseCaseImpl(new ReservationEventStatusUpdater(reservationProcessRepository));
    }

    @Test
    void debePasarACancelled_cuandoLaReservaEstaConfirmada() {
        ReservationProcess process = proceso(CONFIRMED);
        when(reservationProcessRepository.findByReservationProcessId(PROCESO)).thenReturn(Optional.of(process));

        useCase.handleAppointmentCancelled(PROCESO);

        verify(reservationProcessRepository).save(process);
        assertThat(process.getStatus()).isEqualTo(CANCELLED);
    }

    @Test
    void noDebeGuardarNada_cuandoLaReservaYaEstabaCancelada() {
        when(reservationProcessRepository.findByReservationProcessId(PROCESO)).thenReturn(Optional.of(proceso(CANCELLED)));

        useCase.handleAppointmentCancelled(PROCESO);

        verify(reservationProcessRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"CONFIRMED", "CANCELLED"}, mode = EnumSource.Mode.EXCLUDE)
    void debeIgnorarElEvento_cuandoElEstadoNoPermiteCancelar(ReservationStatus estado) {
        ReservationProcess process = proceso(estado);
        when(reservationProcessRepository.findByReservationProcessId(PROCESO)).thenReturn(Optional.of(process));

        useCase.handleAppointmentCancelled(PROCESO);

        assertThat(process.getStatus()).isEqualTo(estado);
        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void debeIgnorarElEvento_cuandoElProcesoNoEsConocido() {
        when(reservationProcessRepository.findByReservationProcessId(PROCESO)).thenReturn(Optional.empty());

        useCase.handleAppointmentCancelled(PROCESO);

        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void debeAplicarElEventoUnaSolaVez_cuandoKafkaLoEntregaDosVeces() {
        EventIdempotencyService idempotencia = new EventIdempotencyService(processedEventRepository);
        when(processedEventRepository.existsByEventId(EVENTO)).thenReturn(false, true);
        when(reservationProcessRepository.findByReservationProcessId(PROCESO)).thenReturn(Optional.of(proceso(CONFIRMED)));

        boolean primera = idempotencia.processOnce(EVENTO, TIPO, () -> useCase.handleAppointmentCancelled(PROCESO));
        boolean segunda = idempotencia.processOnce(EVENTO, TIPO, () -> useCase.handleAppointmentCancelled(PROCESO));

        assertThat(primera).isTrue();
        assertThat(segunda).isFalse();
        verify(reservationProcessRepository, times(1)).save(any());
        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
    }

    private static ReservationProcess proceso(ReservationStatus estado) {
        return ReservationProcess.builder().id(57L).reservationProcessId(PROCESO).status(estado).build();
    }
}
