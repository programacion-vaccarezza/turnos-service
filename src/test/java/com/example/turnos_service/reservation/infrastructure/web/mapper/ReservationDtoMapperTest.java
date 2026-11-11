package com.example.turnos_service.reservation.infrastructure.web.mapper;

import com.example.turnos_service.reservation.domain.model.AvailableSlot;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.model.UserReservation;
import com.example.turnos_service.reservation.infrastructure.web.dto.AvailableSlotResponse;
import com.example.turnos_service.reservation.infrastructure.web.dto.ReservationResponse;
import com.example.turnos_service.reservation.infrastructure.web.dto.UserReservationResponse;
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
        assertThat(mapper.toResponse((ReservationProcess) null)).isNull();
    }

    @Test
    void debeConvertirTodosLosDatos_cuandoArmaLaRespuestaDeUnaReservaDelUsuario() {
        UserReservation reservation = UserReservation.builder()
                .id(2L).reservationId(1502L).professionalId(202L)
                .professionalFirstName("Martin").professionalLastName("Rivas")
                .date(LocalDate.of(2026, 11, 20)).startTime(LocalTime.of(18, 0)).endTime(LocalTime.of(18, 30))
                .status("CANCELLED").expiresAt(EXPIRES_AT).failureReason("PROCESS_EXPIRED")
                .cancellationReason("Cambio de horario").source(UserReservation.SOURCE_CATEDRA).build();

        UserReservationResponse response = mapper.toResponse(reservation);

        assertThat(response)
                .extracting("id", "reservationId", "professionalId", "professionalFirstName", "professionalLastName",
                        "date", "startTime", "endTime", "status", "expiresAt", "failureReason", "cancellationReason", "source")
                .containsExactly(2L, 1502L, 202L, "Martin", "Rivas",
                        LocalDate.of(2026, 11, 20), LocalTime.of(18, 0), LocalTime.of(18, 30),
                        "CANCELLED", EXPIRES_AT, "PROCESS_EXPIRED", "Cambio de horario", UserReservation.SOURCE_CATEDRA);
    }

    @Test
    void noDebeExponerElDuenoEnLaRespuestaDeUnaReservaDelUsuario() {
        assertThat(Arrays.stream(UserReservationResponse.class.getDeclaredFields()).map(Field::getName))
                .doesNotContain("holdId", "reservationProcessId", "requestEventId", "ownerLogin");
    }

    @Test
    void debeDevolverNull_cuandoLaReservaDelUsuarioEsNula() {
        assertThat(mapper.toResponse((UserReservation) null)).isNull();
    }

    @Test
    void debeConvertirLaFechaYLosHorarios_cuandoArmaLaRespuestaDeUnTurnoLibre() {
        AvailableSlotResponse response = mapper.toResponse(
                new AvailableSlot(LocalDate.of(2026, 11, 20), LocalTime.of(9, 0), LocalTime.of(9, 30)));

        assertThat(response)
                .extracting("date", "startTime", "endTime")
                .containsExactly(LocalDate.of(2026, 11, 20), LocalTime.of(9, 0), LocalTime.of(9, 30));
    }

    @Test
    void debeDevolverNull_cuandoElTurnoLibreEsNulo() {
        assertThat(mapper.toResponse((AvailableSlot) null)).isNull();
    }
}
