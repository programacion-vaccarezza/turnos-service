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

// Proceso de reserva de un usuario, con su estado y los datos de la cátedra
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

    // Estados que se vencen localmente al pasar expiresAt
    private static final Set<ReservationStatus> LOCALLY_EXPIRABLE = EnumSet.of(
            ReservationStatus.HOLD_CREATED, ReservationStatus.WAITING_FOR_PHONE, ReservationStatus.PHONE_REJECTED);

    // Estados cuyo resultado depende de la cátedra
    private static final Set<ReservationStatus> UNKNOWN_OUTCOME = EnumSet.of(
            ReservationStatus.CONFIRMING, ReservationStatus.PHONE_SUBMITTED);

    // Indica si el proceso venció con un resultado desconocido y hay que consultar a la cátedra
    public boolean needsReconciliation(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt) && UNKNOWN_OUTCOME.contains(status);
    }

    // Indica si el proceso venció mientras la cátedra esperaba algo del servicio
    public boolean isLocallyExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt) && LOCALLY_EXPIRABLE.contains(status)
                && status.canTransitionTo(ReservationStatus.EXPIRED);
    }
}
