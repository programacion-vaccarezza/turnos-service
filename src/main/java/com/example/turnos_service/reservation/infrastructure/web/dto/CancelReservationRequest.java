package com.example.turnos_service.reservation.infrastructure.web.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelReservationRequest {

    @Size(max = 500, message = "El motivo debe tener como máximo 500 caracteres")
    private String reason;
}
