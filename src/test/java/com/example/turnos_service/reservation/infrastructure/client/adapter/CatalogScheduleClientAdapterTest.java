package com.example.turnos_service.reservation.infrastructure.client.adapter;

import com.example.turnos_service.reservation.application.exception.CatalogUnavailableException;
import com.example.turnos_service.reservation.application.exception.ProfessionalNotFoundException;
import com.example.turnos_service.reservation.domain.model.WeeklySchedule;
import com.example.turnos_service.reservation.infrastructure.client.dto.CatalogScheduleResponse;
import com.example.turnos_service.reservation.infrastructure.client.mapper.CatalogScheduleMapper;
import com.example.turnos_service.shared.infrastructure.client.catalog.CatalogTokenService;
import com.example.turnos_service.shared.infrastructure.client.catalog.exception.CatalogAuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.URI;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogScheduleClientAdapterTest {

    private static final Long PROFESIONAL = 202L;

    @Mock
    WebClient webClient;

    @Mock
    WebClient.RequestHeadersUriSpec<?> uriSpec;

    @Mock
    WebClient.RequestHeadersSpec<?> headersSpec;

    @Mock
    WebClient.ResponseSpec responseSpec;

    @Mock
    CatalogTokenService tokenService;

    @Captor
    ArgumentCaptor<Consumer<HttpHeaders>> headersCaptor;

    private CatalogScheduleClientAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CatalogScheduleClientAdapter(webClient, tokenService, new CatalogScheduleMapper());

        doReturn(uriSpec).when(webClient).get();
        doReturn(headersSpec).when(uriSpec).uri(anyString(), any(Object.class));
        doReturn(headersSpec).when(headersSpec).headers(any());
        doReturn(responseSpec).when(headersSpec).retrieve();
    }

    @Test
    void debeDevolverSoloLosHorariosHabilitados_cuandoCatalogoResponde() {
        when(responseSpec.bodyToMono(CatalogScheduleResponse[].class)).thenReturn(Mono.just(new CatalogScheduleResponse[]{
                horario(DayOfWeek.MONDAY, true), horario(DayOfWeek.TUESDAY, false), horario(DayOfWeek.FRIDAY, true)}));

        List<WeeklySchedule> resultado = adapter.findEnabledByProfessionalId(PROFESIONAL);

        assertThat(resultado).extracting(WeeklySchedule::getDayOfWeek)
                .containsExactly(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        verify(tokenService, never()).invalidate();
    }

    @Test
    void debeDevolverListaVacia_cuandoElProfesionalNoTieneHorarios() {
        when(responseSpec.bodyToMono(CatalogScheduleResponse[].class)).thenReturn(Mono.just(new CatalogScheduleResponse[0]));

        assertThat(adapter.findEnabledByProfessionalId(PROFESIONAL)).isEmpty();
    }

    @Test
    void debeEnviarElTokenDeServicioYElProfesional_cuandoConsultaLosHorarios() {
        when(responseSpec.bodyToMono(CatalogScheduleResponse[].class)).thenReturn(Mono.just(new CatalogScheduleResponse[0]));
        when(tokenService.getToken()).thenReturn("token-de-servicio");

        adapter.findEnabledByProfessionalId(PROFESIONAL);

        verify(uriSpec).uri("/api/internal/professionals/{id}/schedules", PROFESIONAL);
        verify(headersSpec).headers(headersCaptor.capture());
        HttpHeaders headers = new HttpHeaders();
        headersCaptor.getValue().accept(headers);
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token-de-servicio");
    }

    @Test
    void debeLanzarProfesionalNoEncontrado_cuandoCatalogoResponde404() {
        when(responseSpec.bodyToMono(CatalogScheduleResponse[].class)).thenReturn(Mono.error(errorDeCatalogo(404)));

        assertThatThrownBy(() -> adapter.findEnabledByProfessionalId(PROFESIONAL))
                .isInstanceOf(ProfessionalNotFoundException.class);
        verify(tokenService, never()).invalidate();
    }

    @Test
    void debeRenovarElTokenYReintentar_cuandoCatalogoRespondeForbidden() {
        when(responseSpec.bodyToMono(CatalogScheduleResponse[].class))
                .thenReturn(Mono.error(errorDeCatalogo(403)))
                .thenReturn(Mono.just(new CatalogScheduleResponse[]{horario(DayOfWeek.MONDAY, true)}));

        assertThat(adapter.findEnabledByProfessionalId(PROFESIONAL)).hasSize(1);

        verify(tokenService, times(1)).invalidate();
        verify(responseSpec, times(2)).bodyToMono(CatalogScheduleResponse[].class);
    }

    @Test
    void debeRenovarElTokenYReintentar_cuandoCatalogoRespondeUnauthorized() {
        when(responseSpec.bodyToMono(CatalogScheduleResponse[].class))
                .thenReturn(Mono.error(errorDeCatalogo(401)))
                .thenReturn(Mono.just(new CatalogScheduleResponse[]{horario(DayOfWeek.MONDAY, true)}));

        assertThat(adapter.findEnabledByProfessionalId(PROFESIONAL)).hasSize(1);

        verify(tokenService, times(1)).invalidate();
    }

    @Test
    void debeLanzarExcepcionSinReintentarDeNuevo_cuandoElTokenRenovadoTambienEsRechazado() {
        when(responseSpec.bodyToMono(CatalogScheduleResponse[].class))
                .thenReturn(Mono.error(errorDeCatalogo(403)))
                .thenReturn(Mono.error(errorDeCatalogo(403)));

        assertThatThrownBy(() -> adapter.findEnabledByProfessionalId(PROFESIONAL))
                .isInstanceOf(CatalogUnavailableException.class)
                .hasCauseInstanceOf(CatalogAuthenticationException.class);
        verify(tokenService, times(1)).invalidate();
        verify(responseSpec, times(2)).bodyToMono(CatalogScheduleResponse[].class);
    }

    @Test
    void debeLanzarCatalogUnavailable_cuandoCatalogoRespondeErrorDelServidor() {
        when(responseSpec.bodyToMono(CatalogScheduleResponse[].class)).thenReturn(Mono.error(errorDeCatalogo(500)));

        assertThatThrownBy(() -> adapter.findEnabledByProfessionalId(PROFESIONAL))
                .isInstanceOf(CatalogUnavailableException.class);
        verify(tokenService, never()).invalidate();
    }

    @Test
    void debeLanzarCatalogUnavailable_cuandoCatalogoNoResponde() {
        WebClientRequestException sinConexion = new WebClientRequestException(
                new ConnectException("Connection refused"), HttpMethod.GET, URI.create("http://localhost:8080"), HttpHeaders.EMPTY);
        when(responseSpec.bodyToMono(CatalogScheduleResponse[].class)).thenReturn(Mono.error(sinConexion));

        assertThatThrownBy(() -> adapter.findEnabledByProfessionalId(PROFESIONAL))
                .isInstanceOf(CatalogUnavailableException.class)
                .hasCauseInstanceOf(WebClientRequestException.class);
    }

    private static CatalogScheduleResponse horario(DayOfWeek dia, boolean habilitado) {
        return new CatalogScheduleResponse(1L, dia, LocalTime.of(9, 0), LocalTime.of(12, 0), 30, habilitado);
    }

    private WebClientResponseException errorDeCatalogo(int status) {
        return WebClientResponseException.create(status, "error", HttpHeaders.EMPTY, new byte[0], null);
    }
}
