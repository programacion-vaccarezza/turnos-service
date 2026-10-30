package com.example.turnos_service.reservation.infrastructure.web.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateReservationRequest {

    @NotNull(message = "El profesional es obligatorio")
    @Positive(message = "El id del profesional debe ser positivo")
    private Long professionalId;

    @NotNull(message = "La fecha es obligatoria")
    @FutureOrPresent(message = "La fecha no puede ser anterior a hoy")
    private LocalDate date;

    @NotNull(message = "El horario es obligatorio")
    private LocalTime startTime;
}
