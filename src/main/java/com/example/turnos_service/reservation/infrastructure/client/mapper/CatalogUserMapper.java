package com.example.turnos_service.reservation.infrastructure.client.mapper;

import com.example.turnos_service.reservation.domain.model.PatientInfo;
import com.example.turnos_service.reservation.infrastructure.client.dto.CatalogUserResponse;
import org.springframework.stereotype.Component;

@Component
public class CatalogUserMapper {

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
