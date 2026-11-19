package com.example.turnos_service.reservation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// Bloqueo temporal de un turno creado en la cátedra
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hold {

    private String holdId;
    private String reservationProcessId;
    private Instant expiresAt;
}
