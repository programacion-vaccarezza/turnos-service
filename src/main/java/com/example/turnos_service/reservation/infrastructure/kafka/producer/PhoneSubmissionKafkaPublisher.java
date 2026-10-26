package com.example.turnos_service.reservation.infrastructure.kafka.producer;

import com.example.turnos_service.reservation.application.exception.PhoneSubmissionFailedException;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.ports.out.PhoneSubmissionPublisher;
import com.example.turnos_service.reservation.infrastructure.kafka.dto.AdditionalInformationSubmittedEvent;
import com.example.turnos_service.shared.infrastructure.client.catedra.config.CatedraConfiguration;
import com.example.turnos_service.shared.infrastructure.kafka.config.CatedraKafkaConfiguration;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publica AdditionalInformationSubmitted en el topic de teléfono de la cátedra.
 * La clave del mensaje es el reservationProcessId, para que los eventos del mismo proceso
 * conserven el orden (anexo §15.1, Tema 13). Espera la confirmación del broker antes de
 * devolver, así el proceso solo pasa a PHONE_SUBMITTED si el evento realmente se publicó.
 */
@Component
@RequiredArgsConstructor
public class PhoneSubmissionKafkaPublisher implements PhoneSubmissionPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(PhoneSubmissionKafkaPublisher.class);
    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CatedraKafkaConfiguration kafkaConfiguration;
    private final CatedraConfiguration catedraConfiguration;

    @Override
    public void publishPhone(ReservationProcess process, String eventId, String phoneNumber) {
        AdditionalInformationSubmittedEvent event = new AdditionalInformationSubmittedEvent(
                eventId,
                AdditionalInformationSubmittedEvent.EVENT_TYPE,
                catedraConfiguration.groupId(),
                process.getReservationProcessId(),
                process.getHoldId(),
                process.getRequestEventId(),
                new AdditionalInformationSubmittedEvent.Information(phoneNumber),
                Instant.now(),
                AdditionalInformationSubmittedEvent.SCHEMA_VERSION);
        try {
            SendResult<String, Object> result = kafkaTemplate
                    .send(kafkaConfiguration.appointmentPhoneTopic(), process.getReservationProcessId(), event)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            LOG.info("publicado {} del proceso {} en la partición {} offset {}", eventId, process.getReservationProcessId(),
                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PhoneSubmissionFailedException("se interrumpió el envío del teléfono", e);
        } catch (ExecutionException | TimeoutException e) {
            LOG.error("no se pudo publicar el teléfono del proceso {}", process.getReservationProcessId(), e);
            throw new PhoneSubmissionFailedException("no se pudo publicar el teléfono en Kafka", e);
        }
    }
}
