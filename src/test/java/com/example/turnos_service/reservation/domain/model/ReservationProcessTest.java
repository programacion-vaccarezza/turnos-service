package com.example.turnos_service.reservation.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationProcessTest {

    private static final Instant VENCE = Instant.parse("2026-11-20T18:00:00Z");

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"HOLD_CREATED", "WAITING_FOR_PHONE", "PHONE_REJECTED"})
    void debeEstarVencido_cuandoPasoElVencimientoYLaCatedraEsperaAlServicio(ReservationStatus estado) {
        assertThat(proceso(estado, VENCE).isLocallyExpired(VENCE.plusSeconds(1))).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"HOLD_CREATED", "WAITING_FOR_PHONE", "PHONE_REJECTED"},
            mode = EnumSource.Mode.EXCLUDE)
    void noDebeEstarVencido_cuandoElEstadoNoSeVenceLocalmente(ReservationStatus estado) {
        assertThat(proceso(estado, VENCE).isLocallyExpired(VENCE.plusSeconds(1))).isFalse();
    }

    @Test
    void noDebeEstarVencido_cuandoTodaviaNoLlegoElVencimiento() {
        assertThat(proceso(ReservationStatus.WAITING_FOR_PHONE, VENCE).isLocallyExpired(VENCE.minusSeconds(1))).isFalse();
    }

    @Test
    void noDebeEstarVencido_cuandoEsJustoElMomentoDelVencimiento() {
        assertThat(proceso(ReservationStatus.WAITING_FOR_PHONE, VENCE).isLocallyExpired(VENCE)).isFalse();
    }

    @Test
    void noDebeEstarVencido_cuandoNoTieneVencimiento() {
        assertThat(proceso(ReservationStatus.WAITING_FOR_PHONE, null).isLocallyExpired(VENCE)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"CONFIRMING", "PHONE_SUBMITTED"})
    void debeNecesitarReconciliacion_cuandoVencioConUnResultadoDesconocido(ReservationStatus estado) {
        assertThat(proceso(estado, VENCE).needsReconciliation(VENCE.plusSeconds(1))).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"CONFIRMING", "PHONE_SUBMITTED"}, mode = EnumSource.Mode.EXCLUDE)
    void noDebeNecesitarReconciliacion_cuandoElResultadoNoDependeDeLaCatedra(ReservationStatus estado) {
        assertThat(proceso(estado, VENCE).needsReconciliation(VENCE.plusSeconds(1))).isFalse();
    }

    @Test
    void noDebeNecesitarReconciliacion_cuandoTodaviaNoVencio() {
        assertThat(proceso(ReservationStatus.PHONE_SUBMITTED, VENCE).needsReconciliation(VENCE.minusSeconds(1))).isFalse();
    }

    @Test
    void noDebeNecesitarReconciliacion_cuandoNoTieneVencimiento() {
        assertThat(proceso(ReservationStatus.CONFIRMING, null).needsReconciliation(VENCE)).isFalse();
    }

    private static ReservationProcess proceso(ReservationStatus estado, Instant expiresAt) {
        return ReservationProcess.builder().status(estado).expiresAt(expiresAt).build();
    }
}
