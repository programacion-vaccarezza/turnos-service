package com.example.turnos_service.reservation.infrastructure.persistence.repository;

import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// Repositorio JPA de procesos de reserva
@Repository
public interface JpaReservationProcessRepository extends JpaRepository<ReservationProcessEntity, Long> {

    // Busca el proceso por el id de la cátedra
    Optional<ReservationProcessEntity> findByReservationProcessId(String reservationProcessId);

    // Devuelve los procesos del usuario, del turno más reciente al más viejo
    List<ReservationProcessEntity> findByOwnerLoginOrderByAppointmentDateDescStartTimeDesc(String ownerLogin);

    // Devuelve los procesos en los estados indicados
    List<ReservationProcessEntity> findByStatusIn(Collection<ReservationStatus> statuses);

}
