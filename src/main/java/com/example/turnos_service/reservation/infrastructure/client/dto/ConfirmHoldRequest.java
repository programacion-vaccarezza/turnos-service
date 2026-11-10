package com.example.turnos_service.reservation.infrastructure.client.dto;

// Body para confirmar un hold en la cátedra
public record ConfirmHoldRequest(String externalPatientId, String patientFirstName, String patientLastName) {
}
