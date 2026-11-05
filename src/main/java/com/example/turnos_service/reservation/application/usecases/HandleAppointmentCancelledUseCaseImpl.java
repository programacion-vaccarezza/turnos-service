package com.example.turnos_service.reservation.application.usecases;

import com.example.turnos_service.reservation.application.service.ReservationEventStatusUpdater;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.ports.in.HandleAppointmentCancelledUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * La cátedra canceló la reserva (anexo §15.10): pasa a CANCELLED, que es final.
 * Si el cancel REST ya la había dejado en CANCELLED, el evento se ignora.
 */
@Component
@RequiredArgsConstructor
public class HandleAppointmentCancelledUseCaseImpl implements HandleAppointmentCancelledUseCase {

    private final ReservationEventStatusUpdater statusUpdater;

    @Override
    public void handleAppointmentCancelled(String reservationProcessId) {
        statusUpdater.apply(reservationProcessId, ReservationStatus.CANCELLED, "AppointmentCancelled", process -> { });
    }
}
