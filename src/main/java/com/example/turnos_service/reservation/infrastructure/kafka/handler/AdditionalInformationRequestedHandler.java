package com.example.turnos_service.reservation.infrastructure.kafka.handler;

import com.example.turnos_service.reservation.application.service.ReservationService;
import com.example.turnos_service.reservation.infrastructure.kafka.consumer.AppointmentEventHandler;
import com.example.turnos_service.reservation.infrastructure.kafka.dto.AppointmentKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdditionalInformationRequestedHandler implements AppointmentEventHandler {

    private final ReservationService reservationService;

    @Override
    public String eventType() {
        return "AdditionalInformationRequested";
    }

    @Override
    public void handle(AppointmentKafkaEvent event) {
        reservationService.handleAdditionalInformationRequested(event.reservationProcessId(), event.eventId(), event.expiresAt());
    }
}
