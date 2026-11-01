package com.example.turnos_service.reservation.application.usecases;

import com.example.turnos_service.reservation.application.exception.ReservationNotFoundException;
import com.example.turnos_service.reservation.application.service.ReservationExpirationService;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.ports.in.GetReservationUseCase;
import com.example.turnos_service.reservation.domain.ports.out.ReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Devuelve el estado local de un proceso propio (enunciado §7): el teléfono y el resultado llegan por Kafka
 * al backend y la app lo consulta acá. Antes de responder se vence el proceso si ya pasó su expiresAt.
 */
@Component
@RequiredArgsConstructor
public class GetReservationUseCaseImpl implements GetReservationUseCase {

    private final ReservationProcessRepository reservationProcessRepository;
    private final ReservationExpirationService reservationExpirationService;

    @Override
    public ReservationProcess getReservation(String ownerLogin, Long processId) {
        ReservationProcess process = reservationProcessRepository.findById(processId)
                .filter(p -> p.getOwnerLogin().equals(ownerLogin))
                .orElseThrow(() -> new ReservationNotFoundException(processId));
        return reservationExpirationService.expireIfOverdue(process);
    }
}
