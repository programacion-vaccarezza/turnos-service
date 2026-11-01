package com.example.turnos_service.reservation.infrastructure.startup;

import com.example.turnos_service.reservation.application.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Al terminar de arrancar, reconcilia con la cátedra los procesos que quedaron abiertos.
 * Un error acá no puede frenar el servicio: una excepción que sale de este evento hace que Spring cierre la aplicación.
 */
@Component
@RequiredArgsConstructor
public class ReservationRecoveryListener {

    private static final Logger LOG = LoggerFactory.getLogger(ReservationRecoveryListener.class);

    private final ReservationService reservationService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            reservationService.reconcileOpenReservations();
        } catch (RuntimeException e) {
            LOG.error("falló la reconciliación al arrancar, el servicio sigue funcionando", e);
        }
    }
}
