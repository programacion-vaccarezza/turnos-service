package com.example.turnos_service.reservation.domain.ports.out;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// Port para guardar y buscar procesos de reserva
public interface ReservationProcessRepository {

    // Guarda el proceso
    ReservationProcess save(ReservationProcess reservationProcess);
    // Busca el proceso por su id local
    Optional<ReservationProcess> findById(Long id);
    // Busca el proceso por el id que le asignó la cátedra
    Optional<ReservationProcess> findByReservationProcessId(String reservationProcessId);
    // Devuelve los procesos del usuario
    List<ReservationProcess> findByOwnerLogin(String ownerLogin);
    // Devuelve los procesos en los estados indicados
    List<ReservationProcess> findByStatusIn(Collection<ReservationStatus> statuses);

}
