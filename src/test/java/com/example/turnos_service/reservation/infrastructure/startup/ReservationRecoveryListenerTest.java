package com.example.turnos_service.reservation.infrastructure.startup;

import com.example.turnos_service.reservation.application.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReservationRecoveryListenerTest {

    @Mock
    ReservationService reservationService;

    @InjectMocks
    ReservationRecoveryListener listener;

    @Test
    void debeReconciliarLosProcesosAbiertos_cuandoLaAplicacionTerminaDeArrancar() {
        listener.onApplicationReady();

        verify(reservationService).reconcileOpenReservations();
    }
}
