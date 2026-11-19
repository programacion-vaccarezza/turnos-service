package com.example.turnos_service.reservation.infrastructure.kafka.handler;

import com.example.turnos_service.reservation.application.service.ReservationService;
import com.example.turnos_service.reservation.infrastructure.kafka.consumer.AppointmentEventHandler;
import com.example.turnos_service.reservation.infrastructure.kafka.dto.AppointmentKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Maneja la confirmación de la reserva
@Component
@RequiredArgsConstructor
public class AppointmentConfirmedHandler implements AppointmentEventHandler {

    private final ReservationService reservationService;

    // Devuelve el tipo de evento que maneja
    @Override
    public String eventType() {
        return "AppointmentConfirmed";
    }

    // Pasa a la fachada el proceso y el id de la reserva
    @Override
    public void handle(AppointmentKafkaEvent event) {
        reservationService.handleAppointmentConfirmed(event.reservationProcessId(), event.reservationId());
    }
}
