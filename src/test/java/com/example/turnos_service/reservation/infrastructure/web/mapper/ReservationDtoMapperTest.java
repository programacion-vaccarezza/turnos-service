package com.example.turnos_service.reservation.infrastructure.web.mapper;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.infrastructure.web.dto.ReservationResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationDtoMapperTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-10-24T19:22:19Z");

    private final ReservationDtoMapper mapper = new ReservationDtoMapper();

    @Test
    void debeConvertirLosDatosDelProceso_cuandoArmaLaRespuesta() {
        ReservationProcess process = ReservationProcess.builder()
                .id(2L)
                .ownerLogin("juan.lopez")
                .professionalId(201L)
                .appointmentDate(LocalDate.of(2026, 10, 28))
                .startTime(LocalTime.of(17, 30))
                .status(ReservationStatus.WAITING_FOR_PHONE)
                .holdId("hold-1")
                .reservationProcessId("process-1")
                .requestEventId("request-event-1")
                .expiresAt(EXPIRES_AT)
                .build();

        ReservationResponse response = mapper.toResponse(process);

        assertThat(response)
                .extracting("id", "professionalId", "date", "startTime", "status", "expiresAt", "failureReason")
                .containsExactly(2L, 201L, LocalDate.of(2026, 10, 28), LocalTime.of(17, 30),
                        ReservationStatus.WAITING_FOR_PHONE, EXPIRES_AT, null);
    }

    @Test
    void noDebeExponerLosIdentificadoresInternosDeLaCatedra() {
        assertThat(Arrays.stream(ReservationResponse.class.getDeclaredFields()).map(Field::getName))
                .doesNotContain("holdId", "reservationProcessId", "requestEventId", "ownerLogin");
    }

    @Test
    void debeDevolverNull_cuandoElProcesoEsNulo() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
