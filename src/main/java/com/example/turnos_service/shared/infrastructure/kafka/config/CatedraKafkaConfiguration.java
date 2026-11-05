package com.example.turnos_service.shared.infrastructure.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Topics de Kafka asignados a la cuenta técnica (anexo §15.2).
 * Los topics los crea y administra la cátedra: turnos-service nunca los declara ni los crea.
 */
@ConfigurationProperties(prefix = "catedra.kafka")
public record CatedraKafkaConfiguration(String appointmentActionsTopic, String appointmentPhoneTopic) {
}
