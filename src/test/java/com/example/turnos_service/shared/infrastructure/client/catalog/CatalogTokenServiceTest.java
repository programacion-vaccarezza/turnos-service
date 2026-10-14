package com.example.turnos_service.shared.infrastructure.client.catalog;

import com.example.turnos_service.shared.infrastructure.client.catalog.config.CatalogConfiguration;
import com.example.turnos_service.shared.infrastructure.client.catalog.dto.ServiceTokenRequest;
import com.example.turnos_service.shared.infrastructure.client.catalog.dto.ServiceTokenResponse;
import com.example.turnos_service.shared.infrastructure.client.catalog.exception.CatalogAuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Function;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogTokenServiceTest {

    private static final CatalogConfiguration CFG =
            new CatalogConfiguration("http://localhost:8080", "turnos-service", "secreto-de-prueba");

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

    @Mock
    ClientResponse clientResponse;

    @Captor
    ArgumentCaptor<Predicate<HttpStatusCode>> statusPredicateCaptor;

    @Captor
    ArgumentCaptor<Function<ClientResponse, Mono<? extends Throwable>>> errorHandlerCaptor;

    private CatalogTokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new CatalogTokenService(webClient, CFG);

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        doReturn(headersSpec).when(bodySpec).bodyValue(any());
        doReturn(responseSpec).when(headersSpec).retrieve();
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    }

    @Test
    void debePedirElTokenACatalogo_cuandoNoHayTokenGuardado() {
        when(responseSpec.bodyToMono(ServiceTokenResponse.class))
                .thenReturn(Mono.just(new ServiceTokenResponse("token-1")));

        String token = tokenService.getToken();

        assertThat(token).isEqualTo("token-1");
    }

    @Test
    void debeEnviarLasCredencialesDelServicio_cuandoPideElToken() {
        when(responseSpec.bodyToMono(ServiceTokenResponse.class))
                .thenReturn(Mono.just(new ServiceTokenResponse("token-1")));

        tokenService.getToken();

        verify(uriSpec).uri("/api/auth/service-token");
        verify(bodySpec).bodyValue(new ServiceTokenRequest("turnos-service", "secreto-de-prueba"));
    }

    @Test
    void debeReutilizarElToken_cuandoYaFueObtenido() {
        when(responseSpec.bodyToMono(ServiceTokenResponse.class))
                .thenReturn(Mono.just(new ServiceTokenResponse("token-1")));

        String primero = tokenService.getToken();
        String segundo = tokenService.getToken();

        assertThat(segundo).isEqualTo(primero);
        verify(webClient, times(1)).post();
    }

    @Test
    void debePedirUnTokenNuevo_cuandoSeInvalidaElActual() {
        when(responseSpec.bodyToMono(ServiceTokenResponse.class))
                .thenReturn(Mono.just(new ServiceTokenResponse("token-1")))
                .thenReturn(Mono.just(new ServiceTokenResponse("token-2")));

        tokenService.getToken();
        tokenService.invalidate();
        String nuevo = tokenService.getToken();

        assertThat(nuevo).isEqualTo("token-2");
        verify(webClient, times(2)).post();
    }

    @Test
    void debeLanzarExcepcion_cuandoCatalogoRechazaLasCredenciales() {
        when(responseSpec.bodyToMono(ServiceTokenResponse.class))
                .thenReturn(Mono.just(new ServiceTokenResponse("token-1")));
        tokenService.getToken();
        verify(responseSpec).onStatus(statusPredicateCaptor.capture(), errorHandlerCaptor.capture());
        when(clientResponse.statusCode()).thenReturn(HttpStatusCode.valueOf(401));

        boolean seManejaComoError = statusPredicateCaptor.getValue().test(HttpStatusCode.valueOf(401));

        assertThat(seManejaComoError).isTrue();
        assertThatThrownBy(() -> errorHandlerCaptor.getValue().apply(clientResponse).block())
                .isInstanceOf(CatalogAuthenticationException.class);
    }

    @Test
    void debeLanzarExcepcion_cuandoCatalogoNoDevuelveToken() {
        when(responseSpec.bodyToMono(ServiceTokenResponse.class)).thenReturn(Mono.empty());

        assertThatThrownBy(() -> tokenService.getToken())
                .isInstanceOf(CatalogAuthenticationException.class);
    }
}
