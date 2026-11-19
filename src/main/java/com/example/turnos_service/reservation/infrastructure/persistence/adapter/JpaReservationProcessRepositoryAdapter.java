package com.example.turnos_service.reservation.infrastructure.persistence.adapter;

import com.example.turnos_service.reservation.application.exception.ConcurrentReservationUpdateException;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.ports.out.ReservationProcessRepository;
import com.example.turnos_service.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import com.example.turnos_service.reservation.infrastructure.persistence.mapper.ReservationProcessMapper;
import com.example.turnos_service.reservation.infrastructure.persistence.repository.JpaReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// Adapter que guarda y busca procesos de reserva con JPA
@Component
@RequiredArgsConstructor
public class JpaReservationProcessRepositoryAdapter implements ReservationProcessRepository {

    private final JpaReservationProcessRepository jpaReservationProcessRepository;
    private final ReservationProcessMapper reservationProcessMapper;

    // Guarda el proceso; un choque de versiones se informa como actualización concurrente
    @Override
    public ReservationProcess save(ReservationProcess reservationProcess) {
        ReservationProcessEntity entity = reservationProcessMapper.toEntity(reservationProcess);
        try {
            return reservationProcessMapper.toDomainModel(jpaReservationProcessRepository.save(entity));
        } catch (OptimisticLockingFailureException e) {
            throw new ConcurrentReservationUpdateException(reservationProcess.getId(), e);
        }
    }

    // Busca el proceso por su id
    @Override
    public Optional<ReservationProcess> findById(Long id) {
        return jpaReservationProcessRepository.findById(id)
                .map(reservationProcessMapper::toDomainModel);
    }

    // Busca el proceso por el id de la cátedra
    @Override
    public Optional<ReservationProcess> findByReservationProcessId(String reservationProcessId) {
        return jpaReservationProcessRepository.findByReservationProcessId(reservationProcessId)
                .map(reservationProcessMapper::toDomainModel);
    }

    // Devuelve los procesos del usuario, del turno más reciente al más viejo
    @Override
    public List<ReservationProcess> findByOwnerLogin(String ownerLogin) {
        return jpaReservationProcessRepository.findByOwnerLoginOrderByAppointmentDateDescStartTimeDesc(ownerLogin).stream()
                .map(reservationProcessMapper::toDomainModel)
                .toList();
    }

    // Devuelve los procesos en los estados indicados
    @Override
    public List<ReservationProcess> findByStatusIn(Collection<ReservationStatus> statuses) {
        return jpaReservationProcessRepository.findByStatusIn(statuses).stream()
                .map(reservationProcessMapper::toDomainModel)
                .toList();
    }
}
