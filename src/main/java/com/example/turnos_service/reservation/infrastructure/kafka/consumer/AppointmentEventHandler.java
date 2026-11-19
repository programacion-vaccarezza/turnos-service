package com.example.turnos_service.reservation.infrastructure.kafka.consumer;

import com.example.turnos_service.reservation.infrastructure.kafka.dto.AppointmentKafkaEvent;

// Handler de un tipo de evento de la cátedra
public interface AppointmentEventHandler {
    // Tipo de evento que maneja
    String eventType();
    // Procesa el evento
    void handle(AppointmentKafkaEvent event);
}
