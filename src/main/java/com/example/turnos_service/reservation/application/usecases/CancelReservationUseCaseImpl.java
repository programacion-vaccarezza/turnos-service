package com.example.turnos_service.reservation.application.usecases;

import com.example.turnos_service.reservation.application.exception.ConcurrentReservationUpdateException;
import com.example.turnos_service.reservation.application.exception.InvalidStateTransitionException;
import com.example.turnos_service.reservation.application.exception.ReservationNotFoundException;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.ports.in.CancelReservationUseCase;
import com.example.turnos_service.reservation.domain.ports.out.CatedraAppointmentPort;
import com.example.turnos_service.reservation.domain.ports.out.ReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * El usuario cancela una reserva propia confirmada (enunciado §14, anexo §12).
 * Dueño y transición se validan antes de llamar a la cátedra; una reserva ya cancelada se devuelve tal cual.
 */
@Component
@RequiredArgsConstructor
public class CancelReservationUseCaseImpl implements CancelReservationUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(CancelReservationUseCaseImpl.class);

    private final ReservationProcessRepository reservationProcessRepository;
    private final CatedraAppointmentPort catedraAppointmentPort;

    @Override
    public ReservationProcess cancelReservation(String ownerLogin, Long processId, String reason) {
        ReservationProcess process = reservationProcessRepository.findById(processId)
                .filter(p -> p.getOwnerLogin().equals(ownerLogin))
                .orElseThrow(() -> new ReservationNotFoundException(processId));

        if (process.getStatus() == ReservationStatus.CANCELLED) {
            LOG.info("proceso de reserva {}: ya estaba CANCELLED", processId);
            return process;
        }
        if (!process.getStatus().canTransitionTo(ReservationStatus.CANCELLED)) {
            throw new InvalidStateTransitionException(process.getStatus(), ReservationStatus.CANCELLED);
        }

        catedraAppointmentPort.cancel(process.getReservationProcessId(), reason);

        process.setStatus(ReservationStatus.CANCELLED);
        try {
            ReservationProcess saved = reservationProcessRepository.save(process);
            LOG.info("proceso de reserva {}: CONFIRMED -> CANCELLED", processId);
            return saved;
        } catch (ConcurrentReservationUpdateException e) {
            // AppointmentCancelled llegó por Kafka antes de guardar: se devuelve el estado ya actualizado.
            return reservationProcessRepository.findById(processId).orElseThrow(() -> e);
        }
    }
}
