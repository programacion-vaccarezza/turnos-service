package com.example.turnos_service.reservation.application.usecases;

import com.example.turnos_service.reservation.application.exception.CatedraUnavailableException;
import com.example.turnos_service.reservation.application.exception.ReservationNotFoundException;
import com.example.turnos_service.reservation.application.service.ReservationExpirationService;
import com.example.turnos_service.reservation.application.service.ReservationReconciler;
import com.example.turnos_service.reservation.domain.model.CatedraAppointment;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.ports.out.CatedraAppointmentPort;
import com.example.turnos_service.reservation.domain.ports.out.ReservationProcessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static com.example.turnos_service.reservation.domain.model.ReservationStatus.CONFIRMED;
import static com.example.turnos_service.reservation.domain.model.ReservationStatus.CONFIRMING;
import static com.example.turnos_service.reservation.domain.model.ReservationStatus.EXPIRED;
import static com.example.turnos_service.reservation.domain.model.ReservationStatus.PHONE_SUBMITTED;
import static com.example.turnos_service.reservation.domain.model.ReservationStatus.WAITING_FOR_PHONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetReservationUseCaseImplTest {

    private static final String LOGIN = "juan.lopez";
    private static final Long ID = 57L;
    private static final String PROCESO = "process-1";

    @Mock
    ReservationProcessRepository reservationProcessRepository;

    @Mock
    CatedraAppointmentPort catedraAppointmentPort;

    GetReservationUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetReservationUseCaseImpl(reservationProcessRepository,
                new ReservationExpirationService(reservationProcessRepository), catedraAppointmentPort,
                new ReservationReconciler(reservationProcessRepository));
    }

    @Test
    void debeDevolverElEstadoLocalSinConsultarALaCatedra_cuandoElProcesoNoVencio() {
        ReservationProcess process = proceso(WAITING_FOR_PHONE, enDiezMinutos());
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.of(process));

        assertThat(useCase.getReservation(LOGIN, ID)).isSameAs(process);

        verifyNoInteractions(catedraAppointmentPort);
        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void debeResponderNoEncontrado_cuandoElProcesoEsDeOtroUsuario() {
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.of(proceso(CONFIRMED, enDiezMinutos())));

        assertThatThrownBy(() -> useCase.getReservation("maria.prueba", ID))
                .isInstanceOf(ReservationNotFoundException.class);

        verifyNoInteractions(catedraAppointmentPort);
    }

    @Test
    void debeResponderNoEncontrado_cuandoElProcesoNoExiste() {
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getReservation(LOGIN, ID))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    void debeVencerElProcesoSinConsultarALaCatedra_cuandoVencioMientrasEsperabaAlUsuario() {
        ReservationProcess process = proceso(WAITING_FOR_PHONE, haceUnMinuto());
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.of(process));
        when(reservationProcessRepository.save(process)).thenReturn(process);

        ReservationProcess resultado = useCase.getReservation(LOGIN, ID);

        assertThat(resultado)
                .extracting(ReservationProcess::getStatus, ReservationProcess::getFailureReason)
                .containsExactly(EXPIRED, "PROCESS_EXPIRED");
        verifyNoInteractions(catedraAppointmentPort);
    }

    @Test
    void debeTomarElEstadoDeLaCatedra_cuandoVencioConUnResultadoDesconocido() {
        ReservationProcess process = proceso(PHONE_SUBMITTED, haceUnMinuto());
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.of(process));
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN)).thenReturn(List.of(
                turno("process-de-otra-reserva", "CANCELLED"), turno(PROCESO, "CONFIRMED")));
        when(reservationProcessRepository.save(process)).thenReturn(process);

        ReservationProcess resultado = useCase.getReservation(LOGIN, ID);

        assertThat(resultado)
                .extracting(ReservationProcess::getStatus, ReservationProcess::getReservationId)
                .containsExactly(CONFIRMED, 1502L);
    }

    @Test
    void debeVencerElProceso_cuandoVencioConUnResultadoDesconocidoYLaCatedraNoTieneLaReserva() {
        ReservationProcess process = proceso(CONFIRMING, haceUnMinuto());
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.of(process));
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN)).thenReturn(List.of());
        when(reservationProcessRepository.save(process)).thenReturn(process);

        ReservationProcess resultado = useCase.getReservation(LOGIN, ID);

        assertThat(resultado)
                .extracting(ReservationProcess::getStatus, ReservationProcess::getFailureReason)
                .containsExactly(EXPIRED, "PROCESS_EXPIRED");
    }

    @Test
    void noDebeConsultarALaCatedra_cuandoElResultadoEsDesconocidoPeroTodaviaNoVencio() {
        ReservationProcess process = proceso(PHONE_SUBMITTED, enDiezMinutos());
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.of(process));

        assertThat(useCase.getReservation(LOGIN, ID).getStatus()).isEqualTo(PHONE_SUBMITTED);

        verifyNoInteractions(catedraAppointmentPort);
    }

    @Test
    void debeDevolverElEstadoLocal_cuandoLaCatedraNoEstaDisponible() {
        ReservationProcess process = proceso(CONFIRMING, haceUnMinuto());
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.of(process));
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN))
                .thenThrow(new CatedraUnavailableException("sin conexión", null));

        assertThat(useCase.getReservation(LOGIN, ID).getStatus()).isEqualTo(CONFIRMING);

        verify(reservationProcessRepository, never()).save(any());
    }

    private static ReservationProcess proceso(ReservationStatus estado, Instant expiresAt) {
        return ReservationProcess.builder()
                .id(ID).ownerLogin(LOGIN).reservationProcessId(PROCESO).status(estado).expiresAt(expiresAt).build();
    }

    private static CatedraAppointment turno(String reservationProcessId, String estado) {
        return CatedraAppointment.builder().reservationId(1502L).reservationProcessId(reservationProcessId).status(estado).build();
    }

    private static Instant enDiezMinutos() {
        return Instant.now().plus(10, ChronoUnit.MINUTES);
    }

    private static Instant haceUnMinuto() {
        return Instant.now().minus(1, ChronoUnit.MINUTES);
    }
}
