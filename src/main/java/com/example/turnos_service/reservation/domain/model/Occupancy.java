package com.example.turnos_service.reservation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/** Turno no disponible según la cátedra (anexo §8): una reserva confirmada o un hold activo. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Occupancy {

    private LocalDate date;
    private LocalTime startTime;
    private String status;
    private Instant expiresAt;
}
