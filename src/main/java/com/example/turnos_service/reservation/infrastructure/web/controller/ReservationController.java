package com.example.turnos_service.reservation.infrastructure.web.controller;

import com.example.turnos_service.reservation.application.exception.CatalogUnavailableException;
import com.example.turnos_service.reservation.application.exception.CatedraUnavailableException;
import com.example.turnos_service.reservation.application.exception.InvalidStateTransitionException;
import com.example.turnos_service.reservation.application.exception.PatientNotFoundException;
import com.example.turnos_service.reservation.application.exception.ProfessionalDisabledException;
import com.example.turnos_service.reservation.application.exception.ProfessionalNotFoundException;
import com.example.turnos_service.reservation.application.exception.ReservationRejectedException;
import com.example.turnos_service.reservation.application.service.ReservationService;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.infrastructure.web.dto.InitiateReservationRequest;
import com.example.turnos_service.reservation.infrastructure.web.dto.ReservationResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Tag(name = "Reservas", description = "Proceso de reserva de turnos del usuario autenticado")
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class ReservationController {

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
            @ApiResponse(responseCode = "400", description = "Datos inválidos, horario fuera de agenda o profesional deshabilitado"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            @ApiResponse(responseCode = "404", description = "El profesional o el usuario no existen en el catálogo"),
            @ApiResponse(responseCode = "409", description = "El turno ya está tomado, el hold venció o la cátedra rechazó la reserva"),
            @ApiResponse(responseCode = "503", description = "catalogo-service o la cátedra no están disponibles")
    })
    @PostMapping
    public ResponseEntity<ReservationResponse> initiateReservation(@Valid @RequestBody InitiateReservationRequest request,
                                                                   Authentication authentication) {
        String ownerLogin = authentication.getName();
        try {
            ReservationProcess process = reservationService.initiateReservation(
                    ownerLogin, request.getProfessionalId(), request.getDate(), request.getStartTime());
            return new ResponseEntity<>(reservationDtoMapper.toResponse(process), HttpStatus.CREATED);
        } catch (ProfessionalNotFoundException | PatientNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (ProfessionalDisabledException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (ReservationRejectedException e) {
            throw new ResponseStatusException(statusFor(e.getCode()), e.getMessage());
        } catch (InvalidStateTransitionException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (CatalogUnavailableException | CatedraUnavailableException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        }
    }

    private HttpStatus statusFor(String catedraCode) {
        if (catedraCode == null) {
            return HttpStatus.CONFLICT;
        }
        return STATUS_BY_CATEDRA_CODE.getOrDefault(catedraCode, HttpStatus.CONFLICT);
    }
}
