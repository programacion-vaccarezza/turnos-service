package com.example.turnos_service.reservation.domain.ports.out;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationProcessRepository {

    ReservationProcess save(ReservationProcess reservationProcess);
    Optional<ReservationProcess> findById(Long id);
    Optional<ReservationProcess> findByReservationProcessId(String reservationProcessId);
    List<ReservationProcess> findByOwnerLogin(String ownerLogin);
    List<ReservationProcess> findByStatusIn(Collection<ReservationStatus> statuses);

}
