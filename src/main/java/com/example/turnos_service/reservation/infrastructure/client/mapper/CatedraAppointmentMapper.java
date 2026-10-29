package com.example.turnos_service.reservation.infrastructure.client.mapper;

import com.example.turnos_service.reservation.domain.model.CatedraAppointment;
import com.example.turnos_service.reservation.infrastructure.client.dto.CatedraAppointmentResponse;
import org.springframework.stereotype.Component;

@Component
public class CatedraAppointmentMapper {

    public CatedraAppointment toDomain(CatedraAppointmentResponse response) {
        if (response == null) {
            return null;
        }
        return CatedraAppointment.builder()
                .reservationId(response.reservationId())
                .reservationProcessId(response.reservationProcessId())
                .externalPatientId(response.externalPatientId())
                .appointmentDate(response.appointmentDate())
                .startTime(response.startTime())
                .endTime(response.endTime())
                .professionalId(response.professionalId())
                .professionalFirstName(response.professionalFirstName())
                .professionalLastName(response.professionalLastName())
                .status(response.status())
                .createdAt(response.createdAt())
                .confirmedAt(response.confirmedAt())
                .cancelledAt(response.cancelledAt())
                .cancellationReason(response.cancellationReason())
                .build();
    }
}
