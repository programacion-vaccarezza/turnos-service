package com.example.turnos_service.reservation.infrastructure.client.dto;

/**
 * Body de POST /api/appointment-holds (anexo §9). Fecha en yyyy-MM-dd y hora en HH:mm.
 */
public record CreateHoldRequest(Long professionalId, String date, String startTime, String externalPatientId) {
}
