package com.example.turnos_service.reservation.domain.ports.out;

import com.example.turnos_service.reservation.domain.model.ProcessedEvent;

public interface ProcessedEventRepository {

    boolean existsByEventId(String eventId);
    ProcessedEvent save(ProcessedEvent processedEvent);

}
