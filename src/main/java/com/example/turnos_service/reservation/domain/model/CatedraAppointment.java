package com.example.turnos_service.reservation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/** Reserva tal como la informa la cátedra en GET /api/appointments (anexo §11). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatedraAppointment {

    private Long reservationId;
    private String reservationProcessId;
    private String externalPatientId;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long professionalId;
    private String professionalFirstName;
    private String professionalLastName;
    private String status;
    private Instant createdAt;
    private Instant confirmedAt;
    private Instant cancelledAt;
    private String cancellationReason;
}
