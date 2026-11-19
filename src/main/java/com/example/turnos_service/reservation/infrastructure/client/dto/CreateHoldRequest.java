package com.example.turnos_service.reservation.infrastructure.client.dto;

public record CreateHoldRequest(Long professionalId, String date, String startTime, String externalPatientId) {
}
