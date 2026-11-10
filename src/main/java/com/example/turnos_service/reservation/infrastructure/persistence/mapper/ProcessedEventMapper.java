package com.example.turnos_service.reservation.infrastructure.persistence.mapper;

import com.example.turnos_service.reservation.domain.model.ProcessedEvent;
import com.example.turnos_service.reservation.infrastructure.persistence.entity.ProcessedEventEntity;
import org.springframework.stereotype.Component;

// Convierte eventos procesados entre el dominio y la entidad JPA
@Component
public class ProcessedEventMapper {

    // Convierte el modelo de dominio en entidad
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

    // Convierte la entidad en modelo de dominio
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
