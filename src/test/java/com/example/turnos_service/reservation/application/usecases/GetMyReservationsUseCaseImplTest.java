package com.example.turnos_service.reservation.application.usecases;

import com.example.turnos_service.reservation.application.exception.CatedraUnavailableException;
import com.example.turnos_service.reservation.application.service.ReservationExpirationService;
import com.example.turnos_service.reservation.domain.model.CatedraAppointment;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.model.UserReservation;
import com.example.turnos_service.reservation.domain.ports.out.CatedraAppointmentPort;
import com.example.turnos_service.reservation.domain.ports.out.ReservationProcessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.example.turnos_service.reservation.domain.model.ReservationStatus.CONFIRMED;
import static com.example.turnos_service.reservation.domain.model.ReservationStatus.EXPIRED;
import static com.example.turnos_service.reservation.domain.model.ReservationStatus.FAILED;
import static com.example.turnos_service.reservation.domain.model.ReservationStatus.WAITING_FOR_PHONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMyReservationsUseCaseImplTest {

    private static final String LOGIN = "juan.lopez";
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 20);
    private static final LocalTime INICIO = LocalTime.of(18, 0);
    private static final LocalTime FIN = LocalTime.of(18, 30);

    @Mock
    ReservationProcessRepository reservationProcessRepository;

    @Mock
    CatedraAppointmentPort catedraAppointmentPort;

    GetMyReservationsUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetMyReservationsUseCaseImpl(reservationProcessRepository, catedraAppointmentPort,
                new ReservationExpirationService(reservationProcessRepository));
    }

    @Test
    void debeCombinarElProcesoLocalConLosDatosDeLaCatedra_cuandoLaCatedraResponde() {
        Instant vencimiento = enDiezMinutos();
        ReservationProcess process = proceso(57L, "process-1", CONFIRMED, vencimiento);
        when(reservationProcessRepository.findByOwnerLogin(LOGIN)).thenReturn(List.of(process));
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN)).thenReturn(List.of(turno("process-1")));

        List<UserReservation> resultado = useCase.getMyReservations(LOGIN);

        assertThat(resultado).singleElement()
                .extracting(UserReservation::getId, UserReservation::getReservationId,
                        UserReservation::getProfessionalId, UserReservation::getProfessionalFirstName,
                        UserReservation::getProfessionalLastName, UserReservation::getDate,
                        UserReservation::getStartTime, UserReservation::getEndTime, UserReservation::getStatus,
                        UserReservation::getExpiresAt, UserReservation::getCancellationReason, UserReservation::getSource)
                .containsExactly(57L, 1502L, 202L, "Martin", "Rivas", FECHA, INICIO, FIN, "CANCELLED",
                        vencimiento, "Cambio de horario", UserReservation.SOURCE_CATEDRA);
    }

    @Test
    void debeDevolverSoloLasReservasPropias_cuandoLaCatedraListaLasDeOtroUsuario() {
        when(reservationProcessRepository.findByOwnerLogin(LOGIN))
                .thenReturn(List.of(proceso(57L, "process-1", CONFIRMED, enDiezMinutos())));
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN))
                .thenReturn(List.of(turno("process-1"), turno("process-de-otro-usuario")));

        List<UserReservation> resultado = useCase.getMyReservations(LOGIN);

        assertThat(resultado).extracting(UserReservation::getId).containsExactly(57L);
    }

    @Test
    void debeConsultarConElLoginRecibido_cuandoBuscaLasReservas() {
        when(reservationProcessRepository.findByOwnerLogin(LOGIN)).thenReturn(List.of());
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN)).thenReturn(List.of());

        assertThat(useCase.getMyReservations(LOGIN)).isEmpty();

        verify(reservationProcessRepository).findByOwnerLogin(LOGIN);
        verify(catedraAppointmentPort).findByExternalPatientId(LOGIN);
    }

    @Test
    void debeOmitirElProcesoLocal_cuandoLaCatedraNoLoLista() {
        when(reservationProcessRepository.findByOwnerLogin(LOGIN))
                .thenReturn(List.of(proceso(57L, "process-1", WAITING_FOR_PHONE, enDiezMinutos())));
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN)).thenReturn(List.of());

        assertThat(useCase.getMyReservations(LOGIN)).isEmpty();
    }

    @Test
    void noDebeFallar_cuandoUnProcesoNoTieneIdDeLaCatedra() {
        when(reservationProcessRepository.findByOwnerLogin(LOGIN)).thenReturn(List.of(
                proceso(56L, null, FAILED, null),
                proceso(55L, null, FAILED, null),
                proceso(57L, "process-1", CONFIRMED, enDiezMinutos())));
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN)).thenReturn(List.of(turno("process-1")));

        assertThat(useCase.getMyReservations(LOGIN)).extracting(UserReservation::getId).containsExactly(57L);
    }

    @Test
    void debeDevolverElEstadoLocal_cuandoLaCatedraNoEstaDisponible() {
        Instant vencimiento = enDiezMinutos();
        ReservationProcess process = proceso(57L, "process-1", WAITING_FOR_PHONE, vencimiento);
        process.setReservationId(1502L);
        when(reservationProcessRepository.findByOwnerLogin(LOGIN)).thenReturn(List.of(process));
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN))
                .thenThrow(new CatedraUnavailableException("sin conexión", null));

        List<UserReservation> resultado = useCase.getMyReservations(LOGIN);

        assertThat(resultado).singleElement()
                .extracting(UserReservation::getId, UserReservation::getReservationId,
                        UserReservation::getProfessionalId, UserReservation::getDate, UserReservation::getStartTime,
                        UserReservation::getEndTime, UserReservation::getProfessionalFirstName,
                        UserReservation::getStatus, UserReservation::getExpiresAt, UserReservation::getSource)
                .containsExactly(57L, 1502L, 202L, FECHA, INICIO, null, null, "WAITING_FOR_PHONE",
                        vencimiento, UserReservation.SOURCE_LOCAL);
    }

    @Test
    void debeIncluirLosProcesosSinIdDeLaCatedra_cuandoLaCatedraNoEstaDisponible() {
        ReservationProcess fallido = proceso(56L, null, FAILED, null);
        fallido.setFailureReason("CATEDRA_UNAVAILABLE");
        when(reservationProcessRepository.findByOwnerLogin(LOGIN)).thenReturn(List.of(fallido));
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN))
                .thenThrow(new CatedraUnavailableException("sin conexión", null));

        List<UserReservation> resultado = useCase.getMyReservations(LOGIN);

        assertThat(resultado).singleElement()
                .extracting(UserReservation::getId, UserReservation::getStatus, UserReservation::getFailureReason)
                .containsExactly(56L, "FAILED", "CATEDRA_UNAVAILABLE");
    }

    @Test
    void debeVencerElProcesoAntesDeResponder_cuandoYaPasoElVencimiento() {
        ReservationProcess process = proceso(57L, "process-1", WAITING_FOR_PHONE, haceUnMinuto());
        when(reservationProcessRepository.findByOwnerLogin(LOGIN)).thenReturn(List.of(process));
        when(reservationProcessRepository.save(any(ReservationProcess.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN))
                .thenThrow(new CatedraUnavailableException("sin conexión", null));

        List<UserReservation> resultado = useCase.getMyReservations(LOGIN);

        verify(reservationProcessRepository).save(process);
        assertThat(process.getStatus()).isEqualTo(EXPIRED);
        assertThat(resultado).singleElement()
                .extracting(UserReservation::getStatus, UserReservation::getFailureReason)
                .containsExactly("EXPIRED", "PROCESS_EXPIRED");
    }

    @Test
    void noDebeGuardarNada_cuandoNingunProcesoEstaVencido() {
        when(reservationProcessRepository.findByOwnerLogin(LOGIN))
                .thenReturn(List.of(proceso(57L, "process-1", WAITING_FOR_PHONE, enDiezMinutos())));
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN)).thenReturn(List.of(turno("process-1")));

        useCase.getMyReservations(LOGIN);

        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void noDebeVencerElProceso_cuandoElResultadoEsDesconocido() {
        ReservationProcess process = proceso(57L, "process-1", ReservationStatus.PHONE_SUBMITTED, haceUnMinuto());
        when(reservationProcessRepository.findByOwnerLogin(LOGIN)).thenReturn(List.of(process));
        when(catedraAppointmentPort.findByExternalPatientId(LOGIN)).thenReturn(List.of());

        useCase.getMyReservations(LOGIN);

        assertThat(process.getStatus()).isEqualTo(ReservationStatus.PHONE_SUBMITTED);
        verify(reservationProcessRepository, never()).save(any());
    }

    private static ReservationProcess proceso(Long id, String reservationProcessId, ReservationStatus estado, Instant expiresAt) {
        return ReservationProcess.builder()
                .id(id).ownerLogin(LOGIN).professionalId(202L).appointmentDate(FECHA).startTime(INICIO)
                .reservationProcessId(reservationProcessId).status(estado).expiresAt(expiresAt).build();
    }

    private static CatedraAppointment turno(String reservationProcessId) {
        return CatedraAppointment.builder()
                .reservationId(1502L).reservationProcessId(reservationProcessId).externalPatientId(LOGIN)
                .appointmentDate(FECHA).startTime(INICIO).endTime(FIN)
                .professionalId(202L).professionalFirstName("Martin").professionalLastName("Rivas")
                .status("CANCELLED").cancellationReason("Cambio de horario").build();
    }

    private static Instant enDiezMinutos() {
        return Instant.now().plus(10, ChronoUnit.MINUTES);
    }

    private static Instant haceUnMinuto() {
        return Instant.now().minus(1, ChronoUnit.MINUTES);
    }
}
