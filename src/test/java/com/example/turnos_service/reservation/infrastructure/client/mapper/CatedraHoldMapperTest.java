package com.example.turnos_service.reservation.infrastructure.client.mapper;

import com.example.turnos_service.reservation.domain.model.Hold;
import com.example.turnos_service.reservation.domain.model.HoldConfirmation;
import com.example.turnos_service.reservation.infrastructure.client.dto.ConfirmHoldRequest;
import com.example.turnos_service.reservation.infrastructure.client.dto.ConfirmHoldResponse;
import com.example.turnos_service.reservation.infrastructure.client.dto.CreateHoldRequest;
import com.example.turnos_service.reservation.infrastructure.client.dto.CreateHoldResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class CatedraHoldMapperTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-10-24T19:22:19Z");

    private final CatedraHoldMapper mapper = new CatedraHoldMapper();

    @Test
    void debeFormatearFechaYHoraSegunElAnexo_cuandoArmaElRequestDelHold() {
        CreateHoldRequest request = mapper.toRequest(201L, LocalDate.of(2026, 10, 28), LocalTime.of(17, 30), "juan.lopez");

        assertThat(request).isEqualTo(new CreateHoldRequest(201L, "2026-10-28", "17:30", "juan.lopez"));
    }

    @Test
    void debeUsarDosDigitos_cuandoLaHoraTieneUnSoloDigito() {
        CreateHoldRequest request = mapper.toRequest(201L, LocalDate.of(2026, 1, 5), LocalTime.of(9, 0), "juan.lopez");

        assertThat(request.date()).isEqualTo("2026-01-05");
        assertThat(request.startTime()).isEqualTo("09:00");
    }

    @Test
    void debeConvertirElHold_cuandoLaCatedraLoCrea() {
        CreateHoldResponse response = new CreateHoldResponse("hold-1", "process-1", "HELD", EXPIRES_AT);

        Hold hold = mapper.toDomain(response);

        assertThat(hold)
                .extracting("holdId", "reservationProcessId", "expiresAt")
                .containsExactly("hold-1", "process-1", EXPIRES_AT);
    }

    @Test
    void debeArmarElRequestDeConfirmacion_cuandoSeConfirmaElHold() {
        ConfirmHoldRequest request = mapper.toConfirmRequest("juan.lopez", "Juan", "Lopez");

        assertThat(request).isEqualTo(new ConfirmHoldRequest("juan.lopez", "Juan", "Lopez"));
    }

    @Test
    void debeConvertirLaConfirmacion_cuandoLaCatedraAceptaElConfirm() {
        ConfirmHoldResponse response = new ConfirmHoldResponse("process-1", "WAITING_FOR_PHONE", EXPIRES_AT);

        HoldConfirmation confirmation = mapper.toDomain(response);

        assertThat(confirmation)
                .extracting("reservationProcessId", "expiresAt")
                .containsExactly("process-1", EXPIRES_AT);
    }

    @Test
    void debeDevolverNull_cuandoLasRespuestasSonNulas() {
        assertThat(mapper.toDomain((CreateHoldResponse) null)).isNull();
        assertThat(mapper.toDomain((ConfirmHoldResponse) null)).isNull();
    }
}
