package com.example.turnos_service.reservation.infrastructure.client.mapper;

import com.example.turnos_service.reservation.domain.model.PatientInfo;
import com.example.turnos_service.reservation.infrastructure.client.dto.CatalogUserResponse;
import org.springframework.stereotype.Component;

// Convierte el usuario de catalogo-service al modelo de dominio
@Component
public class CatalogUserMapper {

    // Convierte la respuesta en PatientInfo
    public PatientInfo toDomain(CatalogUserResponse response) {
        if (response == null) {
            return null;
        }
        return PatientInfo.builder()
                .login(response.login())
                .firstName(response.firstName())
                .lastName(response.lastName())
                .build();
    }
}
