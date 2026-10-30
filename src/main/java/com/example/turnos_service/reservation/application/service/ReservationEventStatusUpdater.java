package com.example.turnos_service.reservation.application.service;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.ports.out.ReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Aplica a un proceso de reserva el cambio de estado que informa un evento de la cátedra.
 * Si el proceso no existe, ya está en ese estado o la transición no es válida (evento tardío,
 * duplicado o fuera de orden), el evento se ignora sin excepción y el proceso nunca retrocede (enunciado §7).
 */
@Component
@RequiredArgsConstructor
public class ReservationEventStatusUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(ReservationEventStatusUpdater.class);

    private final ReservationProcessRepository reservationProcessRepository;

    public void apply(String reservationProcessId, ReservationStatus next, String eventType,
                      Consumer<ReservationProcess> changes) {
        Optional<ReservationProcess> found = reservationProcessRepository.findByReservationProcessId(reservationProcessId);
        if (found.isEmpty()) {
            LOG.info("{} para el proceso {} que turnos-service no conoce, se ignora", eventType, reservationProcessId);
            return;
        }
        ReservationProcess process = found.get();
        ReservationStatus current = process.getStatus();
        if (current == next) {
            LOG.info("proceso de reserva {} ya está en {}, {} se ignora", process.getId(), current, eventType);
            return;
        }
        if (!current.canTransitionTo(next)) {
            LOG.info("proceso de reserva {} en {}: {} no aplica, se ignora", process.getId(), current, eventType);
            return;
        }
        changes.accept(process);
        process.setStatus(next);
        reservationProcessRepository.save(process);
        LOG.info("proceso de reserva {}: {} -> {} ({})", process.getId(), current, next, eventType);
    }
}
