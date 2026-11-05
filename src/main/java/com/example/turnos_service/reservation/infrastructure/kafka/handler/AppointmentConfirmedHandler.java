package com.example.turnos_service.reservation.infrastructure.kafka.handler;

import com.example.turnos_service.reservation.application.service.ReservationService;
import com.example.turnos_service.reservation.infrastructure.kafka.consumer.AppointmentEventHandler;
import com.example.turnos_service.reservation.infrastructure.kafka.dto.AppointmentKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentConfirmedHandler implements AppointmentEventHandler {

    private final ReservationService reservationService;

    @Override
    public String eventType() {
        return "AppointmentConfirmed";
    }

    @Override
    public void handle(AppointmentKafkaEvent event) {
        reservationService.handleAppointmentConfirmed(event.reservationProcessId(), event.reservationId());
    }
}
