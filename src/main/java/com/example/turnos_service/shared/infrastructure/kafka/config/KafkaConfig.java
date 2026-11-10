package com.example.turnos_service.shared.infrastructure.kafka.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// Habilita la lectura de la configuración de los topics de Kafka
@Configuration
@EnableConfigurationProperties(CatedraKafkaConfiguration.class)
public class KafkaConfig {
}
