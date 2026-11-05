package com.example.turnos_service.reservation.infrastructure.web.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CancelReservationRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void debeAceptarElMotivo_cuandoTieneQuinientosCaracteres() {
        assertThat(validator.validate(new CancelReservationRequest("a".repeat(500)))).isEmpty();
    }

    @Test
    void debeRechazarElMotivo_cuandoTieneMasDeQuinientosCaracteres() {
        assertThat(validator.validate(new CancelReservationRequest("a".repeat(501)))).hasSize(1);
    }

    @Test
    void debeAceptarElRequest_cuandoNoTieneMotivo() {
        assertThat(validator.validate(new CancelReservationRequest(null))).isEmpty();
    }
}
