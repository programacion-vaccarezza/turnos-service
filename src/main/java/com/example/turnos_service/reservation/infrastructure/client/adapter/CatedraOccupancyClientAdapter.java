package com.example.turnos_service.reservation.infrastructure.client.adapter;

import com.example.turnos_service.reservation.application.exception.CatedraRejectedException;
import com.example.turnos_service.reservation.application.exception.CatedraUnavailableException;
import com.example.turnos_service.reservation.domain.model.Occupancy;
import com.example.turnos_service.reservation.domain.ports.out.CatedraOccupancyPort;
import com.example.turnos_service.reservation.infrastructure.client.dto.CatedraOccupanciesResponse;
import com.example.turnos_service.reservation.infrastructure.client.mapper.CatedraOccupancyMapper;
import com.example.turnos_service.shared.infrastructure.client.catedra.CatedraErrorHandler;
import com.example.turnos_service.shared.infrastructure.client.catedra.exception.CatedraApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.time.LocalDate;
import java.util.List;

@Component
public class CatedraOccupancyClientAdapter implements CatedraOccupancyPort {

    private static final Logger LOG = LoggerFactory.getLogger(CatedraOccupancyClientAdapter.class);
    private static final String OCCUPANCIES_PATH = "/api/appointment-occupancies";

    private final WebClient catedraWebClient;
    private final CatedraErrorHandler catedraErrorHandler;
    private final CatedraOccupancyMapper catedraOccupancyMapper;

    public CatedraOccupancyClientAdapter(@Qualifier("catedraWebClient") WebClient catedraWebClient,
                                         CatedraErrorHandler catedraErrorHandler,
                                         CatedraOccupancyMapper catedraOccupancyMapper) {
        this.catedraWebClient = catedraWebClient;
        this.catedraErrorHandler = catedraErrorHandler;
        this.catedraOccupancyMapper = catedraOccupancyMapper;
    }

    @Override
    public List<Occupancy> findByProfessionalAndDate(Long professionalId, LocalDate date) {
        LOG.info("consultando a la cátedra las ocupaciones del profesional {} el {}", professionalId, date);
        try {
            List<Occupancy> occupancies = catedraOccupancyMapper.toDomain(catedraWebClient
                    .get()
                    .uri(uriBuilder -> uriBuilder.path(OCCUPANCIES_PATH)
                            .queryParam("professionalId", professionalId)
                            .queryParam("from", date)
                            .queryParam("to", date)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, catedraErrorHandler::toException)
                    .bodyToMono(CatedraOccupanciesResponse.class)
                    .block());
            LOG.info("la cátedra devolvió {} ocupaciones del profesional {} el {}", occupancies.size(), professionalId, date);
            return occupancies;
        } catch (CatedraApiException e) {
            if (e.getStatus() >= 500) {
                LOG.error("la cátedra no pudo devolver las ocupaciones por un error {}", e.getStatus());
                throw new CatedraUnavailableException("la cátedra respondió " + e.getStatus() + " al consultar las ocupaciones", e);
            }
            throw new CatedraRejectedException(e.getStatus(), e.getCode());
        } catch (WebClientRequestException e) {
            LOG.error("no se pudo contactar a la cátedra para consultar las ocupaciones: {}", e.getMessage());
            throw new CatedraUnavailableException("no se pudo contactar a la cátedra para consultar las ocupaciones", e);
        } catch (DecodingException e) {
            LOG.error("la cátedra devolvió una respuesta inválida al consultar las ocupaciones: {}", e.getMessage());
            throw new CatedraUnavailableException("la cátedra devolvió una respuesta inválida al consultar las ocupaciones", e);
        }
    }
}
