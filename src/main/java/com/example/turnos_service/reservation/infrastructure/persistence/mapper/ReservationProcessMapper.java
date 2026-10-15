package com.example.turnos_service.reservation.infrastructure.persistence.mapper;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import org.springframework.stereotype.Component;

@Component
public class ReservationProcessMapper {

    public ReservationProcessEntity toEntity(ReservationProcess reservationProcess) {
        if (reservationProcess == null) {
            return null;
        }
        return ReservationProcessEntity.builder()
                .id(reservationProcess.getId())
                .ownerLogin(reservationProcess.getOwnerLogin())
                .professionalId(reservationProcess.getProfessionalId())
                .appointmentDate(reservationProcess.getAppointmentDate())
                .startTime(reservationProcess.getStartTime())
                .patientFirstName(reservationProcess.getPatientFirstName())
                .patientLastName(reservationProcess.getPatientLastName())
                .status(reservationProcess.getStatus())
                .holdId(reservationProcess.getHoldId())
                .reservationProcessId(reservationProcess.getReservationProcessId())
                .expiresAt(reservationProcess.getExpiresAt())
                .requestEventId(reservationProcess.getRequestEventId())
                .reservationId(reservationProcess.getReservationId())
                .failureReason(reservationProcess.getFailureReason())
                .createdAt(reservationProcess.getCreatedAt())
                .updatedAt(reservationProcess.getUpdatedAt())
                .version(reservationProcess.getVersion())
                .build();
    }

    public ReservationProcess toDomainModel(ReservationProcessEntity entity) {
        if (entity == null) {
            return null;
        }
        return ReservationProcess.builder()
                .id(entity.getId())
                .ownerLogin(entity.getOwnerLogin())
                .professionalId(entity.getProfessionalId())
                .appointmentDate(entity.getAppointmentDate())
                .startTime(entity.getStartTime())
                .patientFirstName(entity.getPatientFirstName())
                .patientLastName(entity.getPatientLastName())
                .status(entity.getStatus())
                .holdId(entity.getHoldId())
                .reservationProcessId(entity.getReservationProcessId())
                .expiresAt(entity.getExpiresAt())
                .requestEventId(entity.getRequestEventId())
                .reservationId(entity.getReservationId())
                .failureReason(entity.getFailureReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }
}
