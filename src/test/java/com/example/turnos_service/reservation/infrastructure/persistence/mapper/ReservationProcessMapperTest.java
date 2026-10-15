package com.example.turnos_service.reservation.infrastructure.persistence.mapper;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationProcessMapperTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-09-10T14:44:50Z");
    private static final Instant CREATED_AT = Instant.parse("2026-09-10T14:30:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-09-10T14:35:10Z");

    private final ReservationProcessMapper mapper = new ReservationProcessMapper();

    @Test
    void debeCopiarTodosLosCampos_cuandoConvierteDominioAEntidad() {
        ReservationProcess dominio = ReservationProcess.builder()
                .id(1L)
                .ownerLogin("juan.perez")
                .professionalId(15L)
                .appointmentDate(LocalDate.of(2026, 9, 14))
                .startTime(LocalTime.of(9, 30))
                .patientFirstName("Juan")
                .patientLastName("Perez")
                .status(ReservationStatus.WAITING_FOR_PHONE)
                .holdId("hold-6c0c2f41")
                .reservationProcessId("process-a92b47d0")
                .expiresAt(EXPIRES_AT)
                .requestEventId("request-event-8a31")
                .reservationId(8251L)
                .failureReason("SLOT_ALREADY_HELD")
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .version(3L)
                .build();

        ReservationProcessEntity entidad = mapper.toEntity(dominio);

        assertThat(entidad)
                .extracting("id", "ownerLogin", "professionalId", "appointmentDate", "startTime",
                        "patientFirstName", "patientLastName", "status", "holdId", "reservationProcessId",
                        "expiresAt", "requestEventId", "reservationId", "failureReason",
                        "createdAt", "updatedAt", "version")
                .containsExactly(1L, "juan.perez", 15L, LocalDate.of(2026, 9, 14), LocalTime.of(9, 30),
                        "Juan", "Perez", ReservationStatus.WAITING_FOR_PHONE, "hold-6c0c2f41", "process-a92b47d0",
                        EXPIRES_AT, "request-event-8a31", 8251L, "SLOT_ALREADY_HELD",
                        CREATED_AT, UPDATED_AT, 3L);
    }

    @Test
    void debeCopiarTodosLosCampos_cuandoConvierteEntidadADominio() {
        ReservationProcessEntity entidad = ReservationProcessEntity.builder()
                .id(1L)
                .ownerLogin("juan.perez")
                .professionalId(15L)
                .appointmentDate(LocalDate.of(2026, 9, 14))
                .startTime(LocalTime.of(9, 30))
                .patientFirstName("Juan")
                .patientLastName("Perez")
                .status(ReservationStatus.CONFIRMED)
                .holdId("hold-6c0c2f41")
                .reservationProcessId("process-a92b47d0")
                .expiresAt(EXPIRES_AT)
                .requestEventId("request-event-8a31")
                .reservationId(8251L)
                .failureReason(null)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .version(3L)
                .build();

        ReservationProcess dominio = mapper.toDomainModel(entidad);

        assertThat(dominio)
                .extracting("id", "ownerLogin", "professionalId", "appointmentDate", "startTime",
                        "patientFirstName", "patientLastName", "status", "holdId", "reservationProcessId",
                        "expiresAt", "requestEventId", "reservationId", "failureReason",
                        "createdAt", "updatedAt", "version")
                .containsExactly(1L, "juan.perez", 15L, LocalDate.of(2026, 9, 14), LocalTime.of(9, 30),
                        "Juan", "Perez", ReservationStatus.CONFIRMED, "hold-6c0c2f41", "process-a92b47d0",
                        EXPIRES_AT, "request-event-8a31", 8251L, null,
                        CREATED_AT, UPDATED_AT, 3L);
    }

    @Test
    void debeDevolverNull_cuandoElDominioEsNulo() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void debeDevolverNull_cuandoLaEntidadEsNula() {
        assertThat(mapper.toDomainModel(null)).isNull();
    }
}
