package com.example.turnos_service.reservation.domain.ports.in;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;

public interface SubmitPhoneUseCase {
    ReservationProcess submitPhone(String ownerLogin, Long processId, String phoneNumber);
}
