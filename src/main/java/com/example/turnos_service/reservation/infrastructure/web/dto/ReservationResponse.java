package com.example.turnos_service.reservation.infrastructure.web.dto;

import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {

    private Long id;
    private Long professionalId;
    private LocalDate date;
    private LocalTime startTime;
    private ReservationStatus status;
    private Instant expiresAt;
    private String failureReason;
}
