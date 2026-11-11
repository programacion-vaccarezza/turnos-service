package com.example.turnos_service.reservation.infrastructure.client.mapper;

import com.example.turnos_service.reservation.domain.model.Occupancy;
import com.example.turnos_service.reservation.infrastructure.client.dto.CatedraOccupanciesResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatedraOccupancyMapper {

    public List<Occupancy> toDomain(CatedraOccupanciesResponse response) {
        if (response == null || response.occupancies() == null) {
            return List.of();
        }
        return response.occupancies().stream().map(this::toDomain).toList();
    }

    private Occupancy toDomain(CatedraOccupanciesResponse.Item item) {
        return Occupancy.builder()
                .date(item.date())
                .startTime(item.startTime())
                .status(item.status())
                .expiresAt(item.expiresAt())
                .build();
    }
}
