package com.example.turnos_service.reservation.domain.ports.in;

import com.example.turnos_service.reservation.domain.model.UserReservation;

import java.util.List;

public interface GetMyReservationsUseCase {
    List<UserReservation> getMyReservations(String ownerLogin);
}
