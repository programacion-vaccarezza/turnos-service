package com.example.turnos_service.reservation.infrastructure.web.mapper;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.infrastructure.web.dto.ReservationResponse;
import org.springframework.stereotype.Component;

@Component
public class ReservationDtoMapper {

    public ReservationResponse toResponse(ReservationProcess process) {
        if (process == null) {
            return null;
        }
        return ReservationResponse.builder()
                .id(process.getId())
                .professionalId(process.getProfessionalId())
                .date(process.getAppointmentDate())
                .startTime(process.getStartTime())
                .status(process.getStatus())
                .expiresAt(process.getExpiresAt())
                .failureReason(process.getFailureReason())
                .build();
    }
}
