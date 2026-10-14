package com.example.turnos_service.reservation.infrastructure.client.adapter;

import com.example.turnos_service.reservation.domain.model.PatientInfo;
import com.example.turnos_service.reservation.domain.ports.out.CatalogUserPort;
import com.example.turnos_service.reservation.infrastructure.client.dto.CatalogUserResponse;
import com.example.turnos_service.reservation.infrastructure.client.mapper.CatalogUserMapper;
import com.example.turnos_service.shared.infrastructure.client.catalog.CatalogTokenService;
import com.example.turnos_service.shared.infrastructure.client.catalog.exception.CatalogAuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class CatalogUserClientAdapter implements CatalogUserPort {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogUserClientAdapter.class);
    private static final String INTERNAL_USERS_PATH = "/api/internal/users/{login}";

    private final WebClient catalogWebClient;
    private final CatalogTokenService catalogTokenService;
    private final CatalogUserMapper catalogUserMapper;

    public CatalogUserClientAdapter(@Qualifier("catalogWebClient") WebClient catalogWebClient,
                                    CatalogTokenService catalogTokenService,
                                    CatalogUserMapper catalogUserMapper) {
        this.catalogWebClient = catalogWebClient;
        this.catalogTokenService = catalogTokenService;
        this.catalogUserMapper = catalogUserMapper;
    }

    @Override
    public Optional<PatientInfo> findByLogin(String login) {
        try {
            return requestUser(login);
        } catch (WebClientResponseException.Unauthorized | WebClientResponseException.Forbidden e) {
            LOG.debug("catalogo rechazó el token de servicio con {}, se reintenta con uno nuevo", e.getStatusCode());
            catalogTokenService.invalidate();
            return retryRequestUser(login);
        }
    }

    private Optional<PatientInfo> retryRequestUser(String login) {
        try {
            return requestUser(login);
        } catch (WebClientResponseException.Unauthorized | WebClientResponseException.Forbidden e) {
            throw new CatalogAuthenticationException("catalogo rechazó el token de servicio renovado con " + e.getStatusCode());
        }
    }

    private Optional<PatientInfo> requestUser(String login) {
        CatalogUserResponse response = catalogWebClient
                .get()
                .uri(INTERNAL_USERS_PATH, login)
                .headers(h -> h.setBearerAuth(catalogTokenService.getToken()))
                .retrieve()
                .bodyToMono(CatalogUserResponse.class)
                .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty())
                .block();
        return Optional.ofNullable(catalogUserMapper.toDomain(response));
    }
}
