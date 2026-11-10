package com.example.turnos_service.reservation.domain.ports.out;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;

// Port para enviar el teléfono a la cátedra
public interface PhoneSubmissionPublisher {
    // Publica el teléfono del proceso
    void publishPhone(ReservationProcess process, String eventId, String phoneNumber);
}
