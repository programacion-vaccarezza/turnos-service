package com.example.turnos_service.reservation.infrastructure.kafka.consumer;

import com.example.turnos_service.reservation.application.service.EventIdempotencyService;
import com.example.turnos_service.reservation.infrastructure.kafka.dto.AppointmentKafkaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Consume el topic de acciones de la cátedra y despacha cada evento al handler de su eventType.
 * Los tipos desconocidos se ignoran; los eventos ya procesados se ignoran a través de {@link EventIdempotencyService}.
 */
@Component
public class AppointmentActionsConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(AppointmentActionsConsumer.class);

    private final Map<String, AppointmentEventHandler> handlers;
    private final EventIdempotencyService eventIdempotencyService;

    public AppointmentActionsConsumer(List<AppointmentEventHandler> handlers,
                                      EventIdempotencyService eventIdempotencyService) {
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(AppointmentEventHandler::eventType, Function.identity()));
        this.eventIdempotencyService = eventIdempotencyService;
    }

    @KafkaListener(topics = "${catedra.kafka.appointment-actions-topic}")
    public void receive(@Payload AppointmentKafkaEvent event,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        if (event.eventId() == null || event.eventType() == null) {
            LOG.warn("evento sin eventId o eventType en la partición {} offset {}, se ignora", partition, offset);
            return;
        }
        LOG.info("recibido {} (eventId {}) del proceso {} en la partición {} offset {}",
                event.eventType(), event.eventId(), event.reservationProcessId(), partition, offset);

        AppointmentEventHandler handler = handlers.get(event.eventType());
        if (handler == null) {
            LOG.info("tipo de evento {} sin handler, se ignora", event.eventType());
            return;
        }
        eventIdempotencyService.processOnce(event.eventId(), event.eventType(), () -> handler.handle(event));
    }
}
