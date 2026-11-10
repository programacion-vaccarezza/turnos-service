package com.example.turnos_service.reservation.infrastructure.client.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// Ocupaciones de un profesional devueltas por la cátedra
public record CatedraOccupanciesResponse(Long professionalId, LocalDate from, LocalDate to, List<Item> occupancies) {

    // Un turno ocupado
    public record Item(LocalDate date, LocalTime startTime, String status, Instant expiresAt) {
    }
}
