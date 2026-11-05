package com.example.turnos_service.reservation.infrastructure.web.controller;

import com.example.turnos_service.reservation.application.exception.CatalogUnavailableException;
import com.example.turnos_service.reservation.application.exception.CatedraUnavailableException;
import com.example.turnos_service.reservation.application.exception.ConcurrentReservationUpdateException;
import com.example.turnos_service.reservation.application.exception.InvalidPhoneNumberException;
import com.example.turnos_service.reservation.application.exception.InvalidStateTransitionException;
import com.example.turnos_service.reservation.application.exception.PatientNotFoundException;
import com.example.turnos_service.reservation.application.exception.PhoneNotRequestedException;
import com.example.turnos_service.reservation.application.exception.PhoneSubmissionFailedException;
import com.example.turnos_service.reservation.application.exception.ProfessionalDisabledException;
import com.example.turnos_service.reservation.application.exception.ProfessionalNotFoundException;
import com.example.turnos_service.reservation.application.exception.ReservationNotFoundException;
import com.example.turnos_service.reservation.application.exception.ReservationRejectedException;
import com.example.turnos_service.reservation.application.service.ReservationService;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.infrastructure.web.dto.ErrorResponse;
import com.example.turnos_service.reservation.infrastructure.web.dto.InitiateReservationRequest;
import com.example.turnos_service.reservation.infrastructure.web.dto.ReservationResponse;
import com.example.turnos_service.reservation.infrastructure.web.dto.SubmitPhoneRequest;
import com.example.turnos_service.reservation.infrastructure.web.mapper.ReservationDtoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.stream.Stream;

import static com.example.turnos_service.reservation.domain.model.ReservationStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationControllerTest {

    private static final String LOGIN = "juan.lopez";
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 28);
    private static final LocalTime HORA = LocalTime.of(17, 30);

    @Mock
    ReservationService reservationService;

    @Mock
    Authentication authentication;

    private ReservationController controller;
    private InitiateReservationRequest request;

    @BeforeEach
    void setUp() {
        controller = new ReservationController(reservationService, new ReservationDtoMapper());
        request = new InitiateReservationRequest(201L, FECHA, HORA);
        when(authentication.getName()).thenReturn(LOGIN);
    }

    @Test
    void debeResponder201ConElProceso_cuandoLaReservaSeInicia() {
        ReservationProcess process = ReservationProcess.builder()
                .id(2L).professionalId(201L).appointmentDate(FECHA).startTime(HORA)
                .status(WAITING_FOR_PHONE).expiresAt(Instant.parse("2026-10-24T19:22:19Z")).build();
        when(reservationService.initiateReservation(LOGIN, 201L, FECHA, HORA)).thenReturn(process);

        ResponseEntity<?> response = controller.initiateReservation(request, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isInstanceOf(ReservationResponse.class);
        assertThat(((ReservationResponse) response.getBody()).getStatus()).isEqualTo(WAITING_FOR_PHONE);
    }

    @Test
    void debeUsarElLoginDelUsuarioAutenticado_cuandoIniciaLaReserva() {
        when(reservationService.initiateReservation(LOGIN, 201L, FECHA, HORA))
                .thenReturn(ReservationProcess.builder().id(2L).status(WAITING_FOR_PHONE).build());

        controller.initiateReservation(request, authentication);

        verify(reservationService).initiateReservation(LOGIN, 201L, FECHA, HORA);
    }

    static Stream<Arguments> erroresYRespuestas() {
        return Stream.of(
                Arguments.of(new ProfessionalNotFoundException(201L), HttpStatus.NOT_FOUND, "PROFESSIONAL_NOT_FOUND"),
                Arguments.of(new PatientNotFoundException(LOGIN), HttpStatus.NOT_FOUND, "PATIENT_NOT_FOUND"),
                Arguments.of(new ProfessionalDisabledException(201L), HttpStatus.BAD_REQUEST, "PROFESSIONAL_DISABLED"),
                Arguments.of(rechazo(FAILED, "INVALID_SLOT"), HttpStatus.BAD_REQUEST, "INVALID_SLOT"),
                Arguments.of(rechazo(FAILED, "PROFESSIONAL_DISABLED"), HttpStatus.BAD_REQUEST, "PROFESSIONAL_DISABLED"),
                Arguments.of(rechazo(FAILED, "VALIDATION_ERROR"), HttpStatus.BAD_REQUEST, "VALIDATION_ERROR"),
                Arguments.of(rechazo(FAILED, "PROFESSIONAL_NOT_FOUND"), HttpStatus.NOT_FOUND, "PROFESSIONAL_NOT_FOUND"),
                Arguments.of(rechazo(FAILED, "SLOT_ALREADY_HELD"), HttpStatus.CONFLICT, "SLOT_ALREADY_HELD"),
                Arguments.of(rechazo(FAILED, "SLOT_ALREADY_RESERVED"), HttpStatus.CONFLICT, "SLOT_ALREADY_RESERVED"),
                Arguments.of(rechazo(EXPIRED, "HOLD_EXPIRED"), HttpStatus.CONFLICT, "HOLD_EXPIRED"),
                Arguments.of(rechazo(FAILED, "HOLD_NOT_FOUND"), HttpStatus.CONFLICT, "HOLD_NOT_FOUND"),
                Arguments.of(rechazo(FAILED, null), HttpStatus.CONFLICT, "CATEDRA_REJECTED"),
                Arguments.of(new InvalidStateTransitionException(CONFIRMED, HOLD_CREATED), HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION"),
                Arguments.of(new CatalogUnavailableException("catalogo caído", null), HttpStatus.SERVICE_UNAVAILABLE, "CATALOG_UNAVAILABLE"),
                Arguments.of(new CatedraUnavailableException("cátedra caída", null), HttpStatus.SERVICE_UNAVAILABLE, "CATEDRA_UNAVAILABLE")
        );
    }

    @ParameterizedTest(name = "{2} -> {1}")
    @MethodSource("erroresYRespuestas")
    void debeResponderElCodigoHttpYElCode_cuandoLaReservaFalla(RuntimeException error, HttpStatus status, String code) {
        when(reservationService.initiateReservation(LOGIN, 201L, FECHA, HORA)).thenThrow(error);

        ResponseEntity<?> response = controller.initiateReservation(request, authentication);

        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
        assertThat(((ErrorResponse) response.getBody()).getCode()).isEqualTo(code);
    }

    @Test
    void debeResponder202ConElProceso_cuandoElTelefonoSeEnvia() {
        when(reservationService.submitPhone(LOGIN, 2L, "+5492615551234"))
                .thenReturn(ReservationProcess.builder().id(2L).status(PHONE_SUBMITTED).build());

        ResponseEntity<?> response = controller.submitPhone(2L, new SubmitPhoneRequest("+5492615551234"), authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(((ReservationResponse) response.getBody()).getStatus()).isEqualTo(PHONE_SUBMITTED);
        verify(reservationService).submitPhone(LOGIN, 2L, "+5492615551234");
    }

    static Stream<Arguments> erroresDelTelefono() {
        return Stream.of(
                Arguments.of(new ReservationNotFoundException(2L), HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND"),
                Arguments.of(new InvalidPhoneNumberException(), HttpStatus.BAD_REQUEST, "INVALID_PHONE_NUMBER"),
                Arguments.of(new PhoneNotRequestedException(2L), HttpStatus.CONFLICT, "PHONE_NOT_REQUESTED"),
                Arguments.of(rechazo(EXPIRED, "PROCESS_EXPIRED"), HttpStatus.CONFLICT, "PROCESS_EXPIRED"),
                Arguments.of(new InvalidStateTransitionException(CONFIRMED, PHONE_SUBMITTED), HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION"),
                Arguments.of(new ConcurrentReservationUpdateException(2L, null), HttpStatus.CONFLICT, "CONCURRENT_UPDATE"),
                Arguments.of(new PhoneSubmissionFailedException("kafka caído", null), HttpStatus.SERVICE_UNAVAILABLE, "PHONE_SUBMISSION_FAILED")
        );
    }

    @ParameterizedTest(name = "{2} -> {1}")
    @MethodSource("erroresDelTelefono")
    void debeResponderElCodigoHttpYElCode_cuandoElEnvioDelTelefonoFalla(RuntimeException error, HttpStatus status, String code) {
        when(reservationService.submitPhone(LOGIN, 2L, "+5492615551234")).thenThrow(error);

        ResponseEntity<?> response = controller.submitPhone(2L, new SubmitPhoneRequest("+5492615551234"), authentication);

        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(((ErrorResponse) response.getBody()).getCode()).isEqualTo(code);
    }

    private static ReservationRejectedException rechazo(ReservationStatus status, String code) {
        return new ReservationRejectedException(3L, status, code);
    }
}
