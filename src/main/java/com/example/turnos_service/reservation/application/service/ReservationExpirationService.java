package com.example.turnos_service.reservation.application.service;

import com.example.turnos_service.reservation.application.exception.ConcurrentReservationUpdateException;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.ports.out.ReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Vence localmente un proceso cuyo hold ya pasó su expiresAt: la copia local no extiende el hold (enunciado §7).
 * Se aplica al consultar el proceso; ver {@link ReservationProcess#isLocallyExpired(Instant)}.
 */
@Component
@RequiredArgsConstructor
public class ReservationExpirationService {

    public static final String PROCESS_EXPIRED = "PROCESS_EXPIRED";
    private static final Logger LOG = LoggerFactory.getLogger(ReservationExpirationService.class);

    private final ReservationProcessRepository reservationProcessRepository;

    public ReservationProcess expireIfOverdue(ReservationProcess process) {
        if (!process.isLocallyExpired(Instant.now())) {
            return process;
        }
        ReservationStatus previous = process.getStatus();
        process.setStatus(ReservationStatus.EXPIRED);
        process.setFailureReason(PROCESS_EXPIRED);
        try {
            ReservationProcess saved = reservationProcessRepository.save(process);
            LOG.info("proceso de reserva {}: {} -> EXPIRED (pasó expiresAt {})", process.getId(), previous, process.getExpiresAt());
            return saved;
        } catch (ConcurrentReservationUpdateException e) {
            // Un evento de Kafka actualizó el proceso al mismo tiempo: se devuelve el estado que dejó el evento.
            return reservationProcessRepository.findById(process.getId()).orElseThrow(() -> e);
        }
    }
}
