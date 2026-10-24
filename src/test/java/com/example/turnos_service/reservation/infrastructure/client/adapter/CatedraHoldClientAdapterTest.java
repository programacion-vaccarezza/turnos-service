package com.example.turnos_service.reservation.infrastructure.client.adapter;

import com.example.turnos_service.reservation.application.exception.CatedraRejectedException;
import com.example.turnos_service.reservation.application.exception.CatedraUnavailableException;
import com.example.turnos_service.reservation.domain.model.Hold;
import com.example.turnos_service.reservation.domain.model.HoldConfirmation;
import com.example.turnos_service.reservation.infrastructure.client.dto.ConfirmHoldRequest;
import com.example.turnos_service.reservation.infrastructure.client.dto.ConfirmHoldResponse;
import com.example.turnos_service.reservation.infrastructure.client.dto.CreateHoldRequest;
import com.example.turnos_service.reservation.infrastructure.client.dto.CreateHoldResponse;
import com.example.turnos_service.reservation.infrastructure.client.mapper.CatedraHoldMapper;
import com.example.turnos_service.shared.infrastructure.client.catedra.CatedraErrorHandler;
import com.example.turnos_service.shared.infrastructure.client.catedra.exception.CatedraApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatedraHoldClientAdapterTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 10, 28);
    private static final LocalTime HORA = LocalTime.of(17, 30);
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-24T19:22:19Z");

    @Mock
    WebClient webClient;

    @Mock
    WebClient.RequestBodyUriSpec uriSpec;

    @Mock
    WebClient.RequestBodySpec bodySpec;

    @Mock
    WebClient.RequestHeadersSpec<?> headersSpec;

    @Mock
    WebClient.ResponseSpec responseSpec;

    private CatedraHoldClientAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CatedraHoldClientAdapter(webClient, new CatedraErrorHandler(), new CatedraHoldMapper());

        when(webClient.post()).thenReturn(uriSpec);
        lenient().when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        lenient().when(uriSpec.uri(anyString(), any(Object.class))).thenReturn(bodySpec);
        doReturn(headersSpec).when(bodySpec).bodyValue(any());
        doReturn(responseSpec).when(headersSpec).retrieve();
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    }

    // ---------- crear hold ----------

    @Test
    void debeDevolverElHold_cuandoLaCatedraLoCrea() {
        when(responseSpec.bodyToMono(CreateHoldResponse.class))
                .thenReturn(Mono.just(new CreateHoldResponse("hold-1", "process-1", "HELD", EXPIRES_AT)));

        Hold hold = adapter.createHold(201L, FECHA, HORA, "juan.lopez");

        assertThat(hold)
                .extracting("holdId", "reservationProcessId", "expiresAt")
                .containsExactly("hold-1", "process-1", EXPIRES_AT);
    }

    @Test
    void debeEnviarElBodyDelAnexo_cuandoPideElHold() {
        when(responseSpec.bodyToMono(CreateHoldResponse.class))
                .thenReturn(Mono.just(new CreateHoldResponse("hold-1", "process-1", "HELD", EXPIRES_AT)));

        adapter.createHold(201L, FECHA, HORA, "juan.lopez");

        verify(uriSpec).uri("/api/appointment-holds");
        verify(bodySpec).bodyValue(new CreateHoldRequest(201L, "2026-10-28", "17:30", "juan.lopez"));
    }

    @Test
    void debeLanzarRechazoConCode_cuandoLaCatedraRechazaElHold() {
        when(responseSpec.bodyToMono(CreateHoldResponse.class))
                .thenReturn(Mono.error(new CatedraApiException(409, "SLOT_ALREADY_HELD")));

        assertThatThrownBy(() -> adapter.createHold(201L, FECHA, HORA, "juan.lopez"))
                .isInstanceOf(CatedraRejectedException.class)
                .extracting("status", "code")
                .containsExactly(409, "SLOT_ALREADY_HELD");
    }

    @Test
    void debeLanzarNoDisponible_cuandoLaCatedraRespondeErrorDelServidor() {
        when(responseSpec.bodyToMono(CreateHoldResponse.class))
                .thenReturn(Mono.error(new CatedraApiException(503, null)));

        assertThatThrownBy(() -> adapter.createHold(201L, FECHA, HORA, "juan.lopez"))
                .isInstanceOf(CatedraUnavailableException.class);
    }

    @Test
    void debeLanzarNoDisponible_cuandoLaCatedraNoRespondeAlCrearElHold() {
        when(responseSpec.bodyToMono(CreateHoldResponse.class)).thenReturn(Mono.error(sinRespuesta()));

        assertThatThrownBy(() -> adapter.createHold(201L, FECHA, HORA, "juan.lopez"))
                .isInstanceOf(CatedraUnavailableException.class)
                .hasCauseInstanceOf(WebClientRequestException.class);
    }

    @Test
    void debeLanzarNoDisponible_cuandoLaCatedraNoDevuelveLosDatosDelHold() {
        when(responseSpec.bodyToMono(CreateHoldResponse.class)).thenReturn(Mono.empty());

        assertThatThrownBy(() -> adapter.createHold(201L, FECHA, HORA, "juan.lopez"))
                .isInstanceOf(CatedraUnavailableException.class);
    }

    // ---------- confirmar hold ----------

    @Test
    void debeDevolverLaConfirmacion_cuandoLaCatedraAceptaElConfirm() {
        when(responseSpec.bodyToMono(ConfirmHoldResponse.class))
                .thenReturn(Mono.just(new ConfirmHoldResponse("process-1", "WAITING_FOR_PHONE", EXPIRES_AT)));

        HoldConfirmation confirmation = adapter.confirmHold("hold-1", "juan.lopez", "Juan", "Lopez");

        assertThat(confirmation)
                .extracting("reservationProcessId", "expiresAt")
                .containsExactly("process-1", EXPIRES_AT);
    }

    @Test
    void debeEnviarElHoldIdYLosDatosDelPaciente_cuandoConfirma() {
        when(responseSpec.bodyToMono(ConfirmHoldResponse.class))
                .thenReturn(Mono.just(new ConfirmHoldResponse("process-1", "WAITING_FOR_PHONE", EXPIRES_AT)));

        adapter.confirmHold("hold-1", "juan.lopez", "Juan", "Lopez");

        verify(uriSpec).uri("/api/appointment-holds/{holdId}/confirm", "hold-1");
        verify(bodySpec).bodyValue(new ConfirmHoldRequest("juan.lopez", "Juan", "Lopez"));
    }

    @Test
    void debeLanzarRechazoConCode_cuandoElHoldVencio() {
        when(responseSpec.bodyToMono(ConfirmHoldResponse.class))
                .thenReturn(Mono.error(new CatedraApiException(404, "HOLD_EXPIRED")));

        assertThatThrownBy(() -> adapter.confirmHold("hold-1", "juan.lopez", "Juan", "Lopez"))
                .isInstanceOf(CatedraRejectedException.class)
                .extracting("code")
                .isEqualTo("HOLD_EXPIRED");
    }

    @Test
    void debeLanzarNoDisponible_cuandoElConfirmNoRecibeRespuesta() {
        when(responseSpec.bodyToMono(ConfirmHoldResponse.class)).thenReturn(Mono.error(sinRespuesta()));

        assertThatThrownBy(() -> adapter.confirmHold("hold-1", "juan.lopez", "Juan", "Lopez"))
                .isInstanceOf(CatedraUnavailableException.class);
    }

    @Test
    void debeLanzarNoDisponible_cuandoLaCatedraNoDevuelveLosDatosDeLaConfirmacion() {
        when(responseSpec.bodyToMono(ConfirmHoldResponse.class)).thenReturn(Mono.empty());

        assertThatThrownBy(() -> adapter.confirmHold("hold-1", "juan.lopez", "Juan", "Lopez"))
                .isInstanceOf(CatedraUnavailableException.class);
    }

    private WebClientRequestException sinRespuesta() {
        return new WebClientRequestException(new TimeoutException("sin respuesta"), HttpMethod.POST,
                URI.create("http://catedra"), HttpHeaders.EMPTY);
    }
}
