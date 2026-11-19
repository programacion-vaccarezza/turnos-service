package com.example.turnos_service.reservation.domain.ports.in;

// Caso de uso: recuperar los procesos abiertos
public interface ReconcileOpenReservationsUseCase {
    // Reconcilia los procesos abiertos con la cátedra
    void reconcileOpenReservations();
}
