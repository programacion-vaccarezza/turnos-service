package com.example.turnos_service.reservation.infrastructure.client.dto;

// Usuario devuelto por catalogo-service
public record CatalogUserResponse(String login, String firstName, String lastName) {
}
