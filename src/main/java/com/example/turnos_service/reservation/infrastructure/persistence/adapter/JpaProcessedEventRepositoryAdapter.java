package com.example.turnos_service.reservation.infrastructure.persistence.adapter;

import com.example.turnos_service.reservation.domain.model.ProcessedEvent;
import com.example.turnos_service.reservation.domain.ports.out.ProcessedEventRepository;
import com.example.turnos_service.reservation.infrastructure.persistence.mapper.ProcessedEventMapper;
import com.example.turnos_service.reservation.infrastructure.persistence.repository.JpaProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Adapter que guarda los eventos procesados con JPA
@Component
@RequiredArgsConstructor
public class JpaProcessedEventRepositoryAdapter implements ProcessedEventRepository {

    private final JpaProcessedEventRepository jpaProcessedEventRepository;
    private final ProcessedEventMapper processedEventMapper;

    // Indica si el evento ya está guardado
    @Override
    public boolean existsByEventId(String eventId) {
        return jpaProcessedEventRepository.existsById(eventId);
    }

    // Guarda el evento
    @Override
    public ProcessedEvent save(ProcessedEvent processedEvent) {
        return processedEventMapper.toDomainModel(
                jpaProcessedEventRepository.save(processedEventMapper.toEntity(processedEvent)));
    }
}
