package com.example.turnos_service.reservation.domain.ports.out;

import com.example.turnos_service.reservation.domain.model.Hold;

import java.time.LocalDate;
import java.time.LocalTime;

public interface CatedraHoldPort {
    Hold createHold(Long professionalId, LocalDate date, LocalTime startTime, String externalPatientId);
}
