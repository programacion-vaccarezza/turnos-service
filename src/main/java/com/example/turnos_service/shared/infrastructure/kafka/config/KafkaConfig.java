package com.example.turnos_service.shared.infrastructure.kafka.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CatedraKafkaConfiguration.class)
public class KafkaConfig {
}
