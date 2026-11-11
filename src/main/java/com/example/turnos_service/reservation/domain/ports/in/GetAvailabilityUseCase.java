package com.example.turnos_service.reservation.domain.ports.in;

import com.example.turnos_service.reservation.domain.model.AvailableSlot;

import java.time.LocalDate;
import java.util.List;

public interface GetAvailabilityUseCase {
    List<AvailableSlot> getAvailability(Long professionalId, LocalDate date);
}
