package com.example.turnos_service.reservation.infrastructure.client.adapter;

import com.example.turnos_service.reservation.application.exception.CatedraRejectedException;
import com.example.turnos_service.reservation.application.exception.CatedraUnavailableException;
import com.example.turnos_service.reservation.domain.model.Occupancy;
import com.example.turnos_service.reservation.infrastructure.client.dto.CatedraOccupanciesResponse;
import com.example.turnos_service.reservation.infrastructure.client.mapper.CatedraOccupancyMapper;
import com.example.turnos_service.shared.infrastructure.client.catedra.CatedraErrorHandler;
import com.example.turnos_service.shared.infrastructure.client.catedra.exception.CatedraApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatedraOccupancyClientAdapterTest {

    private static final Long PROFESIONAL = 202L;
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 20);

    @Mock
    WebClient webClient;

    @Mock
    WebClient.RequestHeadersUriSpec<?> uriSpec;

    @Mock
    WebClient.RequestHeadersSpec<?> headersSpec;

    @Mock
    WebClient.ResponseSpec responseSpec;

    @Captor
    ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor;

    private CatedraOccupancyClientAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CatedraOccupancyClientAdapter(webClient, new CatedraErrorHandler(), new CatedraOccupancyMapper());

        doReturn(uriSpec).when(webClient).get();
        doReturn(headersSpec).when(uriSpec).uri(ArgumentMatchers.<Function<UriBuilder, URI>>any());
        doReturn(responseSpec).when(headersSpec).retrieve();
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    }

    @Test
    void debeDevolverLosTurnosOcupados_cuandoLaCatedraResponde() {
        when(responseSpec.bodyToMono(CatedraOccupanciesResponse.class)).thenReturn(Mono.just(ocupaciones(
                new CatedraOccupanciesResponse.Item(FECHA, LocalTime.of(9, 0), "HELD", null),
                new CatedraOccupanciesResponse.Item(FECHA, LocalTime.of(10, 0), "CONFIRMED", null))));

        List<Occupancy> resultado = adapter.findByProfessionalAndDate(PROFESIONAL, FECHA);

        assertThat(resultado).extracting(Occupancy::getStartTime)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(10, 0));
    }

    @Test
    void debePedirLasOcupacionesDelProfesionalSoloEnEsaFecha_cuandoConsulta() {
        when(responseSpec.bodyToMono(CatedraOccupanciesResponse.class)).thenReturn(Mono.just(ocupaciones()));

        adapter.findByProfessionalAndDate(PROFESIONAL, FECHA);

        verify(uriSpec).uri(uriCaptor.capture());
        assertThat(uriCaptor.getValue().apply(new DefaultUriBuilderFactory().builder()).toString())
                .isEqualTo("/api/appointment-occupancies?professionalId=202&from=2026-11-20&to=2026-11-20");
    }

    @Test
    void debeDevolverListaVacia_cuandoLaCatedraNoDevuelveCuerpo() {
        when(responseSpec.bodyToMono(CatedraOccupanciesResponse.class)).thenReturn(Mono.empty());

        assertThat(adapter.findByProfessionalAndDate(PROFESIONAL, FECHA)).isEmpty();
    }

    @Test
    void debeLanzarRechazoConCode_cuandoLaCatedraRechazaLaConsulta() {
        when(responseSpec.bodyToMono(CatedraOccupanciesResponse.class))
                .thenReturn(Mono.error(new CatedraApiException(400, "INVALID_DATE_RANGE")));

        assertThatThrownBy(() -> adapter.findByProfessionalAndDate(PROFESIONAL, FECHA))
                .isInstanceOf(CatedraRejectedException.class)
                .extracting("status", "code")
                .containsExactly(400, "INVALID_DATE_RANGE");
    }

    @Test
    void debeLanzarNoDisponible_cuandoLaCatedraRespondeErrorDelServidor() {
        when(responseSpec.bodyToMono(CatedraOccupanciesResponse.class))
                .thenReturn(Mono.error(new CatedraApiException(503, null)));

        assertThatThrownBy(() -> adapter.findByProfessionalAndDate(PROFESIONAL, FECHA))
                .isInstanceOf(CatedraUnavailableException.class)
                .hasCauseInstanceOf(CatedraApiException.class);
    }

    @Test
    void debeLanzarNoDisponible_cuandoLaCatedraNoResponde() {
        when(responseSpec.bodyToMono(CatedraOccupanciesResponse.class)).thenReturn(Mono.error(
                new WebClientRequestException(new TimeoutException("sin respuesta"), HttpMethod.GET,
                        URI.create("http://catedra"), HttpHeaders.EMPTY)));

        assertThatThrownBy(() -> adapter.findByProfessionalAndDate(PROFESIONAL, FECHA))
                .isInstanceOf(CatedraUnavailableException.class)
                .hasCauseInstanceOf(WebClientRequestException.class);
    }

    @Test
    void debeLanzarNoDisponible_cuandoLaCatedraDevuelveUnaRespuestaInvalida() {
        when(responseSpec.bodyToMono(CatedraOccupanciesResponse.class))
                .thenReturn(Mono.error(new DecodingException("respuesta inválida")));

        assertThatThrownBy(() -> adapter.findByProfessionalAndDate(PROFESIONAL, FECHA))
                .isInstanceOf(CatedraUnavailableException.class)
                .hasCauseInstanceOf(DecodingException.class);
    }

    private static CatedraOccupanciesResponse ocupaciones(CatedraOccupanciesResponse.Item... items) {
        return new CatedraOccupanciesResponse(PROFESIONAL, FECHA, FECHA, List.of(items));
    }
}
