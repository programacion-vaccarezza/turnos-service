package com.example.turnos_service.reservation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationProcess {

    private Long id;
    private String ownerLogin;
    private Long professionalId;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private String patientFirstName;
    private String patientLastName;
    private ReservationStatus status;
    private String holdId;
    private String reservationProcessId;
    private Instant expiresAt;
    private String requestEventId;
    private Long reservationId;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;

    /**
     * Estados en los que la cátedra espera algo de turnos-service, así que al pasar expiresAt el proceso está vencido.
     * CONFIRMING y PHONE_SUBMITTED no están: el resultado de la cátedra es desconocido y converge por evento o reconciliación.
     */
    private static final Set<ReservationStatus> LOCALLY_EXPIRABLE = EnumSet.of(
            ReservationStatus.HOLD_CREATED, ReservationStatus.WAITING_FOR_PHONE, ReservationStatus.PHONE_REJECTED);

    public boolean isLocallyExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt) && LOCALLY_EXPIRABLE.contains(status)
                && status.canTransitionTo(ReservationStatus.EXPIRED);
    }
}
