package com.example.turnos_service.reservation.application.usecases;

import com.example.turnos_service.reservation.application.service.ReservationEventStatusUpdater;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.ports.in.HandleAppointmentCancelledUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Registra la cancelación informada por la cátedra
@Component
@RequiredArgsConstructor
public class HandleAppointmentCancelledUseCaseImpl implements HandleAppointmentCancelledUseCase {

    private final ReservationEventStatusUpdater statusUpdater;

    // Pasa el proceso a CANCELLED
    @Override
    public void handleAppointmentCancelled(String reservationProcessId) {
        statusUpdater.apply(reservationProcessId, ReservationStatus.CANCELLED, "AppointmentCancelled", process -> { });
    }
}
