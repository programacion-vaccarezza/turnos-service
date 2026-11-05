package com.example.turnos_service.reservation.infrastructure.web.dto;

import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReservationResponse {

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
