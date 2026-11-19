package com.example.turnos_service.reservation.application.usecases;

import com.example.turnos_service.reservation.application.service.ReservationEventStatusUpdater;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.ports.in.HandleAdditionalInformationRejectedUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

// Registra que la cátedra rechazó el teléfono
@Component
@RequiredArgsConstructor
public class HandleAdditionalInformationRejectedUseCaseImpl implements HandleAdditionalInformationRejectedUseCase {

    private final ReservationEventStatusUpdater statusUpdater;

    // Pasa el proceso a PHONE_REJECTED con el motivo y el nuevo vencimiento
    @Override
    public void handleAdditionalInformationRejected(String reservationProcessId, String reason, Instant expiresAt) {
        statusUpdater.apply(reservationProcessId, ReservationStatus.PHONE_REJECTED, "AdditionalInformationRejected", process -> {
            process.setFailureReason(reason);
            if (expiresAt != null) {
                process.setExpiresAt(expiresAt);
            }
        });
    }
}
