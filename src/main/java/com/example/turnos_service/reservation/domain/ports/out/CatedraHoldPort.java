package com.example.turnos_service.reservation.domain.ports.out;

import com.example.turnos_service.reservation.domain.model.Hold;
import com.example.turnos_service.reservation.domain.model.HoldConfirmation;

import java.time.LocalDate;
import java.time.LocalTime;

// Port para crear y confirmar holds en la cátedra
public interface CatedraHoldPort {
    // Crea el bloqueo temporal del turno
    Hold createHold(Long professionalId, LocalDate date, LocalTime startTime, String externalPatientId);
    // Confirma el hold con los datos del paciente
    HoldConfirmation confirmHold(String holdId, String externalPatientId, String patientFirstName, String patientLastName);
}
