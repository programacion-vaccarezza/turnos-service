package com.example.turnos_service.reservation.infrastructure.client.dto;

public record ConfirmHoldRequest(String externalPatientId, String patientFirstName, String patientLastName) {
}
