package com.example.turnos_service.reservation.infrastructure.client.mapper;

import com.example.turnos_service.reservation.domain.model.Occupancy;
import com.example.turnos_service.reservation.infrastructure.client.dto.CatedraOccupanciesResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class CatedraOccupancyMapperTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 20);
    private static final Instant VENCE = Instant.parse("2026-11-19T15:00:00Z");

    private final CatedraOccupancyMapper mapper = new CatedraOccupancyMapper();

    @Test
    void debeConvertirCadaTurnoOcupado_cuandoLaCatedraLosDevuelve() {
        CatedraOccupanciesResponse response = new CatedraOccupanciesResponse(202L, FECHA, FECHA, List.of(
                new CatedraOccupanciesResponse.Item(FECHA, LocalTime.of(9, 0), "HELD", VENCE),
                new CatedraOccupanciesResponse.Item(FECHA, LocalTime.of(9, 30), "CONFIRMED", null)));

        List<Occupancy> ocupaciones = mapper.toDomain(response);

        assertThat(ocupaciones)
                .extracting(Occupancy::getDate, Occupancy::getStartTime, Occupancy::getStatus, Occupancy::getExpiresAt)
                .containsExactly(
                        tuple(FECHA, LocalTime.of(9, 0), "HELD", VENCE),
                        tuple(FECHA, LocalTime.of(9, 30), "CONFIRMED", null));
    }

    @Test
    void debeDevolverListaVacia_cuandoLaRespuestaEsNula() {
        assertThat(mapper.toDomain(null)).isEmpty();
    }

    @Test
    void debeDevolverListaVacia_cuandoLaRespuestaNoTraeOcupaciones() {
        assertThat(mapper.toDomain(new CatedraOccupanciesResponse(202L, FECHA, FECHA, null))).isEmpty();
    }
}
