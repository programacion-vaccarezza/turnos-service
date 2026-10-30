package com.example.turnos_service.shared.infrastructure.client.catedra.dto;

/**
 * Cuerpo application/problem+json de los errores de la cátedra (anexo §13).
 * Solo se mapean los campos que se usan; el resto se ignora.
 */
public record CatedraProblemResponse(Integer status, String code, String detail) {
}
