package com.example.turnos_service.reservation.infrastructure.client.dto;

// Body para crear un hold en la cátedra
public record CreateHoldRequest(Long professionalId, String date, String startTime, String externalPatientId) {
}
