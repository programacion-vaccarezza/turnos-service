package com.example.turnos_service.reservation.infrastructure.client.mapper;

import com.example.turnos_service.reservation.domain.model.ProfessionalInfo;
import com.example.turnos_service.reservation.infrastructure.client.dto.CatalogProfessionalResponse;
import org.springframework.stereotype.Component;

// Convierte el profesional de catalogo-service al modelo de dominio
@Component
public class CatalogProfessionalMapper {

    // Convierte la respuesta en ProfessionalInfo
    public ProfessionalInfo toDomain(CatalogProfessionalResponse response) {
        if (response == null) {
            return null;
        }
        return ProfessionalInfo.builder()
                .id(response.id())
                .firstName(response.firstName())
                .lastName(response.lastName())
                .enabled(response.enabled())
                .build();
    }
}
