package com.example.turnos_service.reservation.infrastructure.persistence.mapper;

import com.example.turnos_service.reservation.domain.model.ProcessedEvent;
import com.example.turnos_service.reservation.infrastructure.persistence.entity.ProcessedEventEntity;
import org.springframework.stereotype.Component;

@Component
public class ProcessedEventMapper {

    public ProcessedEventEntity toEntity(ProcessedEvent processedEvent) {
        if (processedEvent == null) {
            return null;
        }
        return ProcessedEventEntity.builder()
                .eventId(processedEvent.getEventId())
                .eventType(processedEvent.getEventType())
                .processedAt(processedEvent.getProcessedAt())
                .build();
    }

    public ProcessedEvent toDomainModel(ProcessedEventEntity entity) {
        if (entity == null) {
            return null;
        }
        return ProcessedEvent.builder()
                .eventId(entity.getEventId())
                .eventType(entity.getEventType())
                .processedAt(entity.getProcessedAt())
                .build();
    }
}
