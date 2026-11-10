package com.example.turnos_service.reservation.domain.ports.in;

import com.example.turnos_service.reservation.domain.model.UserReservation;

import java.util.List;

// Caso de uso: listar las reservas del usuario
public interface GetMyReservationsUseCase {
    // Devuelve las reservas del usuario
    List<UserReservation> getMyReservations(String ownerLogin);
}
