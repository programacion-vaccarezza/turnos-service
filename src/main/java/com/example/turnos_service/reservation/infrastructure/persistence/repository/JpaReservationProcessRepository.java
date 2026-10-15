package com.example.turnos_service.reservation.infrastructure.persistence.repository;

import com.example.turnos_service.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaReservationProcessRepository extends JpaRepository<ReservationProcessEntity, Long> {

    Optional<ReservationProcessEntity> findByReservationProcessId(String reservationProcessId);

}
