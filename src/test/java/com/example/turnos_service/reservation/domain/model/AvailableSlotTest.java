package com.example.turnos_service.reservation.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class AvailableSlotTest {

    private static final AvailableSlot TURNO =
            new AvailableSlot(LocalDate.of(2026, 11, 20), LocalTime.of(10, 0), LocalTime.of(10, 30));

    @Test
    void debeEmpezarDespues_cuandoElMomentoEsAnterior() {
        assertThat(TURNO.startsAfter(LocalDateTime.of(2026, 11, 20, 9, 59))).isTrue();
    }

    @Test
    void noDebeEmpezarDespues_cuandoElMomentoEsJustoElInicio() {
        assertThat(TURNO.startsAfter(LocalDateTime.of(2026, 11, 20, 10, 0))).isFalse();
    }

    @Test
    void noDebeEmpezarDespues_cuandoElTurnoYaEmpezo() {
        assertThat(TURNO.startsAfter(LocalDateTime.of(2026, 11, 20, 10, 1))).isFalse();
    }

    @Test
    void debeConsiderarLaFecha_cuandoLaHoraEsLaMismaPeroEsOtroDia() {
        assertThat(TURNO.startsAfter(LocalDateTime.of(2026, 11, 21, 9, 0))).isFalse();
    }
}
