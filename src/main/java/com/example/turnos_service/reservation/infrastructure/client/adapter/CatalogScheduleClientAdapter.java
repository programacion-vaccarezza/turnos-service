package com.example.turnos_service.reservation.infrastructure.client.adapter;

import com.example.turnos_service.reservation.application.exception.CatalogUnavailableException;
import com.example.turnos_service.reservation.application.exception.ProfessionalNotFoundException;
import com.example.turnos_service.reservation.domain.model.WeeklySchedule;
import com.example.turnos_service.reservation.domain.ports.out.CatalogSchedulePort;
import com.example.turnos_service.reservation.infrastructure.client.dto.CatalogScheduleResponse;
import com.example.turnos_service.reservation.infrastructure.client.mapper.CatalogScheduleMapper;
import com.example.turnos_service.shared.infrastructure.client.catalog.CatalogTokenService;
import com.example.turnos_service.shared.infrastructure.client.catalog.exception.CatalogAuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Arrays;
import java.util.List;

/** Consulta los horarios semanales vigentes del profesional. Catalogo incluye los deshabilitados: se filtran acá. */
@Component
public class CatalogScheduleClientAdapter implements CatalogSchedulePort {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogScheduleClientAdapter.class);
    private static final String INTERNAL_SCHEDULES_PATH = "/api/internal/professionals/{id}/schedules";

    private final WebClient catalogWebClient;
    private final CatalogTokenService catalogTokenService;
    private final CatalogScheduleMapper catalogScheduleMapper;

    public CatalogScheduleClientAdapter(@Qualifier("catalogWebClient") WebClient catalogWebClient,
                                        CatalogTokenService catalogTokenService,
                                        CatalogScheduleMapper catalogScheduleMapper) {
        this.catalogWebClient = catalogWebClient;
        this.catalogTokenService = catalogTokenService;
        this.catalogScheduleMapper = catalogScheduleMapper;
    }

    @Override
    public List<WeeklySchedule> findEnabledByProfessionalId(Long professionalId) {
        try {
            return Arrays.stream(requestWithRetry(professionalId))
                    .filter(CatalogScheduleResponse::enabled)
                    .map(catalogScheduleMapper::toDomain)
                    .toList();
        } catch (WebClientResponseException.NotFound e) {
            LOG.warn("el profesional {} no existe en catalogo-service", professionalId);
            throw new ProfessionalNotFoundException(professionalId);
        } catch (CatalogAuthenticationException | WebClientRequestException | WebClientResponseException e) {
            throw new CatalogUnavailableException("no se pudieron consultar los horarios en catalogo-service", e);
        }
    }

    private CatalogScheduleResponse[] requestWithRetry(Long professionalId) {
        LOG.info("consultando a catalogo-service los horarios del profesional {}", professionalId);
        try {
            return requestSchedules(professionalId);
        } catch (WebClientResponseException.Unauthorized | WebClientResponseException.Forbidden e) {
            LOG.warn("catalogo-service rechazó el token de servicio con {}, se renueva y se reintenta una vez", e.getStatusCode());
            catalogTokenService.invalidate();
            return retryRequestSchedules(professionalId);
        }
    }

    private CatalogScheduleResponse[] retryRequestSchedules(Long professionalId) {
        try {
            return requestSchedules(professionalId);
        } catch (WebClientResponseException.Unauthorized | WebClientResponseException.Forbidden e) {
            LOG.error("catalogo-service rechazó también el token renovado con {}", e.getStatusCode());
            throw new CatalogAuthenticationException("catalogo rechazó el token de servicio renovado con " + e.getStatusCode());
        }
    }

    private CatalogScheduleResponse[] requestSchedules(Long professionalId) {
        return catalogWebClient
                .get()
                .uri(INTERNAL_SCHEDULES_PATH, professionalId)
                .headers(h -> h.setBearerAuth(catalogTokenService.getToken()))
                .retrieve()
                .bodyToMono(CatalogScheduleResponse[].class)
                .block();
    }
}
