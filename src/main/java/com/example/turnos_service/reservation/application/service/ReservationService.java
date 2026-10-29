package com.example.turnos_service.reservation.application.service;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.UserReservation;
import com.example.turnos_service.reservation.domain.ports.in.GetMyReservationsUseCase;
import com.example.turnos_service.reservation.domain.ports.in.HandleAdditionalInformationRejectedUseCase;
import com.example.turnos_service.reservation.domain.ports.in.HandleAdditionalInformationRequestedUseCase;
import com.example.turnos_service.reservation.domain.ports.in.HandleAppointmentConfirmedUseCase;
import com.example.turnos_service.reservation.domain.ports.in.HandleAppointmentProcessExpiredUseCase;
import com.example.turnos_service.reservation.domain.ports.in.HandleAppointmentProcessInvalidUseCase;
import com.example.turnos_service.reservation.domain.ports.in.InitiateReservationUseCase;
import com.example.turnos_service.reservation.domain.ports.in.SubmitPhoneUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final InitiateReservationUseCase initiateReservationUseCase;
    private final HandleAdditionalInformationRequestedUseCase handleAdditionalInformationRequestedUseCase;
    private final SubmitPhoneUseCase submitPhoneUseCase;
    private final HandleAppointmentConfirmedUseCase handleAppointmentConfirmedUseCase;
    private final HandleAdditionalInformationRejectedUseCase handleAdditionalInformationRejectedUseCase;
    private final HandleAppointmentProcessExpiredUseCase handleAppointmentProcessExpiredUseCase;
    private final HandleAppointmentProcessInvalidUseCase handleAppointmentProcessInvalidUseCase;
    private final GetMyReservationsUseCase getMyReservationsUseCase;

    public ReservationProcess initiateReservation(String ownerLogin, Long professionalId, LocalDate date, LocalTime startTime) {
        return initiateReservationUseCase.initiateReservation(ownerLogin, professionalId, date, startTime);
    }

    public void handleAdditionalInformationRequested(String reservationProcessId, String requestEventId, Instant expiresAt) {
        handleAdditionalInformationRequestedUseCase.handleAdditionalInformationRequested(reservationProcessId, requestEventId, expiresAt);
    }

    public ReservationProcess submitPhone(String ownerLogin, Long processId, String phoneNumber) {
        return submitPhoneUseCase.submitPhone(ownerLogin, processId, phoneNumber);
    }

    public void handleAppointmentConfirmed(String reservationProcessId, Long reservationId) {
        handleAppointmentConfirmedUseCase.handleAppointmentConfirmed(reservationProcessId, reservationId);
    }

    public void handleAdditionalInformationRejected(String reservationProcessId, String reason, Instant expiresAt) {
        handleAdditionalInformationRejectedUseCase.handleAdditionalInformationRejected(reservationProcessId, reason, expiresAt);
    }

    public void handleAppointmentProcessExpired(String reservationProcessId, String reason) {
        handleAppointmentProcessExpiredUseCase.handleAppointmentProcessExpired(reservationProcessId, reason);
    }

    public void handleAppointmentProcessInvalid(String reservationProcessId, String reason) {
        handleAppointmentProcessInvalidUseCase.handleAppointmentProcessInvalid(reservationProcessId, reason);
    }

    public List<UserReservation> getMyReservations(String ownerLogin) {
        return getMyReservationsUseCase.getMyReservations(ownerLogin);
    }
}
