package com.example.turnos_service.reservation.infrastructure.client.adapter;

import com.example.turnos_service.reservation.application.exception.CatalogUnavailableException;
import com.example.turnos_service.reservation.domain.model.ProfessionalInfo;
import com.example.turnos_service.reservation.infrastructure.client.dto.CatalogProfessionalResponse;
import com.example.turnos_service.reservation.infrastructure.client.mapper.CatalogProfessionalMapper;
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
import java.util.Optional;
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
class CatalogProfessionalClientAdapterTest {

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

    private CatalogProfessionalClientAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CatalogProfessionalClientAdapter(webClient, tokenService, new CatalogProfessionalMapper());

        doReturn(uriSpec).when(webClient).get();
        doReturn(headersSpec).when(uriSpec).uri(anyString(), any(Object.class));
        doReturn(headersSpec).when(headersSpec).headers(any());
        doReturn(responseSpec).when(headersSpec).retrieve();
    }

    @Test
    void debeDevolverElProfesional_cuandoCatalogoRespondeOk() {
        when(responseSpec.bodyToMono(CatalogProfessionalResponse.class))
                .thenReturn(Mono.just(new CatalogProfessionalResponse(201L, 101L, "Ana", "Torres", true)));

        Optional<ProfessionalInfo> resultado = adapter.findById(201L);

        assertThat(resultado).isPresent();
        assertThat(resultado.get())
                .extracting("id", "firstName", "lastName", "enabled")
                .containsExactly(201L, "Ana", "Torres", true);
        verify(tokenService, never()).invalidate();
    }

    @Test
    void debeDevolverElProfesionalDeshabilitado_cuandoCatalogoLoInformaConEnabledFalse() {
        when(responseSpec.bodyToMono(CatalogProfessionalResponse.class))
                .thenReturn(Mono.just(new CatalogProfessionalResponse(202L, 101L, "Luis", "Gomez", false)));

        Optional<ProfessionalInfo> resultado = adapter.findById(202L);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().isEnabled()).isFalse();
    }

    @Test
    void debeDevolverVacio_cuandoElProfesionalNoExisteEnCatalogo() {
        when(responseSpec.bodyToMono(CatalogProfessionalResponse.class)).thenReturn(Mono.error(errorDeCatalogo(404)));

        Optional<ProfessionalInfo> resultado = adapter.findById(999L);

        assertThat(resultado).isEmpty();
        verify(tokenService, never()).invalidate();
    }

    @Test
    void debeRenovarElTokenYReintentar_cuandoCatalogoRespondeForbidden() {
        when(responseSpec.bodyToMono(CatalogProfessionalResponse.class))
                .thenReturn(Mono.error(errorDeCatalogo(403)))
                .thenReturn(Mono.just(new CatalogProfessionalResponse(201L, 101L, "Ana", "Torres", true)));

        Optional<ProfessionalInfo> resultado = adapter.findById(201L);

        assertThat(resultado).isPresent();
        verify(tokenService, times(1)).invalidate();
        verify(responseSpec, times(2)).bodyToMono(CatalogProfessionalResponse.class);
    }

    @Test
    void debeRenovarElTokenYReintentar_cuandoCatalogoRespondeUnauthorized() {
        when(responseSpec.bodyToMono(CatalogProfessionalResponse.class))
                .thenReturn(Mono.error(errorDeCatalogo(401)))
                .thenReturn(Mono.just(new CatalogProfessionalResponse(201L, 101L, "Ana", "Torres", true)));

        Optional<ProfessionalInfo> resultado = adapter.findById(201L);

        assertThat(resultado).isPresent();
        verify(tokenService, times(1)).invalidate();
    }

    @Test
    void debeLanzarCatalogUnavailableSinReintentarDeNuevo_cuandoElTokenRenovadoTambienEsRechazado() {
        when(responseSpec.bodyToMono(CatalogProfessionalResponse.class))
                .thenReturn(Mono.error(errorDeCatalogo(403)))
                .thenReturn(Mono.error(errorDeCatalogo(403)));

        assertThatThrownBy(() -> adapter.findById(201L))
                .isInstanceOf(CatalogUnavailableException.class)
                .hasCauseInstanceOf(CatalogAuthenticationException.class);
        verify(tokenService, times(1)).invalidate();
        verify(responseSpec, times(2)).bodyToMono(CatalogProfessionalResponse.class);
    }

    @Test
    void debeLanzarCatalogUnavailable_cuandoCatalogoRespondeErrorDelServidor() {
        when(responseSpec.bodyToMono(CatalogProfessionalResponse.class)).thenReturn(Mono.error(errorDeCatalogo(500)));

        assertThatThrownBy(() -> adapter.findById(201L))
                .isInstanceOf(CatalogUnavailableException.class);
        verify(tokenService, never()).invalidate();
    }

    @Test
    void debeLanzarCatalogUnavailable_cuandoCatalogoNoResponde() {
        WebClientRequestException sinConexion = new WebClientRequestException(
                new ConnectException("Connection refused"), HttpMethod.GET, URI.create("http://localhost:8080"), HttpHeaders.EMPTY);
        when(responseSpec.bodyToMono(CatalogProfessionalResponse.class)).thenReturn(Mono.error(sinConexion));

        assertThatThrownBy(() -> adapter.findById(201L))
                .isInstanceOf(CatalogUnavailableException.class)
                .hasCauseInstanceOf(WebClientRequestException.class);
        verify(tokenService, never()).invalidate();
    }

    @Test
    void debeEnviarElTokenDeServicioYElId_cuandoConsultaAlProfesional() {
        when(responseSpec.bodyToMono(CatalogProfessionalResponse.class))
                .thenReturn(Mono.just(new CatalogProfessionalResponse(201L, 101L, "Ana", "Torres", true)));
        when(tokenService.getToken()).thenReturn("token-de-servicio");

        adapter.findById(201L);

        verify(uriSpec).uri("/api/internal/professionals/{id}", 201L);
        verify(headersSpec).headers(headersCaptor.capture());
        HttpHeaders headers = new HttpHeaders();
        headersCaptor.getValue().accept(headers);
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token-de-servicio");
    }

    private WebClientResponseException errorDeCatalogo(int status) {
        return WebClientResponseException.create(status, "error", HttpHeaders.EMPTY, new byte[0], null);
    }
}
