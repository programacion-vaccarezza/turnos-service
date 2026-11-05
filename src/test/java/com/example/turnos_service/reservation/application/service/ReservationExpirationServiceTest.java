package com.example.turnos_service.reservation.application.service;

import com.example.turnos_service.reservation.application.exception.ConcurrentReservationUpdateException;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.ports.out.ReservationProcessRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static com.example.turnos_service.reservation.domain.model.ReservationStatus.CONFIRMED;
import static com.example.turnos_service.reservation.domain.model.ReservationStatus.CONFIRMING;
import static com.example.turnos_service.reservation.domain.model.ReservationStatus.EXPIRED;
import static com.example.turnos_service.reservation.domain.model.ReservationStatus.WAITING_FOR_PHONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationExpirationServiceTest {

    private static final Long ID = 57L;

    @Mock
    ReservationProcessRepository reservationProcessRepository;

    @InjectMocks
    ReservationExpirationService service;

    @Test
    void debePasarAExpiredYGuardarElMotivo_cuandoYaPasoElVencimiento() {
        ReservationProcess process = proceso(WAITING_FOR_PHONE, haceUnMinuto());
        when(reservationProcessRepository.save(process)).thenReturn(process);

        ReservationProcess resultado = service.expireIfOverdue(process);

        verify(reservationProcessRepository).save(process);
        assertThat(resultado)
                .extracting(ReservationProcess::getStatus, ReservationProcess::getFailureReason)
                .containsExactly(EXPIRED, ReservationExpirationService.PROCESS_EXPIRED);
    }

    @Test
    void noDebeCambiarNada_cuandoTodaviaNoVencio() {
        ReservationProcess process = proceso(WAITING_FOR_PHONE, enDiezMinutos());

        ReservationProcess resultado = service.expireIfOverdue(process);

        assertThat(resultado).isSameAs(process);
        assertThat(resultado.getStatus()).isEqualTo(WAITING_FOR_PHONE);
        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void noDebeVencerElProceso_cuandoElResultadoDeLaCatedraEsDesconocido() {
        ReservationProcess process = proceso(CONFIRMING, haceUnMinuto());

        ReservationProcess resultado = service.expireIfOverdue(process);

        assertThat(resultado.getStatus()).isEqualTo(CONFIRMING);
        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void debeDevolverElEstadoQueDejoElEvento_cuandoKafkaActualizoElProcesoAntesDeGuardar() {
        ReservationProcess confirmado = proceso(CONFIRMED, haceUnMinuto());
        when(reservationProcessRepository.save(any(ReservationProcess.class)))
                .thenThrow(new ConcurrentReservationUpdateException(ID, null));
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.of(confirmado));

        ReservationProcess resultado = service.expireIfOverdue(proceso(WAITING_FOR_PHONE, haceUnMinuto()));

        assertThat(resultado).isSameAs(confirmado);
    }

    @Test
    void debePropagarElConflicto_cuandoElProcesoYaNoSePuedeReleer() {
        ConcurrentReservationUpdateException conflicto = new ConcurrentReservationUpdateException(ID, null);
        when(reservationProcessRepository.save(any(ReservationProcess.class))).thenThrow(conflicto);
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.expireIfOverdue(proceso(WAITING_FOR_PHONE, haceUnMinuto()))).isSameAs(conflicto);
    }

    private static ReservationProcess proceso(ReservationStatus estado, Instant expiresAt) {
        return ReservationProcess.builder().id(ID).status(estado).expiresAt(expiresAt).build();
    }

    private static Instant enDiezMinutos() {
        return Instant.now().plus(10, ChronoUnit.MINUTES);
    }

    private static Instant haceUnMinuto() {
        return Instant.now().minus(1, ChronoUnit.MINUTES);
    }
}
