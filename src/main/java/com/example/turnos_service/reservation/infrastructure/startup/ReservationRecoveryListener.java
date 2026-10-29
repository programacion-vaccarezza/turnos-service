package com.example.turnos_service.reservation.infrastructure.startup;

import com.example.turnos_service.reservation.application.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Al terminar de arrancar, reconcilia con la cátedra los procesos que quedaron abiertos. */
@Component
@RequiredArgsConstructor
public class ReservationRecoveryListener {

    private final ReservationService reservationService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        reservationService.reconcileOpenReservations();
    }
}
