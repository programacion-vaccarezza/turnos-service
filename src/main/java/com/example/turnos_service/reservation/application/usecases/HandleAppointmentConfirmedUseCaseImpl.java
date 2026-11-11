package com.example.turnos_service.reservation.application.usecases;

import com.example.turnos_service.reservation.application.service.ReservationEventStatusUpdater;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.ports.in.HandleAppointmentConfirmedUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HandleAppointmentConfirmedUseCaseImpl implements HandleAppointmentConfirmedUseCase {

    private final ReservationEventStatusUpdater statusUpdater;

    @Override
    public void handleAppointmentConfirmed(String reservationProcessId, Long reservationId) {
        statusUpdater.apply(reservationProcessId, ReservationStatus.CONFIRMED, "AppointmentConfirmed", process -> {
            process.setReservationId(reservationId);
            process.setFailureReason(null);
        });
    }
}
