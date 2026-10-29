package com.example.turnos_service.reservation.application.usecases;

import com.example.turnos_service.reservation.application.exception.CatedraUnavailableException;
import com.example.turnos_service.reservation.domain.model.CatedraAppointment;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.UserReservation;
import com.example.turnos_service.reservation.domain.ports.in.GetMyReservationsUseCase;
import com.example.turnos_service.reservation.domain.ports.out.CatedraAppointmentPort;
import com.example.turnos_service.reservation.domain.ports.out.ReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Devuelve las reservas del usuario autenticado. La cátedra lista las de toda la cuenta técnica (anexo §11),
 * así que cada reserva se cruza con los procesos locales del usuario y solo se incluyen las suyas.
 * Si la cátedra no está disponible, se responde con los procesos locales y su estado local.
 */
@Component
@RequiredArgsConstructor
public class GetMyReservationsUseCaseImpl implements GetMyReservationsUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(GetMyReservationsUseCaseImpl.class);

    private final ReservationProcessRepository reservationProcessRepository;
    private final CatedraAppointmentPort catedraAppointmentPort;

    @Override
    public List<UserReservation> getMyReservations(String ownerLogin) {
        List<ReservationProcess> processes = reservationProcessRepository.findByOwnerLogin(ownerLogin);
        try {
            List<CatedraAppointment> appointments = catedraAppointmentPort.findByExternalPatientId(ownerLogin);
            return fromCatedra(appointments, processes);
        } catch (CatedraUnavailableException e) {
            LOG.warn("cátedra no disponible, se devuelven las reservas locales de {}", ownerLogin);
            return processes.stream().map(this::fromLocal).toList();
        }
    }

    private List<UserReservation> fromCatedra(List<CatedraAppointment> appointments, List<ReservationProcess> processes) {
        Map<String, ReservationProcess> ownProcesses = processes.stream()
                .filter(process -> process.getReservationProcessId() != null)
                .collect(Collectors.toMap(ReservationProcess::getReservationProcessId, Function.identity()));
        return appointments.stream()
                .filter(appointment -> ownProcesses.containsKey(appointment.getReservationProcessId()))
                .map(appointment -> combine(appointment, ownProcesses.get(appointment.getReservationProcessId())))
                .toList();
    }

    private UserReservation combine(CatedraAppointment appointment, ReservationProcess process) {
        return UserReservation.builder()
                .id(process.getId())
                .reservationId(appointment.getReservationId())
                .professionalId(appointment.getProfessionalId())
                .professionalFirstName(appointment.getProfessionalFirstName())
                .professionalLastName(appointment.getProfessionalLastName())
                .date(appointment.getAppointmentDate())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .status(appointment.getStatus())
                .expiresAt(process.getExpiresAt())
                .failureReason(process.getFailureReason())
                .cancellationReason(appointment.getCancellationReason())
                .source(UserReservation.SOURCE_CATEDRA)
                .build();
    }

    private UserReservation fromLocal(ReservationProcess process) {
        return UserReservation.builder()
                .id(process.getId())
                .reservationId(process.getReservationId())
                .professionalId(process.getProfessionalId())
                .date(process.getAppointmentDate())
                .startTime(process.getStartTime())
                .status(process.getStatus().name())
                .expiresAt(process.getExpiresAt())
                .failureReason(process.getFailureReason())
                .source(UserReservation.SOURCE_LOCAL)
                .build();
    }
}
