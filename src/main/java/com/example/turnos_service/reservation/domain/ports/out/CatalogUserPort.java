package com.example.turnos_service.reservation.domain.ports.out;

import com.example.turnos_service.reservation.domain.model.PatientInfo;

import java.util.Optional;

// Port para consultar usuarios en catalogo-service
public interface CatalogUserPort {
    // Devuelve el nombre y apellido del usuario, o vacío si no existe
    Optional<PatientInfo> findByLogin(String login);
}
