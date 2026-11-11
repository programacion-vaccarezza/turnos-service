package com.example.turnos_service.reservation.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyScheduleTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 16);

    @Test
    void debeDividirElHorarioEnTurnos_cuandoLaFechaCoincideConElDia() {
        List<AvailableSlot> turnos = horario(LocalTime.of(9, 0), LocalTime.of(12, 0), 30).slotsOn(FECHA);

        assertThat(turnos).extracting(AvailableSlot::getStartTime).containsExactly(
                LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0),
                LocalTime.of(10, 30), LocalTime.of(11, 0), LocalTime.of(11, 30));
        assertThat(turnos.getLast())
                .extracting(AvailableSlot::getDate, AvailableSlot::getEndTime)
                .containsExactly(FECHA, LocalTime.of(12, 0));
    }

    @Test
    void debeDevolverListaVacia_cuandoLaFechaEsDeOtroDia() {
        assertThat(horario(LocalTime.of(9, 0), LocalTime.of(12, 0), 30).slotsOn(FECHA.plusDays(1))).isEmpty();
    }

    @Test
    void debeDescartarElUltimoTramo_cuandoNoAlcanzaParaUnTurnoCompleto() {
        List<AvailableSlot> turnos = horario(LocalTime.of(9, 0), LocalTime.of(10, 45), 30).slotsOn(FECHA);

        assertThat(turnos).extracting(AvailableSlot::getStartTime)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0));
        assertThat(turnos.getLast().getEndTime()).isEqualTo(LocalTime.of(10, 30));
    }

    @Test
    void debeDevolverListaVacia_cuandoElTurnoEsMasLargoQueElHorario() {
        assertThat(horario(LocalTime.of(9, 0), LocalTime.of(9, 20), 30).slotsOn(FECHA)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -15})
    void debeDevolverListaVacia_cuandoLaDuracionNoEsPositiva(int duracion) {
        assertThat(horario(LocalTime.of(9, 0), LocalTime.of(12, 0), duracion).slotsOn(FECHA)).isEmpty();
    }

    @Test
    void noDebePasarDeMedianoche_cuandoElHorarioTerminaAlFinalDelDia() {
        List<AvailableSlot> turnos = horario(LocalTime.of(23, 0), LocalTime.of(23, 59), 30).slotsOn(FECHA);

        assertThat(turnos).extracting(AvailableSlot::getStartTime).containsExactly(LocalTime.of(23, 0));
    }

    private static WeeklySchedule horario(LocalTime inicio, LocalTime fin, int duracion) {
        return WeeklySchedule.builder()
                .dayOfWeek(FECHA.getDayOfWeek()).startTime(inicio).endTime(fin).slotDurationMinutes(duracion).build();
    }
}
