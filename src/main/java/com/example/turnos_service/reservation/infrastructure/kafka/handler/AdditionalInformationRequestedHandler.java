package com.example.turnos_service.reservation.infrastructure.kafka.handler;

import com.example.turnos_service.reservation.application.service.ReservationService;
import com.example.turnos_service.reservation.infrastructure.kafka.consumer.AppointmentEventHandler;
import com.example.turnos_service.reservation.infrastructure.kafka.dto.AppointmentKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Maneja el pedido de teléfono de la cátedra
@Component
@RequiredArgsConstructor
public class AdditionalInformationRequestedHandler implements AppointmentEventHandler {

    private final ReservationService reservationService;

    // Devuelve el tipo de evento que maneja
    @Override
    public String eventType() {
        return "AdditionalInformationRequested";
    }

    // Pasa a la fachada el eventId del pedido y el vencimiento
    @Override
    public void handle(AppointmentKafkaEvent event) {
        reservationService.handleAdditionalInformationRequested(event.reservationProcessId(), event.eventId(), event.expiresAt());
    }
}
