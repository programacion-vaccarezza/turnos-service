package com.example.turnos_service.reservation.application.usecases;

import com.example.turnos_service.reservation.application.exception.ConcurrentReservationUpdateException;
import com.example.turnos_service.reservation.application.exception.InvalidPhoneNumberException;
import com.example.turnos_service.reservation.application.exception.InvalidStateTransitionException;
import com.example.turnos_service.reservation.application.exception.PhoneNotRequestedException;
import com.example.turnos_service.reservation.application.exception.PhoneSubmissionFailedException;
import com.example.turnos_service.reservation.application.exception.ReservationNotFoundException;
import com.example.turnos_service.reservation.application.exception.ReservationRejectedException;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.ports.out.PhoneSubmissionPublisher;
import com.example.turnos_service.reservation.domain.ports.out.ReservationProcessRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static com.example.turnos_service.reservation.domain.model.ReservationStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitPhoneUseCaseImplTest {

    private static final String LOGIN = "juan.lopez";
    private static final Long ID = 7L;
    private static final String TELEFONO = "+5492615551234";

    @Mock
    ReservationProcessRepository reservationProcessRepository;

    @Mock
    PhoneSubmissionPublisher phoneSubmissionPublisher;

    @Captor
    ArgumentCaptor<ReservationProcess> processCaptor;

    @Captor
    ArgumentCaptor<String> eventIdCaptor;

    @InjectMocks
    SubmitPhoneUseCaseImpl useCase;

    @Test
    void debePublicarElTelefonoYPasarAPhoneSubmitted_cuandoLaCatedraLoPidio() {
        ReservationProcess process = proceso(WAITING_FOR_PHONE, enDiezMinutos());
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.of(process));
        when(reservationProcessRepository.save(any(ReservationProcess.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationProcess resultado = useCase.submitPhone(LOGIN, ID, TELEFONO);

        verify(phoneSubmissionPublisher).publishPhone(eq(process), eventIdCaptor.capture(), eq(TELEFONO));
        assertThat(eventIdCaptor.getValue()).startsWith("phone-submitted-");
        assertThat(resultado.getStatus()).isEqualTo(PHONE_SUBMITTED);
    }

    @Test
    void debePermitirReenviarElTelefonoYLimpiarElMotivo_cuandoLaCatedraLoRechazo() {
        ReservationProcess process = proceso(PHONE_REJECTED, enDiezMinutos());
        process.setFailureReason("INVALID_PHONE");
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.of(process));
        when(reservationProcessRepository.save(any(ReservationProcess.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationProcess resultado = useCase.submitPhone(LOGIN, ID, TELEFONO);

        verify(phoneSubmissionPublisher).publishPhone(eq(process), anyString(), eq(TELEFONO));
        assertThat(resultado)
                .extracting(ReservationProcess::getStatus, ReservationProcess::getFailureReason)
                .containsExactly(PHONE_SUBMITTED, null);
    }

    @Test
    void debePublicarElTelefonoNormalizado_cuandoTraeEspaciosGuionesYParentesis() {
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.of(proceso(WAITING_FOR_PHONE, enDiezMinutos())));
        when(reservationProcessRepository.save(any(ReservationProcess.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.submitPhone(LOGIN, ID, "+54 9 (261) 555-1234");

        verify(phoneSubmissionPublisher).publishPhone(any(), anyString(), eq(TELEFONO));
    }

    @Test
    void debeResponderNoEncontrado_cuandoElProcesoEsDeOtroUsuario() {
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.of(proceso(WAITING_FOR_PHONE, enDiezMinutos())));

        assertThatThrownBy(() -> useCase.submitPhone("maria.prueba", ID, TELEFONO))
                .isInstanceOf(ReservationNotFoundException.class);
        verifyNoInteractions(phoneSubmissionPublisher);
    }

    @Test
    void debeResponderNoEncontrado_cuandoElProcesoNoExiste() {
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.submitPhone(LOGIN, ID, TELEFONO))
                .isInstanceOf(ReservationNotFoundException.class);
        verifyNoInteractions(phoneSubmissionPublisher);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12345", "+54926155512345678", "261-555-12a4", ""})
    void debeRechazarElTelefono_cuandoNoTieneElFormatoDeLaCatedra(String telefono) {
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.of(proceso(WAITING_FOR_PHONE, enDiezMinutos())));

        assertThatThrownBy(() -> useCase.submitPhone(LOGIN, ID, telefono))
                .isInstanceOf(InvalidPhoneNumberException.class);
        verifyNoInteractions(phoneSubmissionPublisher);
    }

    @Test
    void debePasarAExpiredSinPublicar_cuandoYaPasoElVencimiento() {
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.of(proceso(WAITING_FOR_PHONE, haceUnMinuto())));

        assertThatThrownBy(() -> useCase.submitPhone(LOGIN, ID, TELEFONO))
                .isInstanceOf(ReservationRejectedException.class)
                .extracting("code").isEqualTo("PROCESS_EXPIRED");

        verify(reservationProcessRepository).save(processCaptor.capture());
        assertThat(processCaptor.getValue())
                .extracting(ReservationProcess::getStatus, ReservationProcess::getFailureReason)
                .containsExactly(EXPIRED, "PROCESS_EXPIRED");
        verifyNoInteractions(phoneSubmissionPublisher);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONFIRMING", "PHONE_SUBMITTED", "CONFIRMED", "CANCELLED", "FAILED"})
    void debeRechazarSinPublicar_cuandoElEstadoNoPermiteEnviarElTelefono(String estado) {
        when(reservationProcessRepository.findById(ID))
                .thenReturn(Optional.of(proceso(ReservationStatus.valueOf(estado), enDiezMinutos())));

        assertThatThrownBy(() -> useCase.submitPhone(LOGIN, ID, TELEFONO))
                .isInstanceOf(InvalidStateTransitionException.class);
        verifyNoInteractions(phoneSubmissionPublisher);
        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void debeRechazarSinPublicar_cuandoLaCatedraTodaviaNoPidioElTelefono() {
        ReservationProcess process = proceso(WAITING_FOR_PHONE, enDiezMinutos());
        process.setRequestEventId(null);
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.of(process));

        assertThatThrownBy(() -> useCase.submitPhone(LOGIN, ID, TELEFONO))
                .isInstanceOf(PhoneNotRequestedException.class);
        verifyNoInteractions(phoneSubmissionPublisher);
    }

    @Test
    void noDebeCambiarElEstado_cuandoFallaLaPublicacionEnKafka() {
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.of(proceso(WAITING_FOR_PHONE, enDiezMinutos())));
        doThrow(new PhoneSubmissionFailedException("kafka caído", null))
                .when(phoneSubmissionPublisher).publishPhone(any(), anyString(), anyString());

        assertThatThrownBy(() -> useCase.submitPhone(LOGIN, ID, TELEFONO))
                .isInstanceOf(PhoneSubmissionFailedException.class);
        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void debeDevolverElEstadoQueDejoElEvento_cuandoKafkaActualizoElProcesoAntesDeGuardar() {
        ReservationProcess confirmado = proceso(CONFIRMED, enDiezMinutos());
        when(reservationProcessRepository.findById(ID))
                .thenReturn(Optional.of(proceso(WAITING_FOR_PHONE, enDiezMinutos())), Optional.of(confirmado));
        when(reservationProcessRepository.save(any(ReservationProcess.class)))
                .thenThrow(new ConcurrentReservationUpdateException(ID, null));

        ReservationProcess resultado = useCase.submitPhone(LOGIN, ID, TELEFONO);

        assertThat(resultado.getStatus()).isEqualTo(CONFIRMED);
    }

    private static ReservationProcess proceso(ReservationStatus estado, Instant expiresAt) {
        return ReservationProcess.builder()
                .id(ID).ownerLogin(LOGIN).reservationProcessId("process-a92b47d0").holdId("hold-6c0c2f41")
                .requestEventId("request-event-8a31").status(estado).expiresAt(expiresAt).build();
    }

    private static Instant enDiezMinutos() {
        return Instant.now().plus(10, ChronoUnit.MINUTES);
    }

    private static Instant haceUnMinuto() {
        return Instant.now().minus(1, ChronoUnit.MINUTES);
    }
}
