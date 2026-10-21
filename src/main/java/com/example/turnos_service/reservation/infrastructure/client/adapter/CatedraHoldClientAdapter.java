package com.example.turnos_service.reservation.infrastructure.client.adapter;

import com.example.turnos_service.reservation.application.exception.CatedraRejectedException;
import com.example.turnos_service.reservation.application.exception.CatedraUnavailableException;
import com.example.turnos_service.reservation.domain.model.Hold;
import com.example.turnos_service.reservation.domain.ports.out.CatedraHoldPort;
import com.example.turnos_service.reservation.infrastructure.client.dto.CreateHoldResponse;
import com.example.turnos_service.reservation.infrastructure.client.mapper.CatedraHoldMapper;
import com.example.turnos_service.shared.infrastructure.client.catedra.CatedraErrorHandler;
import com.example.turnos_service.shared.infrastructure.client.catedra.exception.CatedraApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.time.LocalDate;
import java.time.LocalTime;

@Component
public class CatedraHoldClientAdapter implements CatedraHoldPort {

    private static final Logger LOG = LoggerFactory.getLogger(CatedraHoldClientAdapter.class);
    private static final String HOLDS_PATH = "/api/appointment-holds";

    private final WebClient catedraWebClient;
    private final CatedraErrorHandler catedraErrorHandler;
    private final CatedraHoldMapper catedraHoldMapper;

    public CatedraHoldClientAdapter(@Qualifier("catedraWebClient") WebClient catedraWebClient,
                                    CatedraErrorHandler catedraErrorHandler,
                                    CatedraHoldMapper catedraHoldMapper) {
        this.catedraWebClient = catedraWebClient;
        this.catedraErrorHandler = catedraErrorHandler;
        this.catedraHoldMapper = catedraHoldMapper;
    }

    @Override
    public Hold createHold(Long professionalId, LocalDate date, LocalTime startTime, String externalPatientId) {
        LOG.info("solicitando hold a la cátedra para el profesional {} el {} a las {}", professionalId, date, startTime);
        try {
            CreateHoldResponse response = catedraWebClient
                    .post()
                    .uri(HOLDS_PATH)
                    .bodyValue(catedraHoldMapper.toRequest(professionalId, date, startTime, externalPatientId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, catedraErrorHandler::toException)
                    .bodyToMono(CreateHoldResponse.class)
                    .block();

            Hold hold = catedraHoldMapper.toDomain(response);
            if (hold == null || hold.getHoldId() == null || hold.getReservationProcessId() == null) {
                throw new CatedraUnavailableException("la cátedra no devolvió los datos del hold", null);
            }
            LOG.info("hold {} creado para el proceso {} con vencimiento {}",
                    hold.getHoldId(), hold.getReservationProcessId(), hold.getExpiresAt());
            return hold;
        } catch (CatedraApiException e) {
            if (e.getStatus() >= 500) {
                LOG.error("la cátedra no pudo crear el hold por un error {}", e.getStatus());
                throw new CatedraUnavailableException("la cátedra respondió " + e.getStatus() + " al crear el hold", e);
            }
            throw new CatedraRejectedException(e.getStatus(), e.getCode());
        } catch (WebClientRequestException e) {
            LOG.error("no se pudo contactar a la cátedra para crear el hold: {}", e.getMessage());
            throw new CatedraUnavailableException("no se pudo contactar a la cátedra para crear el hold", e);
        }
    }
}
