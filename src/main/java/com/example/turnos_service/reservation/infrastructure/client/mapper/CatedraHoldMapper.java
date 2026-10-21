package com.example.turnos_service.reservation.infrastructure.client.mapper;

import com.example.turnos_service.reservation.domain.model.Hold;
import com.example.turnos_service.reservation.infrastructure.client.dto.CreateHoldRequest;
import com.example.turnos_service.reservation.infrastructure.client.dto.CreateHoldResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Component
public class CatedraHoldMapper {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public CreateHoldRequest toRequest(Long professionalId, LocalDate date, LocalTime startTime, String externalPatientId) {
        return new CreateHoldRequest(professionalId, date.toString(), startTime.format(TIME_FORMAT), externalPatientId);
    }

    public Hold toDomain(CreateHoldResponse response) {
        if (response == null) {
            return null;
        }
        return Hold.builder()
                .holdId(response.holdId())
                .reservationProcessId(response.reservationProcessId())
                .expiresAt(response.expiresAt())
                .build();
    }
}
