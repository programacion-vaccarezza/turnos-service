package com.example.turnos_service.reservation.domain.ports.in;

import com.example.turnos_service.reservation.domain.model.AvailableSlot;

import java.time.LocalDate;
import java.util.List;

// Caso de uso: consultar los turnos libres de un profesional
public interface GetAvailabilityUseCase {
    // Devuelve los turnos libres en una fecha
    List<AvailableSlot> getAvailability(Long professionalId, LocalDate date);
}
