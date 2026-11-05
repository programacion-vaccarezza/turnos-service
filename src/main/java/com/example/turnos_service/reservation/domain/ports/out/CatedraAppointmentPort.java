package com.example.turnos_service.reservation.domain.ports.out;

import com.example.turnos_service.reservation.domain.model.CatedraAppointment;

import java.util.List;

public interface CatedraAppointmentPort {
    List<CatedraAppointment> findByExternalPatientId(String externalPatientId);
    void cancel(String reservationProcessId, String reason);
}
