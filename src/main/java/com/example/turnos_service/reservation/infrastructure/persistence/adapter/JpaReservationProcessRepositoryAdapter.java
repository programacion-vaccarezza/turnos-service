package com.example.turnos_service.reservation.infrastructure.persistence.adapter;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.ports.out.ReservationProcessRepository;
import com.example.turnos_service.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import com.example.turnos_service.reservation.infrastructure.persistence.mapper.ReservationProcessMapper;
import com.example.turnos_service.reservation.infrastructure.persistence.repository.JpaReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaReservationProcessRepositoryAdapter implements ReservationProcessRepository {

    private final JpaReservationProcessRepository jpaReservationProcessRepository;
    private final ReservationProcessMapper reservationProcessMapper;

    @Override
    public ReservationProcess save(ReservationProcess reservationProcess) {
        ReservationProcessEntity entity = reservationProcessMapper.toEntity(reservationProcess);
        return reservationProcessMapper.toDomainModel(jpaReservationProcessRepository.save(entity));
    }

    @Override
    public Optional<ReservationProcess> findById(Long id) {
        return jpaReservationProcessRepository.findById(id)
                .map(reservationProcessMapper::toDomainModel);
    }

    @Override
    public Optional<ReservationProcess> findByReservationProcessId(String reservationProcessId) {
        return jpaReservationProcessRepository.findByReservationProcessId(reservationProcessId)
                .map(reservationProcessMapper::toDomainModel);
    }
}
