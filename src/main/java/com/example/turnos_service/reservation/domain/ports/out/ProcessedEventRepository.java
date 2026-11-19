package com.example.turnos_service.reservation.domain.ports.out;

import com.example.turnos_service.reservation.domain.model.ProcessedEvent;

// Port para guardar los eventos de Kafka procesados
public interface ProcessedEventRepository {

    // Indica si el evento ya se procesó
    boolean existsByEventId(String eventId);
    // Registra el evento como procesado
    ProcessedEvent save(ProcessedEvent processedEvent);

}
