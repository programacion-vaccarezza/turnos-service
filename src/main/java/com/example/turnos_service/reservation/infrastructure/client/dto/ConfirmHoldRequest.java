package com.example.turnos_service.reservation.infrastructure.client.dto;

/**
 * Body de POST /api/appointment-holds/{holdId}/confirm (anexo §10).
 */
public record ConfirmHoldRequest(String externalPatientId, String patientFirstName, String patientLastName) {
}
