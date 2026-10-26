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
import com.example.turnos_service.reservation.infrastructure.web.dto.ErrorResponse;
import com.example.turnos_service.reservation.infrastructure.web.dto.InitiateReservationRequest;
import com.example.turnos_service.reservation.infrastructure.web.dto.SubmitPhoneRequest;
import com.example.turnos_service.reservation.infrastructure.web.mapper.ReservationDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Reservas", description = "Proceso de reserva de turnos del usuario autenticado")
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class ReservationController {

    private static final String PROFESSIONAL_NOT_FOUND = "PROFESSIONAL_NOT_FOUND";
    private static final String PATIENT_NOT_FOUND = "PATIENT_NOT_FOUND";
    private static final String PROFESSIONAL_DISABLED = "PROFESSIONAL_DISABLED";
    private static final String INVALID_STATE_TRANSITION = "INVALID_STATE_TRANSITION";
    private static final String CONCURRENT_UPDATE = "CONCURRENT_UPDATE";
    private static final String CATALOG_UNAVAILABLE = "CATALOG_UNAVAILABLE";
    private static final String CATEDRA_UNAVAILABLE = "CATEDRA_UNAVAILABLE";
    private static final String CATEDRA_REJECTED = "CATEDRA_REJECTED";
    private static final String RESERVATION_NOT_FOUND = "RESERVATION_NOT_FOUND";
    private static final String INVALID_PHONE_NUMBER = "INVALID_PHONE_NUMBER";
    private static final String PHONE_NOT_REQUESTED = "PHONE_NOT_REQUESTED";
    private static final String PHONE_SUBMISSION_FAILED = "PHONE_SUBMISSION_FAILED";

    /** Código HTTP para cada código de rechazo de la cátedra (anexo §13.1); el resto responde 409. */
    private static final Map<String, HttpStatus> STATUS_BY_CATEDRA_CODE = Map.of(
            "INVALID_SLOT", HttpStatus.BAD_REQUEST,
            "PROFESSIONAL_DISABLED", HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR", HttpStatus.BAD_REQUEST,
            "PROFESSIONAL_NOT_FOUND", HttpStatus.NOT_FOUND);

    private final ReservationService reservationService;
    private final ReservationDtoMapper reservationDtoMapper;

    @Operation(summary = "Iniciar una reserva: crea el hold en la cátedra y lo confirma, dejando el proceso a la espera del teléfono")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Proceso creado. status WAITING_FOR_PHONE, o CONFIRMING si la cátedra no respondió a tiempo"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos. code: INVALID_SLOT, PROFESSIONAL_DISABLED"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "404", description = "code: PROFESSIONAL_NOT_FOUND, PATIENT_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "code: SLOT_ALREADY_HELD, SLOT_ALREADY_RESERVED, HOLD_EXPIRED, INVALID_STATE_TRANSITION, CONCURRENT_UPDATE u otro código de rechazo de la cátedra"),
            @ApiResponse(responseCode = "503", description = "code: CATALOG_UNAVAILABLE, CATEDRA_UNAVAILABLE")
    })
    @PostMapping
    public ResponseEntity<?> initiateReservation(@Valid @RequestBody InitiateReservationRequest request,
                                                 Authentication authentication) {
        String ownerLogin = authentication.getName();
        try {
            ReservationProcess process = reservationService.initiateReservation(
                    ownerLogin, request.getProfessionalId(), request.getDate(), request.getStartTime());
            return new ResponseEntity<>(reservationDtoMapper.toResponse(process), HttpStatus.CREATED);
        } catch (ProfessionalNotFoundException e) {
            return error(HttpStatus.NOT_FOUND, PROFESSIONAL_NOT_FOUND);
        } catch (PatientNotFoundException e) {
            return error(HttpStatus.NOT_FOUND, PATIENT_NOT_FOUND);
        } catch (ProfessionalDisabledException e) {
            return error(HttpStatus.BAD_REQUEST, PROFESSIONAL_DISABLED);
        } catch (ReservationRejectedException e) {
            String code = e.getCode() != null ? e.getCode() : CATEDRA_REJECTED;
            return error(STATUS_BY_CATEDRA_CODE.getOrDefault(code, HttpStatus.CONFLICT), code);
        } catch (InvalidStateTransitionException e) {
            return error(HttpStatus.CONFLICT, INVALID_STATE_TRANSITION);
        } catch (ConcurrentReservationUpdateException e) {
            return error(HttpStatus.CONFLICT, CONCURRENT_UPDATE);
        } catch (CatalogUnavailableException e) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, CATALOG_UNAVAILABLE);
        } catch (CatedraUnavailableException e) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, CATEDRA_UNAVAILABLE);
        }
    }

    @Operation(summary = "Enviar el teléfono que pidió la cátedra para una reserva propia")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Teléfono enviado. status PHONE_SUBMITTED; el resultado final llega por Kafka"),
            @ApiResponse(responseCode = "400", description = "code: INVALID_PHONE_NUMBER"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "404", description = "code: RESERVATION_NOT_FOUND (no existe o no es del usuario)"),
            @ApiResponse(responseCode = "409", description = "code: PHONE_NOT_REQUESTED, PROCESS_EXPIRED, INVALID_STATE_TRANSITION, CONCURRENT_UPDATE"),
            @ApiResponse(responseCode = "503", description = "code: PHONE_SUBMISSION_FAILED")
    })
    @PostMapping("/{id}/phone")
    public ResponseEntity<?> submitPhone(@PathVariable Long id, @Valid @RequestBody SubmitPhoneRequest request,
                                         Authentication authentication) {
        try {
            ReservationProcess process = reservationService.submitPhone(authentication.getName(), id, request.getPhoneNumber());
            return new ResponseEntity<>(reservationDtoMapper.toResponse(process), HttpStatus.ACCEPTED);
        } catch (ReservationNotFoundException e) {
            return error(HttpStatus.NOT_FOUND, RESERVATION_NOT_FOUND);
        } catch (InvalidPhoneNumberException e) {
            return error(HttpStatus.BAD_REQUEST, INVALID_PHONE_NUMBER);
        } catch (PhoneNotRequestedException e) {
            return error(HttpStatus.CONFLICT, PHONE_NOT_REQUESTED);
        } catch (ReservationRejectedException e) {
            return error(HttpStatus.CONFLICT, e.getCode());
        } catch (InvalidStateTransitionException e) {
            return error(HttpStatus.CONFLICT, INVALID_STATE_TRANSITION);
        } catch (ConcurrentReservationUpdateException e) {
            return error(HttpStatus.CONFLICT, CONCURRENT_UPDATE);
        } catch (PhoneSubmissionFailedException e) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, PHONE_SUBMISSION_FAILED);
        }
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code) {
        return ResponseEntity.status(status).body(new ErrorResponse(code));
    }
}
