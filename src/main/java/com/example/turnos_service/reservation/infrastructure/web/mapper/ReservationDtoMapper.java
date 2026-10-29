package com.example.turnos_service.reservation.infrastructure.web.mapper;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.UserReservation;
import com.example.turnos_service.reservation.infrastructure.web.dto.ReservationResponse;
import com.example.turnos_service.reservation.infrastructure.web.dto.UserReservationResponse;
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

    public UserReservationResponse toResponse(UserReservation reservation) {
        if (reservation == null) {
            return null;
        }
        return UserReservationResponse.builder()
                .id(reservation.getId())
                .reservationId(reservation.getReservationId())
                .professionalId(reservation.getProfessionalId())
                .professionalFirstName(reservation.getProfessionalFirstName())
                .professionalLastName(reservation.getProfessionalLastName())
                .date(reservation.getDate())
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .status(reservation.getStatus())
                .expiresAt(reservation.getExpiresAt())
                .failureReason(reservation.getFailureReason())
                .cancellationReason(reservation.getCancellationReason())
                .source(reservation.getSource())
                .build();
    }
}
