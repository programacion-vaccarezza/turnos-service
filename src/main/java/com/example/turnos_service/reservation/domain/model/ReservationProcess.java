package com.example.turnos_service.reservation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationProcess {

    private Long id;
    private String ownerLogin;
    private Long professionalId;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private String patientFirstName;
    private String patientLastName;
    private ReservationStatus status;
    private String holdId;
    private String reservationProcessId;
    private Instant expiresAt;
    private String requestEventId;
    private Long reservationId;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
