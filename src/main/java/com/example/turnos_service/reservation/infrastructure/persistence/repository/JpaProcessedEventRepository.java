package com.example.turnos_service.reservation.infrastructure.persistence.repository;

import com.example.turnos_service.reservation.infrastructure.persistence.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Repositorio JPA de eventos procesados
@Repository
public interface JpaProcessedEventRepository extends JpaRepository<ProcessedEventEntity, String> {
}
