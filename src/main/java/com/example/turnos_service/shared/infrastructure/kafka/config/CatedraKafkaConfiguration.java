package com.example.turnos_service.shared.infrastructure.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Topics de Kafka de la cátedra, leídos de application.properties
@ConfigurationProperties(prefix = "catedra.kafka")
public record CatedraKafkaConfiguration(String appointmentActionsTopic, String appointmentPhoneTopic) {
}
