package com.example.turnos_service.shared.domain.ports.out;

public interface TokenValidator {
    boolean isTokenValid(String token);
    String getLoginFromToken(String token);
}