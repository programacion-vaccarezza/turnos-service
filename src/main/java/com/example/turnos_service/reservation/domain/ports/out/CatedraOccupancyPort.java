package com.example.turnos_service.reservation.domain.ports.out;

import com.example.turnos_service.reservation.domain.model.Occupancy;

import java.time.LocalDate;
import java.util.List;

// Port para consultar los turnos ocupados en la cátedra
public interface CatedraOccupancyPort {
    // Devuelve las ocupaciones del profesional en una fecha
    List<Occupancy> findByProfessionalAndDate(Long professionalId, LocalDate date);
}
