package com.example.turnos_service.reservation.application.usecases;

import com.example.turnos_service.reservation.application.service.ReservationEventStatusUpdater;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.ports.in.HandleAppointmentProcessExpiredUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Registra el vencimiento informado por la cátedra
@Component
@RequiredArgsConstructor
public class HandleAppointmentProcessExpiredUseCaseImpl implements HandleAppointmentProcessExpiredUseCase {

    private final ReservationEventStatusUpdater statusUpdater;

    // Pasa el proceso a EXPIRED con el motivo
    @Override
    public void handleAppointmentProcessExpired(String reservationProcessId, String reason) {
        statusUpdater.apply(reservationProcessId, ReservationStatus.EXPIRED, "AppointmentProcessExpired",
                process -> process.setFailureReason(reason));
    }
}
