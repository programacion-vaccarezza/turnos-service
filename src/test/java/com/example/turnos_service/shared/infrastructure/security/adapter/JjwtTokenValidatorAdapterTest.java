package com.example.turnos_service.shared.infrastructure.security.adapter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JjwtTokenValidatorAdapterTest {

    private static final String SECRET_DE_PRUEBA = "YJ7eO3pwjdp/SMz58x4JzCDPadAy9pN+cyem/yldBxs=";
    private static final String OTRO_SECRET_DE_PRUEBA = "vulgnzl0d8iHf/YSu8uysigabZN8vygf76/zFIEJJP8=";

    private final JjwtTokenValidatorAdapter tokenValidator = new JjwtTokenValidatorAdapter(SECRET_DE_PRUEBA);

    @Test
    void debeSerValido_cuandoElTokenFueFirmadoConElMismoSecreto() {
        String token = generarToken(SECRET_DE_PRUEBA, "juan.perez", Duration.ofHours(1));

        boolean valido = tokenValidator.isTokenValid(token);

        assertThat(valido).isTrue();
    }

    @Test
    void debeExtraerElLogin_cuandoElTokenEsValido() {
        String token = generarToken(SECRET_DE_PRUEBA, "juan.perez", Duration.ofHours(1));

        String login = tokenValidator.getLoginFromToken(token);

        assertThat(login).isEqualTo("juan.perez");
    }

    @Test
    void debeSerInvalido_cuandoElTokenEstaCorrupto() {
        boolean valido = tokenValidator.isTokenValid("esto-no-es-un-jwt-valido");

        assertThat(valido).isFalse();
    }

    @Test
    void debeSerInvalido_cuandoElTokenFueFirmadoConOtroSecreto() {
        String tokenFirmadoConOtroSecreto = generarToken(OTRO_SECRET_DE_PRUEBA, "juan.perez", Duration.ofHours(1));

        boolean valido = tokenValidator.isTokenValid(tokenFirmadoConOtroSecreto);

        assertThat(valido).isFalse();
    }

    @Test
    void debeSerInvalido_cuandoElTokenYaVencio() {
        String tokenVencido = generarToken(SECRET_DE_PRUEBA, "juan.perez", Duration.ofHours(-1));

        boolean valido = tokenValidator.isTokenValid(tokenVencido);

        assertThat(valido).isFalse();
    }

    @Test
    void debeSerInvalido_cuandoElTokenEsNulo() {
        boolean valido = tokenValidator.isTokenValid(null);

        assertThat(valido).isFalse();
    }

    private String generarToken(String secret, String login, Duration vigencia) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(login)
                .issuedAt(Date.from(ahora.minus(Duration.ofHours(2))))
                .expiration(Date.from(ahora.plus(vigencia)))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret)))
                .compact();
    }
}