package com.example.turnos_service.reservation.application.service;

import com.example.turnos_service.reservation.application.exception.ConcurrentReservationUpdateException;
import com.example.turnos_service.reservation.domain.model.CatedraAppointment;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.ports.out.ReservationProcessRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static com.example.turnos_service.reservation.domain.model.ReservationStatus.CONFIRMED;
import static com.example.turnos_service.reservation.domain.model.ReservationStatus.CONFIRMING;
import static com.example.turnos_service.reservation.domain.model.ReservationStatus.EXPIRED;
import static com.example.turnos_service.reservation.domain.model.ReservationStatus.PHONE_SUBMITTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationReconcilerTest {

    private static final Long ID = 57L;
    private static final Long RESERVA = 1502L;

    @Mock
    ReservationProcessRepository reservationProcessRepository;

    @InjectMocks
    ReservationReconciler reconciler;

    @Test
    void debeConfirmarYGuardarElIdDeLaReserva_cuandoLaCatedraLaConfirmo() {
        ReservationProcess process = proceso(PHONE_SUBMITTED, enDiezMinutos());
        when(reservationProcessRepository.save(process)).thenReturn(process);

        ReservationProcess resultado = reconciler.reconcile(process, turno("CONFIRMED"));

        verify(reservationProcessRepository).save(process);
        assertThat(resultado)
                .extracting(ReservationProcess::getStatus, ReservationProcess::getReservationId, ReservationProcess::getFailureReason)
                .containsExactly(CONFIRMED, RESERVA, null);
    }

    @Test
    void debePasarAExpiredConMotivo_cuandoLaCatedraInformaFailed() {
        ReservationProcess process = proceso(CONFIRMING, enDiezMinutos());
        when(reservationProcessRepository.save(process)).thenReturn(process);

        ReservationProcess resultado = reconciler.reconcile(process, turno("FAILED"));

        assertThat(resultado)
                .extracting(ReservationProcess::getStatus, ReservationProcess::getFailureReason)
                .containsExactly(EXPIRED, "CATEDRA_FAILED");
    }

    @Test
    void noDebeGuardarNada_cuandoElProcesoYaEstaEnElEstadoDeLaCatedra() {
        ReservationProcess process = proceso(CONFIRMED, enDiezMinutos());

        assertThat(reconciler.reconcile(process, turno("CONFIRMED"))).isSameAs(process);

        verify(reservationProcessRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"WAITING_FOR_PHONE", "PHONE_SUBMITTED", "PHONE_REJECTED"})
    void noDebeCambiarElEstado_cuandoLaCatedraSigueEsperandoElTelefono(ReservationStatus estado) {
        ReservationProcess process = proceso(estado, enDiezMinutos());

        assertThat(reconciler.reconcile(process, turno("PHONE_PENDING")).getStatus()).isEqualTo(estado);

        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void noDebeCambiarElEstado_cuandoLaCatedraInformaUnEstadoDesconocido() {
        ReservationProcess process = proceso(CONFIRMING, enDiezMinutos());

        assertThat(reconciler.reconcile(process, turno("OTRO_ESTADO")).getStatus()).isEqualTo(CONFIRMING);

        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void noDebeCambiarElEstado_cuandoNoExisteUnCaminoValido() {
        ReservationProcess process = proceso(CONFIRMED, enDiezMinutos());

        assertThat(reconciler.reconcile(process, turno("FAILED")).getStatus()).isEqualTo(CONFIRMED);

        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void debePasarAExpired_cuandoLaCatedraNoTieneLaReservaYElProcesoYaVencio() {
        ReservationProcess process = proceso(CONFIRMING, haceUnMinuto());
        when(reservationProcessRepository.save(process)).thenReturn(process);

        ReservationProcess resultado = reconciler.reconcile(process, null);

        assertThat(resultado)
                .extracting(ReservationProcess::getStatus, ReservationProcess::getFailureReason)
                .containsExactly(EXPIRED, "PROCESS_EXPIRED");
    }

    @Test
    void noDebeCambiarElEstado_cuandoLaCatedraNoTieneLaReservaYTodaviaNoVencio() {
        ReservationProcess process = proceso(CONFIRMING, enDiezMinutos());

        assertThat(reconciler.reconcile(process, null).getStatus()).isEqualTo(CONFIRMING);

        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void debeDevolverElEstadoQueDejoElEvento_cuandoKafkaActualizoElProcesoAntesDeGuardar() {
        ReservationProcess actualizado = proceso(CONFIRMED, enDiezMinutos());
        when(reservationProcessRepository.save(any(ReservationProcess.class)))
                .thenThrow(new ConcurrentReservationUpdateException(ID, null));
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.of(actualizado));

        assertThat(reconciler.reconcile(proceso(CONFIRMING, enDiezMinutos()), turno("CONFIRMED"))).isSameAs(actualizado);
    }

    @Test
    void debePropagarElConflicto_cuandoElProcesoYaNoSePuedeReleer() {
        ConcurrentReservationUpdateException conflicto = new ConcurrentReservationUpdateException(ID, null);
        when(reservationProcessRepository.save(any(ReservationProcess.class))).thenThrow(conflicto);
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reconciler.reconcile(proceso(CONFIRMING, enDiezMinutos()), turno("CONFIRMED")))
                .isSameAs(conflicto);
    }

    private static ReservationProcess proceso(ReservationStatus estado, Instant expiresAt) {
        return ReservationProcess.builder()
                .id(ID).ownerLogin("juan.lopez").reservationProcessId("process-1").status(estado).expiresAt(expiresAt).build();
    }

    private static CatedraAppointment turno(String estado) {
        return CatedraAppointment.builder().reservationId(RESERVA).reservationProcessId("process-1").status(estado).build();
    }

    private static Instant enDiezMinutos() {
        return Instant.now().plus(10, ChronoUnit.MINUTES);
    }

    private static Instant haceUnMinuto() {
        return Instant.now().minus(1, ChronoUnit.MINUTES);
    }
}
