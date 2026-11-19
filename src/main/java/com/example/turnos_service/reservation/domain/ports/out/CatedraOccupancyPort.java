package com.example.turnos_service.reservation.domain.ports.out;

import com.example.turnos_service.reservation.domain.model.Occupancy;

import java.time.LocalDate;
import java.util.List;

public interface CatedraOccupancyPort {
    List<Occupancy> findByProfessionalAndDate(Long professionalId, LocalDate date);
}
