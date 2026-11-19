package com.example.turnos_service.reservation.domain.ports.out;

import com.example.turnos_service.reservation.domain.model.ProfessionalInfo;

import java.util.Optional;

// Port para consultar profesionales en catalogo-service
public interface CatalogProfessionalPort {
    // Devuelve el profesional, o vacío si no existe
    Optional<ProfessionalInfo> findById(Long professionalId);
}
