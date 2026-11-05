package com.example.turnos_service.reservation.infrastructure.kafka.producer;

import com.example.turnos_service.reservation.application.exception.PhoneSubmissionFailedException;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.infrastructure.kafka.dto.AdditionalInformationSubmittedEvent;
import com.example.turnos_service.shared.infrastructure.client.catedra.config.CatedraConfiguration;
import com.example.turnos_service.shared.infrastructure.kafka.config.CatedraKafkaConfiguration;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhoneSubmissionKafkaPublisherTest {

    private static final String TOPIC = "catedra.turnos.telefono.proyecto-franco-vaccarezza";
    private static final String GROUP_ID = "proyecto-franco-vaccarezza";
    private static final String PROCESO = "process-a92b47d0";

    @Mock
    KafkaTemplate<String, Object> kafkaTemplate;

    @Captor
    ArgumentCaptor<Object> eventCaptor;

    private PhoneSubmissionKafkaPublisher publisher;
    private ReservationProcess process;

    @BeforeEach
    void setUp() {
        publisher = new PhoneSubmissionKafkaPublisher(kafkaTemplate,
                new CatedraKafkaConfiguration("alumnos.turnos.acciones." + GROUP_ID, TOPIC),
                new CatedraConfiguration("http://catedra", "jwt-de-prueba", GROUP_ID));
        process = ReservationProcess.builder()
                .id(7L).reservationProcessId(PROCESO).holdId("hold-6c0c2f41").requestEventId("request-event-8a31").build();
    }

    @Test
    void debePublicarEnElTopicDeTelefonoConElProcesoComoKey_cuandoSeEnviaElTelefono() {
        when(kafkaTemplate.send(eq(TOPIC), eq(PROCESO), any())).thenReturn(enviado());

        publisher.publishPhone(process, "phone-submitted-1", "+5492615551234");

        verify(kafkaTemplate).send(eq(TOPIC), eq(PROCESO), eventCaptor.capture());
        AdditionalInformationSubmittedEvent event = (AdditionalInformationSubmittedEvent) eventCaptor.getValue();
        assertThat(event)
                .extracting(AdditionalInformationSubmittedEvent::eventId, AdditionalInformationSubmittedEvent::eventType,
                        AdditionalInformationSubmittedEvent::groupId, AdditionalInformationSubmittedEvent::reservationProcessId,
                        AdditionalInformationSubmittedEvent::holdId, AdditionalInformationSubmittedEvent::requestEventId,
                        AdditionalInformationSubmittedEvent::schemaVersion)
                .containsExactly("phone-submitted-1", "AdditionalInformationSubmitted", GROUP_ID, PROCESO,
                        "hold-6c0c2f41", "request-event-8a31", 1);
        assertThat(event.information().phoneNumber()).isEqualTo("+5492615551234");
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void debeLanzarPhoneSubmissionFailed_cuandoKafkaNoConfirmaElEnvio() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(new KafkaException("broker caído")));

        assertThatThrownBy(() -> publisher.publishPhone(process, "phone-submitted-1", "+5492615551234"))
                .isInstanceOf(PhoneSubmissionFailedException.class);
    }

    private static CompletableFuture<SendResult<String, Object>> enviado() {
        ProducerRecord<String, Object> record = new ProducerRecord<>(TOPIC, PROCESO, "evento");
        RecordMetadata metadata = new RecordMetadata(new TopicPartition(TOPIC, 0), 5L, 0, 0L, 0, 0);
        return CompletableFuture.completedFuture(new SendResult<>(record, metadata));
    }
}
