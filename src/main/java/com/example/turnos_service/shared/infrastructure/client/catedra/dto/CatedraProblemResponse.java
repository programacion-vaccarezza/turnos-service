package com.example.turnos_service.shared.infrastructure.client.catedra.dto;

// Cuerpo de error que devuelve la API de la cátedra
public record CatedraProblemResponse(Integer status, String code, String detail) {
}
