package com.example.turnos_service.reservation.infrastructure.kafka.consumer;

import com.example.turnos_service.reservation.infrastructure.kafka.dto.AppointmentKafkaEvent;

/**
 * Un handler por tipo de evento de la cátedra. Agregar un tipo nuevo es agregar un handler,
 * sin modificar el consumer ni los handlers existentes.
 */
public interface AppointmentEventHandler {
    String eventType();
    void handle(AppointmentKafkaEvent event);
}
