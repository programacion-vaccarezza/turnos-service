package com.example.turnos_service.reservation.application.usecases;

import com.example.turnos_service.reservation.application.exception.CatedraUnavailableException;
import com.example.turnos_service.reservation.application.service.ReservationReconciler;
import com.example.turnos_service.reservation.domain.model.CatedraAppointment;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.ports.in.ReconcileOpenReservationsUseCase;
import com.example.turnos_service.reservation.domain.ports.out.CatedraAppointmentPort;
import com.example.turnos_service.reservation.domain.ports.out.ReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Recupera los procesos que quedaron abiertos (enunciado §8, anexo §18.4): consulta a la cátedra las reservas
 * de sus dueños y lleva cada proceso local al estado informado con {@link ReservationReconciler}.
 * Si la cátedra no responde, reintenta una cantidad limitada de veces y después se rinde sin tocar nada.
 */
@Component
@RequiredArgsConstructor
public class ReconcileOpenReservationsUseCaseImpl implements ReconcileOpenReservationsUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ReconcileOpenReservationsUseCaseImpl.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MILLIS = 5_000;

    /** Procesos que todavía pueden cambiar: los no finales y CONFIRMED, que puede haberse cancelado. */
    private static final Set<ReservationStatus> RECONCILABLE = EnumSet.of(
            ReservationStatus.HOLD_CREATED, ReservationStatus.CONFIRMING, ReservationStatus.WAITING_FOR_PHONE,
            ReservationStatus.PHONE_SUBMITTED, ReservationStatus.PHONE_REJECTED, ReservationStatus.CONFIRMED);

    private final ReservationProcessRepository reservationProcessRepository;
    private final CatedraAppointmentPort catedraAppointmentPort;
    private final ReservationReconciler reservationReconciler;

    @Override
    public void reconcileOpenReservations() {
        List<ReservationProcess> openProcesses = reservationProcessRepository.findByStatusIn(RECONCILABLE);
        if (openProcesses.isEmpty()) {
            LOG.info("reconciliación: no hay procesos de reserva abiertos");
            return;
        }
        LOG.info("reconciliación: {} procesos de reserva abiertos", openProcesses.size());

        Optional<Map<String, CatedraAppointment>> appointments = fetchWithRetries(openProcesses);
        if (appointments.isEmpty()) {
            LOG.error("reconciliación abandonada después de {} intentos: la cátedra no responde. "
                    + "Los procesos convergen por eventos o por el vencimiento local", MAX_ATTEMPTS);
            return;
        }
        openProcesses.forEach(process -> reservationReconciler.reconcile(process, appointments.get().get(process.getReservationProcessId())));
        LOG.info("reconciliación terminada");
    }

    /** Reservas de la cátedra de todos los dueños de procesos abiertos, por reservationProcessId. Vacío si se agotan los intentos. */
    private Optional<Map<String, CatedraAppointment>> fetchWithRetries(List<ReservationProcess> openProcesses) {
        Set<String> owners = openProcesses.stream().map(ReservationProcess::getOwnerLogin).collect(Collectors.toSet());
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                Map<String, CatedraAppointment> byProcessId = new HashMap<>();
                owners.forEach(owner -> catedraAppointmentPort.findByExternalPatientId(owner).stream()
                        .filter(appointment -> appointment.getReservationProcessId() != null)
                        .forEach(appointment -> byProcessId.put(appointment.getReservationProcessId(), appointment)));
                return Optional.of(byProcessId);
            } catch (CatedraUnavailableException e) {
                LOG.warn("reconciliación: intento {} de {} sin respuesta de la cátedra", attempt, MAX_ATTEMPTS);
                if (attempt < MAX_ATTEMPTS && !waitBeforeRetry()) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    private boolean waitBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MILLIS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
