package com.example.turnos_service.reservation.infrastructure.kafka.handler;

import com.example.turnos_service.reservation.application.service.ReservationService;
import com.example.turnos_service.reservation.infrastructure.kafka.consumer.AppointmentEventHandler;
import com.example.turnos_service.reservation.infrastructure.kafka.dto.AppointmentKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Maneja el proceso marcado como inválido
@Component
@RequiredArgsConstructor
public class AppointmentProcessInvalidHandler implements AppointmentEventHandler {

    private final ReservationService reservationService;

    // Devuelve el tipo de evento que maneja
    @Override
    public String eventType() {
        return "AppointmentProcessInvalid";
    }

    // Pasa a la fachada el proceso y el motivo
    @Override
    public void handle(AppointmentKafkaEvent event) {
        reservationService.handleAppointmentProcessInvalid(event.reservationProcessId(), event.reason());
    }
}
