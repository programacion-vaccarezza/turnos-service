package com.example.turnos_service.reservation.application.usecases;

import com.example.turnos_service.reservation.application.exception.ConcurrentReservationUpdateException;
import com.example.turnos_service.reservation.application.exception.InvalidPhoneNumberException;
import com.example.turnos_service.reservation.application.exception.InvalidStateTransitionException;
import com.example.turnos_service.reservation.application.exception.PhoneNotRequestedException;
import com.example.turnos_service.reservation.application.exception.ReservationNotFoundException;
import com.example.turnos_service.reservation.application.exception.ReservationRejectedException;
import com.example.turnos_service.reservation.application.service.ReservationExpirationService;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.ports.in.SubmitPhoneUseCase;
import com.example.turnos_service.reservation.domain.ports.out.PhoneSubmissionPublisher;
import com.example.turnos_service.reservation.domain.ports.out.ReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * El usuario envía su teléfono (anexo §15.5). Todas las validaciones se hacen ANTES de publicar en Kafka:
 * dueño, formato, que la cátedra lo haya pedido, vencimiento y transición de estado.
 */
@Component
@RequiredArgsConstructor
public class SubmitPhoneUseCaseImpl implements SubmitPhoneUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(SubmitPhoneUseCaseImpl.class);
    /** Formato que exige la cátedra después de quitar espacios, guiones, paréntesis y puntos (anexo §15.5). */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{7,15}$");

    private final ReservationProcessRepository reservationProcessRepository;
    private final PhoneSubmissionPublisher phoneSubmissionPublisher;
    private final ReservationExpirationService reservationExpirationService;

    @Override
    public ReservationProcess submitPhone(String ownerLogin, Long processId, String phoneNumber) {
        ReservationProcess process = reservationProcessRepository.findById(processId)
                .filter(p -> p.getOwnerLogin().equals(ownerLogin))
                .orElseThrow(() -> new ReservationNotFoundException(processId));

        String normalizedPhone = normalize(phoneNumber);
        if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            throw new InvalidPhoneNumberException();
        }
        // La copia local no extiende el hold (enunciado §7): si ya venció se cierra sin enviar nada.
        process = reservationExpirationService.expireIfOverdue(process);
        if (process.getStatus() == ReservationStatus.EXPIRED) {
            throw new ReservationRejectedException(process.getId(), ReservationStatus.EXPIRED,
                    ReservationExpirationService.PROCESS_EXPIRED);
        }
        if (!process.getStatus().canTransitionTo(ReservationStatus.PHONE_SUBMITTED)) {
            throw new InvalidStateTransitionException(process.getStatus(), ReservationStatus.PHONE_SUBMITTED);
        }
        if (process.getRequestEventId() == null) {
            throw new PhoneNotRequestedException(processId);
        }

        String eventId = "phone-submitted-" + UUID.randomUUID();
        phoneSubmissionPublisher.publishPhone(process, eventId, normalizedPhone);
        LOG.info("teléfono enviado a la cátedra para el proceso de reserva {} (eventId {})", processId, eventId);

        ReservationStatus previous = process.getStatus();
        process.setStatus(ReservationStatus.PHONE_SUBMITTED);
        process.setFailureReason(null);
        try {
            ReservationProcess saved = reservationProcessRepository.save(process);
            LOG.info("proceso de reserva {}: {} -> PHONE_SUBMITTED", processId, previous);
            return saved;
        } catch (ConcurrentReservationUpdateException e) {
            // El resultado de la cátedra llegó por Kafka antes de guardar: se devuelve el estado ya actualizado.
            return reservationProcessRepository.findById(processId).orElseThrow(() -> e);
        }
    }

    private String normalize(String phoneNumber) {
        return phoneNumber == null ? "" : phoneNumber.replaceAll("[\\s\\-().]", "");
    }
}
