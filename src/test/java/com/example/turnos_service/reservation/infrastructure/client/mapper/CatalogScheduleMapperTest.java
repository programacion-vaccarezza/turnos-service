package com.example.turnos_service.reservation.infrastructure.client.mapper;

import com.example.turnos_service.reservation.domain.model.WeeklySchedule;
import com.example.turnos_service.reservation.infrastructure.client.dto.CatalogScheduleResponse;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogScheduleMapperTest {

    private final CatalogScheduleMapper mapper = new CatalogScheduleMapper();

    @Test
    void debeConvertirLosDatosDelHorario_cuandoCatalogoLoDevuelve() {
        CatalogScheduleResponse response = new CatalogScheduleResponse(
                5L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 30, true);

        WeeklySchedule horario = mapper.toDomain(response);

        assertThat(horario)
                .extracting(WeeklySchedule::getDayOfWeek, WeeklySchedule::getStartTime,
                        WeeklySchedule::getEndTime, WeeklySchedule::getSlotDurationMinutes)
                .containsExactly(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 30);
    }

    @Test
    void debeDevolverNull_cuandoLaRespuestaEsNula() {
        assertThat(mapper.toDomain(null)).isNull();
    }
}
