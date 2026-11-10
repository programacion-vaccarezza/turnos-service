package com.example.turnos_service.reservation.infrastructure.client.mapper;

import com.example.turnos_service.reservation.domain.model.Hold;
import com.example.turnos_service.reservation.domain.model.HoldConfirmation;
import com.example.turnos_service.reservation.infrastructure.client.dto.ConfirmHoldRequest;
import com.example.turnos_service.reservation.infrastructure.client.dto.ConfirmHoldResponse;
import com.example.turnos_service.reservation.infrastructure.client.dto.CreateHoldRequest;
import com.example.turnos_service.reservation.infrastructure.client.dto.CreateHoldResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

// Arma los pedidos de hold para la cátedra y convierte sus respuestas
@Component
public class CatedraHoldMapper {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    // Arma el body del hold con la fecha y la hora en el formato de la cátedra
    public CreateHoldRequest toRequest(Long professionalId, LocalDate date, LocalTime startTime, String externalPatientId) {
        return new CreateHoldRequest(professionalId, date.toString(), startTime.format(TIME_FORMAT), externalPatientId);
    }

    // Convierte la respuesta del hold en Hold
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

    // Arma el body del confirm
    public ConfirmHoldRequest toConfirmRequest(String externalPatientId, String patientFirstName, String patientLastName) {
        return new ConfirmHoldRequest(externalPatientId, patientFirstName, patientLastName);
    }

    // Convierte la respuesta del confirm en HoldConfirmation
    public HoldConfirmation toDomain(ConfirmHoldResponse response) {
        if (response == null) {
            return null;
        }
        return HoldConfirmation.builder()
                .reservationProcessId(response.reservationProcessId())
                .expiresAt(response.expiresAt())
                .build();
    }
}
