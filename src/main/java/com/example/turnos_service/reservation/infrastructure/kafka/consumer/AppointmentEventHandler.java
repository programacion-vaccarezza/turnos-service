package com.example.turnos_service.reservation.infrastructure.kafka.consumer;

import com.example.turnos_service.reservation.infrastructure.kafka.dto.AppointmentKafkaEvent;

public interface AppointmentEventHandler {
    String eventType();
    void handle(AppointmentKafkaEvent event);
}
