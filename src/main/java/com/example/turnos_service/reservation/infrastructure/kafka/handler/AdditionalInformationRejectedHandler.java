package com.example.turnos_service.reservation.infrastructure.kafka.handler;

import com.example.turnos_service.reservation.application.service.ReservationService;
import com.example.turnos_service.reservation.infrastructure.kafka.consumer.AppointmentEventHandler;
import com.example.turnos_service.reservation.infrastructure.kafka.dto.AppointmentKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Maneja el rechazo del teléfono
@Component
@RequiredArgsConstructor
public class AdditionalInformationRejectedHandler implements AppointmentEventHandler {

    private final ReservationService reservationService;

    // Devuelve el tipo de evento que maneja
    @Override
    public String eventType() {
        return "AdditionalInformationRejected";
    }

    // Pasa a la fachada el motivo y el nuevo vencimiento
    @Override
    public void handle(AppointmentKafkaEvent event) {
        reservationService.handleAdditionalInformationRejected(event.reservationProcessId(), event.reason(), event.expiresAt());
    }
}
