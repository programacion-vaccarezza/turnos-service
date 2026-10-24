package com.example.turnos_service.reservation.infrastructure.client.dto;

public record CatalogProfessionalResponse(Long id, Long categoryId, String firstName, String lastName, boolean enabled) {
}
