package com.example.turnos_service.reservation.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.example.turnos_service.reservation.domain.model.ReservationStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class ReservationStatusTest {


    private static final Map<ReservationStatus, Set<ReservationStatus>> TRANSICIONES_VALIDAS = new EnumMap<>(ReservationStatus.class);

    static {
        TRANSICIONES_VALIDAS.put(INITIATED, EnumSet.of(HOLD_CREATED, FAILED));
        TRANSICIONES_VALIDAS.put(HOLD_CREATED, EnumSet.of(CONFIRMING, EXPIRED, INVALID));
        TRANSICIONES_VALIDAS.put(CONFIRMING, EnumSet.of(WAITING_FOR_PHONE, EXPIRED, INVALID, FAILED));
        TRANSICIONES_VALIDAS.put(WAITING_FOR_PHONE, EnumSet.of(PHONE_SUBMITTED, PHONE_REJECTED, CONFIRMED, EXPIRED, INVALID));
        TRANSICIONES_VALIDAS.put(PHONE_SUBMITTED, EnumSet.of(PHONE_REJECTED, CONFIRMED, EXPIRED, INVALID));
        TRANSICIONES_VALIDAS.put(PHONE_REJECTED, EnumSet.of(PHONE_SUBMITTED, CONFIRMED, EXPIRED, INVALID));
        TRANSICIONES_VALIDAS.put(CONFIRMED, EnumSet.of(CANCELLED));
        TRANSICIONES_VALIDAS.put(CANCELLED, EnumSet.noneOf(ReservationStatus.class));
        TRANSICIONES_VALIDAS.put(EXPIRED, EnumSet.noneOf(ReservationStatus.class));
        TRANSICIONES_VALIDAS.put(INVALID, EnumSet.noneOf(ReservationStatus.class));
        TRANSICIONES_VALIDAS.put(FAILED, EnumSet.noneOf(ReservationStatus.class));
    }

    static Stream<Arguments> transicionesValidas() {
        return Arrays.stream(values())
                .flatMap(actual -> TRANSICIONES_VALIDAS.get(actual).stream()
                        .map(siguiente -> Arguments.of(actual, siguiente)));
    }

    static Stream<Arguments> transicionesInvalidas() {
        return Arrays.stream(values())
                .flatMap(actual -> Arrays.stream(values())
                        .filter(siguiente -> !TRANSICIONES_VALIDAS.get(actual).contains(siguiente))
                        .map(siguiente -> Arguments.of(actual, siguiente)));
    }

    @ParameterizedTest(name = "{0} -> {1} es válida")
    @MethodSource("transicionesValidas")
    void debePermitirLaTransicion_cuandoEstaEnLaTablaDeTransiciones(ReservationStatus actual, ReservationStatus siguiente) {
        assertThat(actual.canTransitionTo(siguiente)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} es inválida")
    @MethodSource("transicionesInvalidas")
    void debeRechazarLaTransicion_cuandoNoEstaEnLaTablaDeTransiciones(ReservationStatus actual, ReservationStatus siguiente) {
        assertThat(actual.canTransitionTo(siguiente)).isFalse();
    }

    @ParameterizedTest(name = "{0} es final")
    @EnumSource(value = ReservationStatus.class, names = {"CANCELLED", "EXPIRED", "INVALID", "FAILED"})
    void debeSerFinal_cuandoElProcesoTermino(ReservationStatus estado) {
        assertThat(estado.isFinal()).isTrue();
    }

    @ParameterizedTest(name = "{0} no es final")
    @EnumSource(value = ReservationStatus.class, names = {"CANCELLED", "EXPIRED", "INVALID", "FAILED"},
            mode = EnumSource.Mode.EXCLUDE)
    void noDebeSerFinal_cuandoElProcesoPuedeAvanzar(ReservationStatus estado) {
        assertThat(estado.isFinal()).isFalse();
    }

    @ParameterizedTest(name = "{0} no tiene salidas")
    @EnumSource(value = ReservationStatus.class, names = {"CANCELLED", "EXPIRED", "INVALID", "FAILED"})
    void noDebePermitirNingunaTransicion_cuandoElEstadoEsFinal(ReservationStatus estadoFinal) {
        assertThat(values())
                .noneMatch(estadoFinal::canTransitionTo);
    }

    @Test
    void noDebeSerFinal_cuandoLaReservaEstaConfirmada() {
        assertThat(CONFIRMED.isFinal()).isFalse();
        assertThat(CONFIRMED.canTransitionTo(CANCELLED)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> null es inválida")
    @EnumSource(ReservationStatus.class)
    void debeRechazarLaTransicion_cuandoElSiguienteEstadoEsNulo(ReservationStatus estado) {
        assertThat(estado.canTransitionTo(null)).isFalse();
    }
}
