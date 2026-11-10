package com.example.turnos_service.reservation.domain.ports.in;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;

// Caso de uso: enviar el teléfono pedido por la cátedra
public interface SubmitPhoneUseCase {
    // Publica el teléfono del usuario
    ReservationProcess submitPhone(String ownerLogin, Long processId, String phoneNumber);
}
