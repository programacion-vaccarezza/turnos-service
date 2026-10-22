package com.example.turnos_service.reservation.infrastructure.client.adapter;

import com.example.turnos_service.reservation.application.exception.CatalogUnavailableException;
import com.example.turnos_service.reservation.domain.model.ProfessionalInfo;
import com.example.turnos_service.reservation.domain.ports.out.CatalogProfessionalPort;
import com.example.turnos_service.reservation.infrastructure.client.dto.CatalogProfessionalResponse;
import com.example.turnos_service.reservation.infrastructure.client.mapper.CatalogProfessionalMapper;
import com.example.turnos_service.shared.infrastructure.client.catalog.CatalogTokenService;
import com.example.turnos_service.shared.infrastructure.client.catalog.exception.CatalogAuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class CatalogProfessionalClientAdapter implements CatalogProfessionalPort {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogProfessionalClientAdapter.class);
    private static final String INTERNAL_PROFESSIONALS_PATH = "/api/internal/professionals/{id}";

    private final WebClient catalogWebClient;
    private final CatalogTokenService catalogTokenService;
    private final CatalogProfessionalMapper catalogProfessionalMapper;

    public CatalogProfessionalClientAdapter(@Qualifier("catalogWebClient") WebClient catalogWebClient,
                                            CatalogTokenService catalogTokenService,
                                            CatalogProfessionalMapper catalogProfessionalMapper) {
        this.catalogWebClient = catalogWebClient;
        this.catalogTokenService = catalogTokenService;
        this.catalogProfessionalMapper = catalogProfessionalMapper;
    }

    @Override
    public Optional<ProfessionalInfo> findById(Long professionalId) {
        try {
            return findByIdWithRetry(professionalId);
        } catch (CatalogAuthenticationException | WebClientRequestException | WebClientResponseException e) {
            throw new CatalogUnavailableException("no se pudo consultar catalogo-service", e);
        }
    }

    private Optional<ProfessionalInfo> findByIdWithRetry(Long professionalId) {
        LOG.info("consultando a catalogo-service el profesional {}", professionalId);
        try {
            return requestProfessional(professionalId);
        } catch (WebClientResponseException.Unauthorized | WebClientResponseException.Forbidden e) {
            LOG.warn("catalogo-service rechazó el token de servicio con {}, se renueva y se reintenta una vez", e.getStatusCode());
            catalogTokenService.invalidate();
            return retryRequestProfessional(professionalId);
        }
    }

    private Optional<ProfessionalInfo> retryRequestProfessional(Long professionalId) {
        try {
            return requestProfessional(professionalId);
        } catch (WebClientResponseException.Unauthorized | WebClientResponseException.Forbidden e) {
            LOG.error("catalogo-service rechazó también el token renovado con {}", e.getStatusCode());
            throw new CatalogAuthenticationException("catalogo rechazó el token de servicio renovado con " + e.getStatusCode());
        }
    }

    private Optional<ProfessionalInfo> requestProfessional(Long professionalId) {
        CatalogProfessionalResponse response = catalogWebClient
                .get()
                .uri(INTERNAL_PROFESSIONALS_PATH, professionalId)
                .headers(h -> h.setBearerAuth(catalogTokenService.getToken()))
                .retrieve()
                .bodyToMono(CatalogProfessionalResponse.class)
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    LOG.warn("el profesional {} no existe en catalogo-service", professionalId);
                    return Mono.empty();
                })
                .block();
        return Optional.ofNullable(catalogProfessionalMapper.toDomain(response));
    }
}
