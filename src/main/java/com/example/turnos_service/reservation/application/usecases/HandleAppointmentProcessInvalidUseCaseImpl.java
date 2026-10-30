package com.example.turnos_service.reservation.application.usecases;

import com.example.turnos_service.reservation.application.service.ReservationEventStatusUpdater;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.ports.in.HandleAppointmentProcessInvalidUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** La cátedra detectó una inconsistencia en el proceso (anexo §15.9): pasa a INVALID, que es final. */
@Component
@RequiredArgsConstructor
public class HandleAppointmentProcessInvalidUseCaseImpl implements HandleAppointmentProcessInvalidUseCase {

    private final ReservationEventStatusUpdater statusUpdater;

    @Override
    public void handleAppointmentProcessInvalid(String reservationProcessId, String reason) {
        statusUpdater.apply(reservationProcessId, ReservationStatus.INVALID, "AppointmentProcessInvalid",
                process -> process.setFailureReason(reason));
    }
}
