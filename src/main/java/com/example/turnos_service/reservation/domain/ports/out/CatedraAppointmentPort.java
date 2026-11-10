package com.example.turnos_service.reservation.domain.ports.out;

import com.example.turnos_service.reservation.domain.model.CatedraAppointment;

import java.util.List;

// Port para listar y cancelar reservas en la cátedra
public interface CatedraAppointmentPort {
    // Devuelve las reservas del paciente
    List<CatedraAppointment> findByExternalPatientId(String externalPatientId);
    // Cancela la reserva en la cátedra
    void cancel(String reservationProcessId, String reason);
}
