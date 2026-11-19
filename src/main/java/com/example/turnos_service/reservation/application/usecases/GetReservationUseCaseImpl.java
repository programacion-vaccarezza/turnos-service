package com.example.turnos_service.reservation.application.usecases;

import com.example.turnos_service.reservation.application.exception.CatedraUnavailableException;
import com.example.turnos_service.reservation.application.exception.ReservationNotFoundException;
import com.example.turnos_service.reservation.application.service.ReservationExpirationService;
import com.example.turnos_service.reservation.application.service.ReservationReconciler;
import com.example.turnos_service.reservation.domain.model.CatedraAppointment;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.ports.in.GetReservationUseCase;
import com.example.turnos_service.reservation.domain.ports.out.CatedraAppointmentPort;
import com.example.turnos_service.reservation.domain.ports.out.ReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

// Devuelve el estado local de una reserva del usuario
@Component
@RequiredArgsConstructor
public class GetReservationUseCaseImpl implements GetReservationUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(GetReservationUseCaseImpl.class);

    private final ReservationProcessRepository reservationProcessRepository;
    private final ReservationExpirationService reservationExpirationService;
    private final CatedraAppointmentPort catedraAppointmentPort;
    private final ReservationReconciler reservationReconciler;

    // Valida el dueño, vence el proceso si corresponde y lo reconcilia si su resultado es desconocido
    @Override
    public ReservationProcess getReservation(String ownerLogin, Long processId) {
        ReservationProcess process = reservationProcessRepository.findById(processId)
                .filter(p -> p.getOwnerLogin().equals(ownerLogin))
                .orElseThrow(() -> new ReservationNotFoundException(processId));
        process = reservationExpirationService.expireIfOverdue(process);
        return process.needsReconciliation(Instant.now()) ? reconcile(process) : process;
    }

    // Busca la reserva en la cátedra y actualiza el proceso; si la cátedra no responde, lo deja igual
    private ReservationProcess reconcile(ReservationProcess process) {
        try {
            CatedraAppointment appointment = catedraAppointmentPort.findByExternalPatientId(process.getOwnerLogin()).stream()
                    .filter(a -> process.getReservationProcessId().equals(a.getReservationProcessId()))
                    .findFirst()
                    .orElse(null);
            return reservationReconciler.reconcile(process, appointment);
        } catch (CatedraUnavailableException e) {
            LOG.warn("cátedra no disponible, el proceso {} sigue en {}", process.getId(), process.getStatus());
            return process;
        }
    }
}
