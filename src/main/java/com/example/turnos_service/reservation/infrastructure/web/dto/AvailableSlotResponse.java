package com.example.turnos_service.reservation.infrastructure.web.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

// Turno libre que se devuelve a la app
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableSlotResponse {

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
}
