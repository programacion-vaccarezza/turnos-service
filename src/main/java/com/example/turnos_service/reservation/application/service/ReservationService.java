package com.example.turnos_service.reservation.application.service;

import com.example.turnos_service.reservation.domain.model.AvailableSlot;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.UserReservation;
import com.example.turnos_service.reservation.domain.ports.in.CancelReservationUseCase;
import com.example.turnos_service.reservation.domain.ports.in.GetAvailabilityUseCase;
import com.example.turnos_service.reservation.domain.ports.in.GetMyReservationsUseCase;
import com.example.turnos_service.reservation.domain.ports.in.GetReservationUseCase;
import com.example.turnos_service.reservation.domain.ports.in.HandleAdditionalInformationRejectedUseCase;
import com.example.turnos_service.reservation.domain.ports.in.HandleAppointmentCancelledUseCase;
import com.example.turnos_service.reservation.domain.ports.in.HandleAdditionalInformationRequestedUseCase;
import com.example.turnos_service.reservation.domain.ports.in.HandleAppointmentConfirmedUseCase;
import com.example.turnos_service.reservation.domain.ports.in.HandleAppointmentProcessExpiredUseCase;
import com.example.turnos_service.reservation.domain.ports.in.HandleAppointmentProcessInvalidUseCase;
import com.example.turnos_service.reservation.domain.ports.in.InitiateReservationUseCase;
import com.example.turnos_service.reservation.domain.ports.in.ReconcileOpenReservationsUseCase;
import com.example.turnos_service.reservation.domain.ports.in.SubmitPhoneUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// Fachada que expone los casos de uso de reservas
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
    private final CancelReservationUseCase cancelReservationUseCase;
    private final HandleAppointmentCancelledUseCase handleAppointmentCancelledUseCase;
    private final ReconcileOpenReservationsUseCase reconcileOpenReservationsUseCase;
    private final GetAvailabilityUseCase getAvailabilityUseCase;
    private final GetReservationUseCase getReservationUseCase;

    // Inicia una reserva
    public ReservationProcess initiateReservation(String ownerLogin, Long professionalId, LocalDate date, LocalTime startTime) {
        return initiateReservationUseCase.initiateReservation(ownerLogin, professionalId, date, startTime);
    }

    // Registra el pedido de teléfono de la cátedra
    public void handleAdditionalInformationRequested(String reservationProcessId, String requestEventId, Instant expiresAt) {
        handleAdditionalInformationRequestedUseCase.handleAdditionalInformationRequested(reservationProcessId, requestEventId, expiresAt);
    }

    // Envía el teléfono del usuario
    public ReservationProcess submitPhone(String ownerLogin, Long processId, String phoneNumber) {
        return submitPhoneUseCase.submitPhone(ownerLogin, processId, phoneNumber);
    }

    // Marca la reserva como confirmada
    public void handleAppointmentConfirmed(String reservationProcessId, Long reservationId) {
        handleAppointmentConfirmedUseCase.handleAppointmentConfirmed(reservationProcessId, reservationId);
    }

    // Registra el rechazo del teléfono
    public void handleAdditionalInformationRejected(String reservationProcessId, String reason, Instant expiresAt) {
        handleAdditionalInformationRejectedUseCase.handleAdditionalInformationRejected(reservationProcessId, reason, expiresAt);
    }

    // Marca el proceso como vencido
    public void handleAppointmentProcessExpired(String reservationProcessId, String reason) {
        handleAppointmentProcessExpiredUseCase.handleAppointmentProcessExpired(reservationProcessId, reason);
    }

    // Marca el proceso como inválido
    public void handleAppointmentProcessInvalid(String reservationProcessId, String reason) {
        handleAppointmentProcessInvalidUseCase.handleAppointmentProcessInvalid(reservationProcessId, reason);
    }

    // Devuelve las reservas del usuario
    public List<UserReservation> getMyReservations(String ownerLogin) {
        return getMyReservationsUseCase.getMyReservations(ownerLogin);
    }

    // Cancela una reserva del usuario
    public ReservationProcess cancelReservation(String ownerLogin, Long processId, String reason) {
        return cancelReservationUseCase.cancelReservation(ownerLogin, processId, reason);
    }

    // Marca la reserva como cancelada
    public void handleAppointmentCancelled(String reservationProcessId) {
        handleAppointmentCancelledUseCase.handleAppointmentCancelled(reservationProcessId);
    }

    // Reconcilia con la cátedra los procesos abiertos
    public void reconcileOpenReservations() {
        reconcileOpenReservationsUseCase.reconcileOpenReservations();
    }

    // Devuelve los turnos libres de un profesional en una fecha
    public List<AvailableSlot> getAvailability(Long professionalId, LocalDate date) {
        return getAvailabilityUseCase.getAvailability(professionalId, date);
    }

    // Devuelve el estado de una reserva del usuario
    public ReservationProcess getReservation(String ownerLogin, Long processId) {
        return getReservationUseCase.getReservation(ownerLogin, processId);
    }
}
