package com.example.turnos_service.reservation.infrastructure.client.mapper;

import com.example.turnos_service.reservation.domain.model.Occupancy;
import com.example.turnos_service.reservation.infrastructure.client.dto.CatedraOccupanciesResponse;
import org.springframework.stereotype.Component;

import java.util.List;

// Convierte las ocupaciones de la cátedra al modelo de dominio
@Component
public class CatedraOccupancyMapper {

    // Convierte la respuesta en una lista de ocupaciones
    public List<Occupancy> toDomain(CatedraOccupanciesResponse response) {
        if (response == null || response.occupancies() == null) {
            return List.of();
        }
        return response.occupancies().stream().map(this::toDomain).toList();
    }

    // Convierte un turno ocupado
    private Occupancy toDomain(CatedraOccupanciesResponse.Item item) {
        return Occupancy.builder()
                .date(item.date())
                .startTime(item.startTime())
                .status(item.status())
                .expiresAt(item.expiresAt())
                .build();
    }
}
