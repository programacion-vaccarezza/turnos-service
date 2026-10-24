package com.example.turnos_service.reservation.infrastructure.web.dto;

import lombok.*;

/**
 * Cuerpo de los errores conocidos de la API: un código estable por el que el cliente decide
 * (mismo criterio que la cátedra, anexo §13). Los errores inesperados no usan este cuerpo.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private String code;
}
