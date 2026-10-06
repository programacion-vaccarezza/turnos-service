package com.example.turnos_service.reservation.domain.ports.out;

import com.example.turnos_service.reservation.domain.model.PatientInfo;

import java.util.Optional;

public interface CatalogUserPort {
    Optional<PatientInfo> findByLogin(String login);
}
