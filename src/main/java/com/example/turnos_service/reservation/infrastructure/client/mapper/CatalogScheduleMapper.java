package com.example.turnos_service.reservation.infrastructure.client.mapper;

import com.example.turnos_service.reservation.domain.model.WeeklySchedule;
import com.example.turnos_service.reservation.infrastructure.client.dto.CatalogScheduleResponse;
import org.springframework.stereotype.Component;

@Component
public class CatalogScheduleMapper {

    public WeeklySchedule toDomain(CatalogScheduleResponse response) {
        if (response == null) {
            return null;
        }
        return WeeklySchedule.builder()
                .dayOfWeek(response.dayOfWeek())
                .startTime(response.startTime())
                .endTime(response.endTime())
                .slotDurationMinutes(response.slotDurationMinutes())
                .build();
    }
}
