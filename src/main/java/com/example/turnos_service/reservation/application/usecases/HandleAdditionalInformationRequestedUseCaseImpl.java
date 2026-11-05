package com.example.turnos_service.reservation.application.usecases;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.ports.in.HandleAdditionalInformationRequestedUseCase;
import com.example.turnos_service.reservation.domain.ports.out.ReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * La cátedra pide el teléfono (anexo §15.4). Se guarda el eventId como requestEventId, necesario para
 * responder, y el vencimiento. El evento puede llegar antes o después del 202 del confirm (enunciado §7):
 * desde CONFIRMING avanza a WAITING_FOR_PHONE; si ya estaba ahí solo se guardan los datos.
 * En cualquier otro estado se ignora sin excepción.
 */
@Component
@RequiredArgsConstructor
public class HandleAdditionalInformationRequestedUseCaseImpl implements HandleAdditionalInformationRequestedUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(HandleAdditionalInformationRequestedUseCaseImpl.class);

    private final ReservationProcessRepository reservationProcessRepository;

    @Override
    public void handleAdditionalInformationRequested(String reservationProcessId, String requestEventId, Instant expiresAt) {
        Optional<ReservationProcess> found = reservationProcessRepository.findByReservationProcessId(reservationProcessId);
        if (found.isEmpty()) {
            LOG.info("pedido de teléfono para el proceso {} que turnos-service no conoce, se ignora", reservationProcessId);
            return;
        }
        ReservationProcess process = found.get();
        ReservationStatus current = process.getStatus();

        if (current == ReservationStatus.WAITING_FOR_PHONE) {
            saveRequestData(process, requestEventId, expiresAt);
            LOG.info("proceso de reserva {}: pedido de teléfono registrado (ya esperaba el teléfono)", process.getId());
            return;
        }
        if (!current.canTransitionTo(ReservationStatus.WAITING_FOR_PHONE)) {
            LOG.info("proceso de reserva {} en {}: el pedido de teléfono no aplica, se ignora", process.getId(), current);
            return;
        }
        process.setStatus(ReservationStatus.WAITING_FOR_PHONE);
        saveRequestData(process, requestEventId, expiresAt);
        LOG.info("proceso de reserva {}: {} -> WAITING_FOR_PHONE (pedido de teléfono)", process.getId(), current);
    }

    private void saveRequestData(ReservationProcess process, String requestEventId, Instant expiresAt) {
        process.setRequestEventId(requestEventId);
        if (expiresAt != null) {
            process.setExpiresAt(expiresAt);
        }
        reservationProcessRepository.save(process);
    }
}
