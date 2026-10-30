package com.example.turnos_service.reservation.domain.ports.out;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;

public interface PhoneSubmissionPublisher {
    void publishPhone(ReservationProcess process, String eventId, String phoneNumber);
}
