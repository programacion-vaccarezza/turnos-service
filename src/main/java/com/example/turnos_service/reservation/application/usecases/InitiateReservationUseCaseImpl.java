package com.example.turnos_service.reservation.application.usecases;

import com.example.turnos_service.reservation.application.exception.CatedraRejectedException;
import com.example.turnos_service.reservation.application.exception.ConcurrentReservationUpdateException;
import com.example.turnos_service.reservation.application.exception.CatedraUnavailableException;
import com.example.turnos_service.reservation.application.exception.InvalidStateTransitionException;
import com.example.turnos_service.reservation.application.exception.PatientNotFoundException;
import com.example.turnos_service.reservation.application.exception.ProfessionalDisabledException;
import com.example.turnos_service.reservation.application.exception.ProfessionalNotFoundException;
import com.example.turnos_service.reservation.application.exception.ReservationRejectedException;
import com.example.turnos_service.reservation.domain.model.Hold;
import com.example.turnos_service.reservation.domain.model.HoldConfirmation;
import com.example.turnos_service.reservation.domain.model.PatientInfo;
import com.example.turnos_service.reservation.domain.model.ProfessionalInfo;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.ports.in.InitiateReservationUseCase;
import com.example.turnos_service.reservation.domain.ports.out.CatalogProfessionalPort;
import com.example.turnos_service.reservation.domain.ports.out.CatalogUserPort;
import com.example.turnos_service.reservation.domain.ports.out.CatedraHoldPort;
import com.example.turnos_service.reservation.domain.ports.out.ReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Inicia una reserva: valida el profesional y el usuario en catalogo-service, guarda el proceso local,
 * crea el hold en la cátedra y lo confirma, avanzando la máquina de estados según cada resultado.
 * Cada transición se valida antes de llamar a la cátedra.
 */
@Component
@RequiredArgsConstructor
public class InitiateReservationUseCaseImpl implements InitiateReservationUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(InitiateReservationUseCaseImpl.class);
    private static final String ALREADY_CONFIRMED = "RESERVATION_PROCESS_ALREADY_CONFIRMED";
    private static final String HOLD_EXPIRED = "HOLD_EXPIRED";
    private static final String CATEDRA_UNAVAILABLE = "CATEDRA_UNAVAILABLE";

    private final CatalogProfessionalPort catalogProfessionalPort;
    private final CatalogUserPort catalogUserPort;
    private final ReservationProcessRepository reservationProcessRepository;
    private final CatedraHoldPort catedraHoldPort;

    @Override
    public ReservationProcess initiateReservation(String ownerLogin, Long professionalId, LocalDate date, LocalTime startTime) {
        ProfessionalInfo professional = catalogProfessionalPort.findById(professionalId)
                .orElseThrow(() -> new ProfessionalNotFoundException(professionalId));
        if (!professional.isEnabled()) {
            throw new ProfessionalDisabledException(professionalId);
        }
        PatientInfo patient = catalogUserPort.findByLogin(ownerLogin)
                .orElseThrow(() -> new PatientNotFoundException(ownerLogin));

        ReservationProcess process = reservationProcessRepository.save(ReservationProcess.builder()
                .ownerLogin(ownerLogin)
                .professionalId(professionalId)
                .appointmentDate(date)
                .startTime(startTime)
                .patientFirstName(patient.getFirstName())
                .patientLastName(patient.getLastName())
                .status(ReservationStatus.INITIATED)
                .build());
        LOG.info("proceso de reserva {} iniciado por {} para el profesional {} el {} a las {}",
                process.getId(), ownerLogin, professionalId, date, startTime);

        process = createHold(process);
        return confirmHold(process);
    }

    private ReservationProcess createHold(ReservationProcess process) {
        validateTransition(process, ReservationStatus.HOLD_CREATED);
        Hold hold;
        try {
            hold = catedraHoldPort.createHold(process.getProfessionalId(), process.getAppointmentDate(),
                    process.getStartTime(), process.getOwnerLogin());
        } catch (CatedraRejectedException e) {
            ReservationProcess failed = changeStatus(process, ReservationStatus.FAILED, e.getCode());
            throw new ReservationRejectedException(failed.getId(), failed.getStatus(), e.getCode());
        } catch (CatedraUnavailableException e) {
            // Si el hold llegó a crearse, la cátedra lo vence sola: el proceso local no puede continuar.
            changeStatus(process, ReservationStatus.FAILED, CATEDRA_UNAVAILABLE);
            throw e;
        }
        process.setHoldId(hold.getHoldId());
        process.setReservationProcessId(hold.getReservationProcessId());
        process.setExpiresAt(hold.getExpiresAt());
        return changeStatus(process, ReservationStatus.HOLD_CREATED, null);
    }

    private ReservationProcess confirmHold(ReservationProcess process) {
        validateTransition(process, ReservationStatus.CONFIRMING);
        process = changeStatus(process, ReservationStatus.CONFIRMING, null);
        validateTransition(process, ReservationStatus.WAITING_FOR_PHONE);
        try {
            HoldConfirmation confirmation = catedraHoldPort.confirmHold(process.getHoldId(), process.getOwnerLogin(),
                    process.getPatientFirstName(), process.getPatientLastName());
            if (confirmation.getExpiresAt() != null) {
                process.setExpiresAt(confirmation.getExpiresAt());
            }
            return moveToWaitingForPhone(process);
        } catch (CatedraRejectedException e) {
            return handleConfirmRejection(process, e);
        } catch (CatedraUnavailableException e) {
            // Un timeout no prueba que la confirmación falló (anexo §18.4): queda en CONFIRMING y converge después.
            LOG.warn("sin respuesta de la cátedra al confirmar el proceso {}, queda en CONFIRMING", process.getId());
            return process;
        }
    }

    private ReservationProcess handleConfirmRejection(ReservationProcess process, CatedraRejectedException e) {
        if (ALREADY_CONFIRMED.equals(e.getCode())) {
            return moveToWaitingForPhone(process);
        }
        ReservationStatus finalStatus = HOLD_EXPIRED.equals(e.getCode()) ? ReservationStatus.EXPIRED : ReservationStatus.FAILED;
        ReservationProcess rejected = changeStatus(process, finalStatus, e.getCode());
        throw new ReservationRejectedException(rejected.getId(), rejected.getStatus(), e.getCode());
    }

    /**
     * AdditionalInformationRequested puede llegar por Kafka antes que la respuesta del confirm (enunciado §7)
     * y ya haber pasado el proceso a WAITING_FOR_PHONE: en ese caso se devuelve el estado guardado.
     */
    private ReservationProcess moveToWaitingForPhone(ReservationProcess process) {
        try {
            return changeStatus(process, ReservationStatus.WAITING_FOR_PHONE, null);
        } catch (ConcurrentReservationUpdateException e) {
            ReservationProcess current = reservationProcessRepository.findById(process.getId()).orElseThrow(() -> e);
            if (current.getStatus() == ReservationStatus.WAITING_FOR_PHONE) {
                LOG.info("proceso de reserva {}: ya estaba en WAITING_FOR_PHONE por el evento de Kafka", process.getId());
                return current;
            }
            throw e;
        }
    }

    private void validateTransition(ReservationProcess process, ReservationStatus next) {
        if (!process.getStatus().canTransitionTo(next)) {
            throw new InvalidStateTransitionException(process.getStatus(), next);
        }
    }

    private ReservationProcess changeStatus(ReservationProcess process, ReservationStatus next, String failureReason) {
        validateTransition(process, next);
        LOG.info("proceso de reserva {}: {} -> {}{}", process.getId(), process.getStatus(), next,
                failureReason != null ? " (" + failureReason + ")" : "");
        process.setStatus(next);
        process.setFailureReason(failureReason);
        return reservationProcessRepository.save(process);
    }
}
