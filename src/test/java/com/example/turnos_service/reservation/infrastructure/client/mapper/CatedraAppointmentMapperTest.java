package com.example.turnos_service.reservation.infrastructure.client.mapper;

import com.example.turnos_service.reservation.domain.model.CatedraAppointment;
import com.example.turnos_service.reservation.infrastructure.client.dto.CatedraAppointmentResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class CatedraAppointmentMapperTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 20);
    private static final LocalTime INICIO = LocalTime.of(18, 0);
    private static final LocalTime FIN = LocalTime.of(18, 30);
    private static final Instant CREADA = Instant.parse("2026-11-04T15:16:40Z");
    private static final Instant CONFIRMADA = Instant.parse("2026-11-04T15:17:00Z");
    private static final Instant CANCELADA = Instant.parse("2026-11-04T15:20:00Z");

    private final CatedraAppointmentMapper mapper = new CatedraAppointmentMapper();

    @Test
    void debeConvertirTodosLosDatos_cuandoLaCatedraDevuelveUnaReserva() {
        CatedraAppointmentResponse response = new CatedraAppointmentResponse(1502L, "process-1", "juan.lopez", FECHA,
                INICIO, FIN, 202L, "Martin", "Rivas", "CANCELLED", CREADA, CONFIRMADA, CANCELADA, "Cambio de horario");

        CatedraAppointment appointment = mapper.toDomain(response);

        assertThat(appointment)
                .extracting("reservationId", "reservationProcessId", "externalPatientId", "appointmentDate",
                        "startTime", "endTime", "professionalId", "professionalFirstName", "professionalLastName",
                        "status", "createdAt", "confirmedAt", "cancelledAt", "cancellationReason")
                .containsExactly(1502L, "process-1", "juan.lopez", FECHA, INICIO, FIN, 202L, "Martin", "Rivas",
                        "CANCELLED", CREADA, CONFIRMADA, CANCELADA, "Cambio de horario");
    }

    @Test
    void debeDevolverNull_cuandoLaRespuestaEsNula() {
        assertThat(mapper.toDomain(null)).isNull();
    }
}
