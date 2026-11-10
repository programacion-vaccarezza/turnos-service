package com.example.turnos_service.reservation.infrastructure.client.dto;

// Profesional devuelto por catalogo-service
public record CatalogProfessionalResponse(Long id, Long categoryId, String firstName, String lastName, boolean enabled) {
}
