package com.example.turnos_service.reservation.domain.ports.out;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;

import java.util.List;
import java.util.Optional;

public interface ReservationProcessRepository {

    ReservationProcess save(ReservationProcess reservationProcess);
    Optional<ReservationProcess> findById(Long id);
    Optional<ReservationProcess> findByReservationProcessId(String reservationProcessId);
    List<ReservationProcess> findByOwnerLogin(String ownerLogin);

}
