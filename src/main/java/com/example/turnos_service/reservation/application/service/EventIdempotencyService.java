package com.example.turnos_service.reservation.application.service;

import com.example.turnos_service.reservation.domain.model.ProcessedEvent;
import com.example.turnos_service.reservation.domain.ports.out.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Garantiza que cada evento de Kafka se procese una sola vez: si su eventId ya fue procesado se ignora,
 * y si no, se aplica su efecto y se registra el eventId en la misma transacción (CLAUDE.md §3).
 */
@Service
@RequiredArgsConstructor
public class EventIdempotencyService {

    private static final Logger LOG = LoggerFactory.getLogger(EventIdempotencyService.class);

    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public boolean processOnce(String eventId, String eventType, Runnable effect) {
        if (processedEventRepository.existsByEventId(eventId)) {
            LOG.info("evento {} ({}) ya procesado, se ignora", eventId, eventType);
            return false;
        }
        effect.run();
        processedEventRepository.save(new ProcessedEvent(eventId, eventType, Instant.now()));
        return true;
    }
}
