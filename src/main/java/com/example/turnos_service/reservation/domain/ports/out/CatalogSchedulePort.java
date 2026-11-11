package com.example.turnos_service.reservation.domain.ports.out;

import com.example.turnos_service.reservation.domain.model.WeeklySchedule;

import java.util.List;

public interface CatalogSchedulePort {
    List<WeeklySchedule> findEnabledByProfessionalId(Long professionalId);
}
