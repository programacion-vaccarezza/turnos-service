package com.example.turnos_service.shared.domain.ports.out;

// Port para validar el JWT del usuario y obtener su login
public interface TokenValidator {
    // Indica si el token tiene una firma válida y no está vencido
    boolean isTokenValid(String token);
    // Devuelve el login del usuario guardado en el token
    String getLoginFromToken(String token);
}