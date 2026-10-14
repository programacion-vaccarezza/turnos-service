package com.example.turnos_service.reservation.infrastructure.client.adapter;

import com.example.turnos_service.reservation.domain.model.PatientInfo;
import com.example.turnos_service.reservation.infrastructure.client.dto.CatalogUserResponse;
import com.example.turnos_service.reservation.infrastructure.client.mapper.CatalogUserMapper;
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
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

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
class CatalogUserClientAdapterTest {

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

    private CatalogUserClientAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CatalogUserClientAdapter(webClient, tokenService, new CatalogUserMapper());

        doReturn(uriSpec).when(webClient).get();
        doReturn(headersSpec).when(uriSpec).uri(anyString(), anyString());
        doReturn(headersSpec).when(headersSpec).headers(any());
        doReturn(responseSpec).when(headersSpec).retrieve();
    }

    @Test
    void debeDevolverLosDatosDelUsuario_cuandoCatalogoRespondeOk() {
        when(responseSpec.bodyToMono(CatalogUserResponse.class))
                .thenReturn(Mono.just(new CatalogUserResponse("juan.perez", "Juan", "Perez")));

        Optional<PatientInfo> resultado = adapter.findByLogin("juan.perez");

        assertThat(resultado).isPresent();
        assertThat(resultado.get())
                .extracting("login", "firstName", "lastName")
                .containsExactly("juan.perez", "Juan", "Perez");
        verify(tokenService, never()).invalidate();
    }

    @Test
    void debeDevolverVacio_cuandoElUsuarioNoExisteEnCatalogo() {
        when(responseSpec.bodyToMono(CatalogUserResponse.class)).thenReturn(Mono.error(errorDeCatalogo(404)));

        Optional<PatientInfo> resultado = adapter.findByLogin("no.existe");

        assertThat(resultado).isEmpty();
        verify(tokenService, never()).invalidate();
    }

    @Test
    void debeRenovarElTokenYReintentar_cuandoCatalogoRespondeForbidden() {
        when(responseSpec.bodyToMono(CatalogUserResponse.class))
                .thenReturn(Mono.error(errorDeCatalogo(403)))
                .thenReturn(Mono.just(new CatalogUserResponse("juan.perez", "Juan", "Perez")));

        Optional<PatientInfo> resultado = adapter.findByLogin("juan.perez");

        assertThat(resultado).isPresent();
        verify(tokenService, times(1)).invalidate();
        verify(responseSpec, times(2)).bodyToMono(CatalogUserResponse.class);
    }

    @Test
    void debeRenovarElTokenYReintentar_cuandoCatalogoRespondeUnauthorized() {
        when(responseSpec.bodyToMono(CatalogUserResponse.class))
                .thenReturn(Mono.error(errorDeCatalogo(401)))
                .thenReturn(Mono.just(new CatalogUserResponse("juan.perez", "Juan", "Perez")));

        Optional<PatientInfo> resultado = adapter.findByLogin("juan.perez");

        assertThat(resultado).isPresent();
        verify(tokenService, times(1)).invalidate();
    }

    @Test
    void debeLanzarExcepcionSinReintentarDeNuevo_cuandoElTokenRenovadoTambienEsRechazado() {
        when(responseSpec.bodyToMono(CatalogUserResponse.class))
                .thenReturn(Mono.error(errorDeCatalogo(403)))
                .thenReturn(Mono.error(errorDeCatalogo(403)));

        assertThatThrownBy(() -> adapter.findByLogin("juan.perez"))
                .isInstanceOf(CatalogAuthenticationException.class);
        verify(tokenService, times(1)).invalidate();
        verify(responseSpec, times(2)).bodyToMono(CatalogUserResponse.class);
    }

    @Test
    void debeEnviarElTokenDeServicioYElLogin_cuandoConsultaAlUsuario() {
        when(responseSpec.bodyToMono(CatalogUserResponse.class))
                .thenReturn(Mono.just(new CatalogUserResponse("juan.perez", "Juan", "Perez")));
        when(tokenService.getToken()).thenReturn("token-de-servicio");

        adapter.findByLogin("juan.perez");

        verify(uriSpec).uri("/api/internal/users/{login}", "juan.perez");
        verify(headersSpec).headers(headersCaptor.capture());
        HttpHeaders headers = new HttpHeaders();
        headersCaptor.getValue().accept(headers);
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token-de-servicio");
    }

    private WebClientResponseException errorDeCatalogo(int status) {
        return WebClientResponseException.create(status, "error", HttpHeaders.EMPTY, new byte[0], null);
    }
}
