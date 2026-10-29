package com.example.turnos_service.reservation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Reserva de un usuario: el proceso local combinado con los datos de la cátedra.
 * Si la cátedra no está disponible, solo tiene los datos locales y {@code source} es LOCAL.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReservation {

    public static final String SOURCE_CATEDRA = "CATEDRA";
    public static final String SOURCE_LOCAL = "LOCAL";

    private Long id;
    private Long reservationId;
    private Long professionalId;
    private String professionalFirstName;
    private String professionalLastName;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private Instant expiresAt;
    private String failureReason;
    private String cancellationReason;
    private String source;
}
