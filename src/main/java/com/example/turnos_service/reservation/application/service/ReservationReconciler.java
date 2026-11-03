package com.example.turnos_service.reservation.application.service;

import com.example.turnos_service.reservation.application.exception.ConcurrentReservationUpdateException;
import com.example.turnos_service.reservation.domain.model.CatedraAppointment;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.ports.out.ReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReservationReconciler {

    private static final Logger LOG = LoggerFactory.getLogger(ReservationReconciler.class);
    private static final String PROCESS_EXPIRED = "PROCESS_EXPIRED";
    private static final String CATEDRA_FAILED = "CATEDRA_FAILED";

    private static final Map<String, ReservationStatus> LOCAL_STATUS_BY_CATEDRA_STATUS = Map.of(
            "PHONE_PENDING", ReservationStatus.WAITING_FOR_PHONE,
            "CONFIRMED", ReservationStatus.CONFIRMED,
            "CANCELLED", ReservationStatus.CANCELLED,
            "FAILED", ReservationStatus.EXPIRED);

    private static final Set<ReservationStatus> PHONE_STAGE = EnumSet.of(
            ReservationStatus.WAITING_FOR_PHONE, ReservationStatus.PHONE_SUBMITTED, ReservationStatus.PHONE_REJECTED);

    private final ReservationProcessRepository reservationProcessRepository;

    public ReservationProcess reconcile(ReservationProcess process, CatedraAppointment appointment) {
        if (appointment == null) {
            return reconcileNotListed(process);
        }
        ReservationStatus target = LOCAL_STATUS_BY_CATEDRA_STATUS.get(appointment.getStatus());
        if (target == null) {
            LOG.warn("reconciliación: estado {} de la cátedra desconocido para el proceso {}, se deja como está",
                    appointment.getStatus(), process.getId());
            return process;
        }
        if (process.getStatus() == target || (target == ReservationStatus.WAITING_FOR_PHONE && PHONE_STAGE.contains(process.getStatus()))) {
            return process;
        }
        if (appointment.getReservationId() != null) {
            process.setReservationId(appointment.getReservationId());
        }
        return moveTo(process, target, target == ReservationStatus.EXPIRED ? CATEDRA_FAILED : null);
    }

    private ReservationProcess reconcileNotListed(ReservationProcess process) {
        if (process.getExpiresAt() != null && Instant.now().isAfter(process.getExpiresAt())) {
            return moveTo(process, ReservationStatus.EXPIRED, PROCESS_EXPIRED);
        }
        return process;
    }

    private ReservationProcess moveTo(ReservationProcess process, ReservationStatus target, String failureReason) {
        ReservationStatus current = process.getStatus();
        List<ReservationStatus> path = current.pathTo(target);
        if (path.isEmpty()) {
            LOG.warn("reconciliación: el proceso {} no puede pasar de {} a {}, se deja como está", process.getId(), current, target);
            return process;
        }
        process.setStatus(target);
        process.setFailureReason(failureReason);
        try {
            ReservationProcess saved = reservationProcessRepository.save(process);
            LOG.info("reconciliación: proceso de reserva {}: {} -> {}", process.getId(), current,
                    path.stream().map(Enum::name).collect(Collectors.joining(" -> ")));
            return saved;
        } catch (ConcurrentReservationUpdateException e) {
            LOG.info("reconciliación: el proceso {} cambió por un evento al mismo tiempo, se deja el estado del evento", process.getId());
            return reservationProcessRepository.findById(process.getId()).orElseThrow(() -> e);
        }
    }
}
