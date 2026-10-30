package com.example.turnos_service.reservation.application.usecases;

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

import java.time.Instant;
import java.util.Optional;

import static com.example.turnos_service.reservation.domain.model.ReservationStatus.CONFIRMING;
import static com.example.turnos_service.reservation.domain.model.ReservationStatus.WAITING_FOR_PHONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandleAdditionalInformationRequestedUseCaseImplTest {

    private static final String PROCESO = "process-a92b47d0";
    private static final String REQUEST_EVENT = "request-event-8a31";
    private static final Instant VENCE_HOLD = Instant.parse("2026-09-10T14:40:00Z");
    private static final Instant VENCE_PEDIDO = Instant.parse("2026-09-10T14:45:00Z");

    @Mock
    ReservationProcessRepository reservationProcessRepository;

    @Captor
    ArgumentCaptor<ReservationProcess> processCaptor;

    @InjectMocks
    HandleAdditionalInformationRequestedUseCaseImpl useCase;

    @Test
    void debePasarAWaitingForPhoneYGuardarElPedido_cuandoElProcesoEstaConfirmando() {
        when(reservationProcessRepository.findByReservationProcessId(PROCESO)).thenReturn(Optional.of(proceso(CONFIRMING)));

        useCase.handleAdditionalInformationRequested(PROCESO, REQUEST_EVENT, VENCE_PEDIDO);

        verify(reservationProcessRepository).save(processCaptor.capture());
        assertThat(processCaptor.getValue())
                .extracting(ReservationProcess::getStatus, ReservationProcess::getRequestEventId, ReservationProcess::getExpiresAt)
                .containsExactly(WAITING_FOR_PHONE, REQUEST_EVENT, VENCE_PEDIDO);
    }

    @Test
    void debeGuardarElRequestEventId_cuandoElProcesoYaEsperabaElTelefono() {
        when(reservationProcessRepository.findByReservationProcessId(PROCESO)).thenReturn(Optional.of(proceso(WAITING_FOR_PHONE)));

        useCase.handleAdditionalInformationRequested(PROCESO, REQUEST_EVENT, VENCE_PEDIDO);

        verify(reservationProcessRepository).save(processCaptor.capture());
        assertThat(processCaptor.getValue())
                .extracting(ReservationProcess::getStatus, ReservationProcess::getRequestEventId, ReservationProcess::getExpiresAt)
                .containsExactly(WAITING_FOR_PHONE, REQUEST_EVENT, VENCE_PEDIDO);
    }

    @Test
    void debeConservarElVencimientoAnterior_cuandoElEventoNoTraeExpiresAt() {
        when(reservationProcessRepository.findByReservationProcessId(PROCESO)).thenReturn(Optional.of(proceso(CONFIRMING)));

        useCase.handleAdditionalInformationRequested(PROCESO, REQUEST_EVENT, null);

        verify(reservationProcessRepository).save(processCaptor.capture());
        assertThat(processCaptor.getValue().getExpiresAt()).isEqualTo(VENCE_HOLD);
    }

    @Test
    void debeIgnorarElEvento_cuandoElProcesoNoExisteLocalmente() {
        when(reservationProcessRepository.findByReservationProcessId(PROCESO)).thenReturn(Optional.empty());

        useCase.handleAdditionalInformationRequested(PROCESO, REQUEST_EVENT, VENCE_PEDIDO);

        verify(reservationProcessRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"CONFIRMING", "WAITING_FOR_PHONE"}, mode = EnumSource.Mode.EXCLUDE)
    void debeIgnorarElEventoSinCambiarNada_cuandoElPedidoNoAplicaAlEstado(ReservationStatus estado) {
        ReservationProcess process = proceso(estado);
        when(reservationProcessRepository.findByReservationProcessId(PROCESO)).thenReturn(Optional.of(process));

        useCase.handleAdditionalInformationRequested(PROCESO, REQUEST_EVENT, VENCE_PEDIDO);

        verify(reservationProcessRepository, never()).save(any());
        assertThat(process.getStatus()).isEqualTo(estado);
        assertThat(process.getRequestEventId()).isNull();
    }

    private static ReservationProcess proceso(ReservationStatus estado) {
        return ReservationProcess.builder()
                .id(7L).ownerLogin("juan.lopez").reservationProcessId(PROCESO)
                .status(estado).expiresAt(VENCE_HOLD).build();
    }
}
