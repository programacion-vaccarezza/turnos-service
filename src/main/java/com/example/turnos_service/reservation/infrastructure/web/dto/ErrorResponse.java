package com.example.turnos_service.reservation.infrastructure.web.dto;

import lombok.*;

// Respuesta de error con su code
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private String code;
}
