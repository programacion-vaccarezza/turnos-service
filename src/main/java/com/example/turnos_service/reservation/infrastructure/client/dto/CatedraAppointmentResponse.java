package com.example.turnos_service.reservation.infrastructure.client.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record CatedraAppointmentResponse(Long reservationId, String reservationProcessId, String externalPatientId,
                                         LocalDate appointmentDate, LocalTime startTime, LocalTime endTime,
                                         Long professionalId, String professionalFirstName, String professionalLastName,
                                         String status, Instant createdAt, Instant confirmedAt, Instant cancelledAt,
                                         String cancellationReason) {
}
