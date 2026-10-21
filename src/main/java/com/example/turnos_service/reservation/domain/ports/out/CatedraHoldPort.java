package com.example.turnos_service.reservation.domain.ports.out;

import com.example.turnos_service.reservation.domain.model.Hold;
import com.example.turnos_service.reservation.domain.model.HoldConfirmation;

import java.time.LocalDate;
import java.time.LocalTime;

public interface CatedraHoldPort {
    Hold createHold(Long professionalId, LocalDate date, LocalTime startTime, String externalPatientId);
    HoldConfirmation confirmHold(String holdId, String externalPatientId, String patientFirstName, String patientLastName);
}
