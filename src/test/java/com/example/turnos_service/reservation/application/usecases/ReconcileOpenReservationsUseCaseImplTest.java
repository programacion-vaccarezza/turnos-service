package com.example.turnos_service.reservation.application.usecases;

import com.example.turnos_service.reservation.application.exception.CatedraUnavailableException;
import com.example.turnos_service.reservation.application.exception.ConcurrentReservationUpdateException;
import com.example.turnos_service.reservation.domain.model.CatedraAppointment;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.ports.out.CatedraAppointmentPort;
import com.example.turnos_service.reservation.domain.ports.out.ReservationProcessRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;

import static com.example.turnos_service.reservation.domain.model.ReservationStatus.CONFIRMED;
import static com.example.turnos_service.reservation.domain.model.ReservationStatus.CONFIRMING;
import static com.example.turnos_service.reservation.domain.model.ReservationStatus.EXPIRED;
import static com.example.turnos_service.reservation.domain.model.ReservationStatus.HOLD_CREATED;
import static com.example.turnos_service.reservation.domain.model.ReservationStatus.PHONE_REJECTED;
import static com.example.turnos_service.reservation.domain.model.ReservationStatus.PHONE_SUBMITTED;
import static com.example.turnos_service.reservation.domain.model.ReservationStatus.WAITING_FOR_PHONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconcileOpenReservationsUseCaseImplTest {

    private static final String LOGIN = "juan.lopez";
    private static final String OTRO_LOGIN = "maria.prueba";
    private static final String PROCESO = "process-1";
    private static final Long RESERVA = 1502L;

    @Mock
    ReservationProcessRepository reservationProcessRepository;

    @Mock
    CatedraAppointmentPort catedraAppointmentPort;

    @Captor
    ArgumentCaptor<Collection<ReservationStatus>> estadosCaptor;

    @InjectMocks
    ReconcileOpenReservationsUseCaseImpl useCase;

    @Test
    void noDebeConsultarALaCatedra_cuandoNoHayProcesosAbiertos() {
        when(reservationProcessRepository.findByStatusIn(any())).thenReturn(List.of());

        useCase.reconcileOpenReservations();

        verifyNoInteractions(catedraAppointmentPort);
    }

    @Test
    void debeBuscarSoloLosEstadosQueTodaviaPuedenCambiar_cuandoReconcilia() {
        when(reservationProcessRepository.findByStatusIn(any())).thenReturn(List.of());

        useCase.reconcileOpenReservations();

        verify(reservationProcessRepository).findByStatusIn(estadosCaptor.capture());
        assertThat(estadosCaptor.getValue()).containsExactlyInAnyOrder(
                HOLD_CREATED, CONFIRMING, WAITING_FOR_PHONE, PHONE_SUBMITTED, PHONE_REJECTED, CONFIRMED);
    }

    @ParameterizedTest
    @CsvSource({
            "HOLD_CREATED, PHONE_PENDING, WAITING_FOR_PHONE",
            "CONFIRMING, PHONE_PENDING, WAITING_FOR_PHONE",
            "CONFIRMING, CONFIRMED, CONFIRMED",
            "WAITING_FOR_PHONE, CONFIRMED, CONFIRMED",
            "PHONE_SUBMITTED, CONFIRMED, CONFIRMED",
            "PHONE_REJECTED, CONFIRMED, CONFIRMED",
            "CONFIRMED, CANCELLED, CANCELLED",
            "PHONE_SUBMITTED, CANCELLED, CANCELLED"
    })
    void debeLlevarElProcesoAlEstadoDeLaCatedra_cuandoExisteUnCaminoValido(ReservationStatus local, String catedra,
                                                                           ReservationStatus esperado) {
        ReservationProcess process = proceso(LOGIN, PROCESO, local, enDiezMinutos());
        procesosAbiertos(process);
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN)).thenReturn(List.of(turno(PROCESO, catedra)));

        useCase.reconcileOpenReservations();

        verify(reservationProcessRepository).save(process);
        assertThat(process)
                .extracting(ReservationProcess::getStatus, ReservationProcess::getReservationId, ReservationProcess::getFailureReason)
                .containsExactly(esperado, RESERVA, null);
    }

    @Test
    void debePasarAExpiredConMotivo_cuandoLaCatedraInformaFailed() {
        ReservationProcess process = proceso(LOGIN, PROCESO, PHONE_SUBMITTED, enDiezMinutos());
        procesosAbiertos(process);
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN)).thenReturn(List.of(turno(PROCESO, "FAILED")));

        useCase.reconcileOpenReservations();

        verify(reservationProcessRepository).save(process);
        assertThat(process)
                .extracting(ReservationProcess::getStatus, ReservationProcess::getFailureReason)
                .containsExactly(EXPIRED, "CATEDRA_FAILED");
    }

    @Test
    void noDebeGuardarNada_cuandoElProcesoYaEstaEnElEstadoDeLaCatedra() {
        procesosAbiertos(proceso(LOGIN, PROCESO, CONFIRMED, enDiezMinutos()));
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN)).thenReturn(List.of(turno(PROCESO, "CONFIRMED")));

        useCase.reconcileOpenReservations();

        verify(reservationProcessRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"WAITING_FOR_PHONE", "PHONE_SUBMITTED", "PHONE_REJECTED"})
    void noDebeCambiarElEstado_cuandoLaCatedraSigueEsperandoElTelefono(ReservationStatus local) {
        ReservationProcess process = proceso(LOGIN, PROCESO, local, enDiezMinutos());
        procesosAbiertos(process);
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN)).thenReturn(List.of(turno(PROCESO, "PHONE_PENDING")));

        useCase.reconcileOpenReservations();

        assertThat(process.getStatus()).isEqualTo(local);
        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void noDebeCambiarElEstado_cuandoLaCatedraInformaUnEstadoDesconocido() {
        ReservationProcess process = proceso(LOGIN, PROCESO, CONFIRMING, enDiezMinutos());
        procesosAbiertos(process);
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN)).thenReturn(List.of(turno(PROCESO, "OTRO_ESTADO")));

        useCase.reconcileOpenReservations();

        assertThat(process.getStatus()).isEqualTo(CONFIRMING);
        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void noDebeCambiarElEstado_cuandoNoExisteUnCaminoValido() {
        ReservationProcess process = proceso(LOGIN, PROCESO, CONFIRMED, enDiezMinutos());
        procesosAbiertos(process);
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN)).thenReturn(List.of(turno(PROCESO, "FAILED")));

        useCase.reconcileOpenReservations();

        assertThat(process.getStatus()).isEqualTo(CONFIRMED);
        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void debePasarAExpired_cuandoLaCatedraNoListaElProcesoYYaVencio() {
        ReservationProcess process = proceso(LOGIN, PROCESO, CONFIRMING, haceUnMinuto());
        procesosAbiertos(process);
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN)).thenReturn(List.of());

        useCase.reconcileOpenReservations();

        verify(reservationProcessRepository).save(process);
        assertThat(process)
                .extracting(ReservationProcess::getStatus, ReservationProcess::getFailureReason)
                .containsExactly(EXPIRED, "PROCESS_EXPIRED");
    }

    @Test
    void noDebeCambiarElEstado_cuandoLaCatedraNoListaElProcesoYTodaviaNoVencio() {
        ReservationProcess process = proceso(LOGIN, PROCESO, CONFIRMING, enDiezMinutos());
        procesosAbiertos(process);
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN)).thenReturn(List.of());

        useCase.reconcileOpenReservations();

        assertThat(process.getStatus()).isEqualTo(CONFIRMING);
        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void noDebeCambiarElEstado_cuandoLaCatedraNoListaElProcesoYNoTieneVencimiento() {
        ReservationProcess process = proceso(LOGIN, PROCESO, HOLD_CREATED, null);
        procesosAbiertos(process);
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN)).thenReturn(List.of());

        useCase.reconcileOpenReservations();

        assertThat(process.getStatus()).isEqualTo(HOLD_CREATED);
        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void noDebeVencerUnaReservaConfirmada_cuandoLaCatedraNoLaLista() {
        ReservationProcess process = proceso(LOGIN, PROCESO, CONFIRMED, haceUnMinuto());
        procesosAbiertos(process);
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN)).thenReturn(List.of());

        useCase.reconcileOpenReservations();

        assertThat(process.getStatus()).isEqualTo(CONFIRMED);
        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void debeConsultarUnaVezPorDueno_cuandoHayVariosProcesos() {
        procesosAbiertos(proceso(LOGIN, PROCESO, CONFIRMED, enDiezMinutos()),
                proceso(LOGIN, "process-2", CONFIRMED, enDiezMinutos()),
                proceso(OTRO_LOGIN, "process-3", CONFIRMED, enDiezMinutos()));
        when(catedraAppointmentPort.findByExternalPatientId(anyString())).thenReturn(List.of());

        useCase.reconcileOpenReservations();

        verify(catedraAppointmentPort, times(1)).findByExternalPatientId(LOGIN);
        verify(catedraAppointmentPort, times(1)).findByExternalPatientId(OTRO_LOGIN);
    }

    @Test
    void debeIgnorarLaReserva_cuandoLaCatedraNoInformaSuProceso() {
        ReservationProcess process = proceso(LOGIN, PROCESO, CONFIRMING, enDiezMinutos());
        procesosAbiertos(process);
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN)).thenReturn(List.of(turno(null, "CONFIRMED")));

        useCase.reconcileOpenReservations();

        assertThat(process.getStatus()).isEqualTo(CONFIRMING);
        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void debeSeguirConLosDemasProcesos_cuandoUnEventoCambiaUnProcesoAlMismoTiempo() {
        ReservationProcess conflicto = proceso(LOGIN, PROCESO, CONFIRMING, enDiezMinutos());
        ReservationProcess siguiente = proceso(LOGIN, "process-2", CONFIRMING, enDiezMinutos());
        procesosAbiertos(conflicto, siguiente);
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN))
                .thenReturn(List.of(turno(PROCESO, "CONFIRMED"), turno("process-2", "CONFIRMED")));
        when(reservationProcessRepository.save(conflicto)).thenThrow(new ConcurrentReservationUpdateException(1L, null));

        useCase.reconcileOpenReservations();

        verify(reservationProcessRepository).save(siguiente);
        assertThat(siguiente.getStatus()).isEqualTo(CONFIRMED);
    }

    @Test
    void debeReintentarYReconciliar_cuandoLaCatedraRespondeEnElSegundoIntento() {
        ReservationProcess process = proceso(LOGIN, PROCESO, CONFIRMING, enDiezMinutos());
        procesosAbiertos(process);
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN))
                .thenThrow(new CatedraUnavailableException("sin conexión", null))
                .thenReturn(List.of(turno(PROCESO, "CONFIRMED")));

        useCase.reconcileOpenReservations();

        verify(catedraAppointmentPort, times(2)).findByExternalPatientId(LOGIN);
        assertThat(process.getStatus()).isEqualTo(CONFIRMED);
    }

    @Test
    void debeRendirseSinTocarNada_cuandoLaCatedraNoRespondeEnNingunIntento() {
        ReservationProcess process = proceso(LOGIN, PROCESO, CONFIRMING, haceUnMinuto());
        procesosAbiertos(process);
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN))
                .thenThrow(new CatedraUnavailableException("sin conexión", null));

        useCase.reconcileOpenReservations();

        verify(catedraAppointmentPort, times(3)).findByExternalPatientId(LOGIN);
        assertThat(process.getStatus()).isEqualTo(CONFIRMING);
        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void debeDejarDeReintentar_cuandoElHiloEsInterrumpido() {
        procesosAbiertos(proceso(LOGIN, PROCESO, CONFIRMING, haceUnMinuto()));
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN))
                .thenThrow(new CatedraUnavailableException("sin conexión", null));
        Thread.currentThread().interrupt();

        useCase.reconcileOpenReservations();

        boolean interrumpido = Thread.interrupted();
        assertThat(interrumpido).isTrue();
        verify(catedraAppointmentPort, times(1)).findByExternalPatientId(LOGIN);
        verify(reservationProcessRepository, never()).save(any());
    }

    private void procesosAbiertos(ReservationProcess... procesos) {
        when(reservationProcessRepository.findByStatusIn(any())).thenReturn(List.of(procesos));
    }

    private static ReservationProcess proceso(String login, String reservationProcessId, ReservationStatus estado, Instant expiresAt) {
        return ReservationProcess.builder()
                .id(57L).ownerLogin(login).reservationProcessId(reservationProcessId).status(estado).expiresAt(expiresAt).build();
    }

    private static CatedraAppointment turno(String reservationProcessId, String estado) {
        return CatedraAppointment.builder()
                .reservationId(RESERVA).reservationProcessId(reservationProcessId).externalPatientId(LOGIN).status(estado).build();
    }

    private static Instant enDiezMinutos() {
        return Instant.now().plus(10, ChronoUnit.MINUTES);
    }

    private static Instant haceUnMinuto() {
        return Instant.now().minus(1, ChronoUnit.MINUTES);
    }
}
