package com.example.turnos_service.reservation.infrastructure.kafka.handler;

import com.example.turnos_service.reservation.application.service.ReservationService;
import com.example.turnos_service.reservation.infrastructure.kafka.consumer.AppointmentEventHandler;
import com.example.turnos_service.reservation.infrastructure.kafka.dto.AppointmentKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Maneja la cancelación informada por la cátedra
@Component
@RequiredArgsConstructor
public class AppointmentCancelledHandler implements AppointmentEventHandler {

    private final ReservationService reservationService;

    // Devuelve el tipo de evento que maneja
    @Override
    public String eventType() {
        return "AppointmentCancelled";
    }

    // Pasa a la fachada el proceso cancelado
    @Override
    public void handle(AppointmentKafkaEvent event) {
        reservationService.handleAppointmentCancelled(event.reservationProcessId());
    }
}
