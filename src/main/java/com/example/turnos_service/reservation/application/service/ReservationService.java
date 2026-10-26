package com.example.turnos_service.reservation.application.service;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.ports.in.HandleAdditionalInformationRequestedUseCase;
import com.example.turnos_service.reservation.domain.ports.in.InitiateReservationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final InitiateReservationUseCase initiateReservationUseCase;
    private final HandleAdditionalInformationRequestedUseCase handleAdditionalInformationRequestedUseCase;

    public ReservationProcess initiateReservation(String ownerLogin, Long professionalId, LocalDate date, LocalTime startTime) {
        return initiateReservationUseCase.initiateReservation(ownerLogin, professionalId, date, startTime);
    }

    public void handleAdditionalInformationRequested(String reservationProcessId, String requestEventId, Instant expiresAt) {
        handleAdditionalInformationRequestedUseCase.handleAdditionalInformationRequested(reservationProcessId, requestEventId, expiresAt);
    }
}
