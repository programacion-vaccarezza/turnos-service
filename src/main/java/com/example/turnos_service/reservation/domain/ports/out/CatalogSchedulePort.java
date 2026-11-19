package com.example.turnos_service.reservation.domain.ports.out;

import com.example.turnos_service.reservation.domain.model.WeeklySchedule;

import java.util.List;

// Port para consultar los horarios de un profesional en catalogo-service
public interface CatalogSchedulePort {
    // Devuelve los horarios habilitados del profesional
    List<WeeklySchedule> findEnabledByProfessionalId(Long professionalId);
}
