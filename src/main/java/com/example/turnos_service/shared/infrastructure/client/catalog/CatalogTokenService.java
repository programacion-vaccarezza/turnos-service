package com.example.turnos_service.shared.infrastructure.client.catalog;

import com.example.turnos_service.shared.infrastructure.client.catalog.config.CatalogConfiguration;
import com.example.turnos_service.shared.infrastructure.client.catalog.dto.ServiceTokenRequest;
import com.example.turnos_service.shared.infrastructure.client.catalog.dto.ServiceTokenResponse;
import com.example.turnos_service.shared.infrastructure.client.catalog.exception.CatalogAuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Obtiene y guarda en memoria el JWT técnico que catalogo-service emite para turnos-service.
 * Quien llama a catalogo debe invocar {@link #invalidate()} cuando catalogo rechaza el token
 * (401 o 403) y reintentar una sola vez.
 */
@Service
public class CatalogTokenService {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogTokenService.class);
    private static final String SERVICE_TOKEN_PATH = "/api/auth/service-token";

    private final WebClient catalogWebClient;
    private final CatalogConfiguration cfg;
    private String token;

    public CatalogTokenService(@Qualifier("catalogWebClient") WebClient catalogWebClient, CatalogConfiguration cfg) {
        this.catalogWebClient = catalogWebClient;
        this.cfg = cfg;
    }

    public synchronized String getToken() {
        if (token == null) {
            token = requestToken();
        }
        return token;
    }

    public synchronized void invalidate() {
        LOG.debug("token de servicio invalidado, se va a pedir uno nuevo");
        this.token = null;
    }

    private String requestToken() {
        LOG.debug("pidiendo token de servicio a {}", cfg.url());
        ServiceTokenResponse response = catalogWebClient
                .post()
                .uri(SERVICE_TOKEN_PATH)
                .bodyValue(new ServiceTokenRequest(cfg.clientId(), cfg.clientSecret()))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, resp -> Mono.error(
                        new CatalogAuthenticationException("catalogo rechazó las credenciales de servicio con " + resp.statusCode())))
                .bodyToMono(ServiceTokenResponse.class)
                .block();

        if (response == null || response.token() == null) {
            throw new CatalogAuthenticationException("catalogo no devolvió un token de servicio");
        }
        LOG.info("token de servicio obtenido de catalogo-service");
        return response.token();
    }
}
