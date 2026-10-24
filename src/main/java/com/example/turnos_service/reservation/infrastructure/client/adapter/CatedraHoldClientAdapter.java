package com.example.turnos_service.reservation.infrastructure.client.adapter;

import com.example.turnos_service.reservation.application.exception.CatedraRejectedException;
import com.example.turnos_service.reservation.application.exception.CatedraUnavailableException;
import com.example.turnos_service.reservation.domain.model.Hold;
import com.example.turnos_service.reservation.domain.model.HoldConfirmation;
import com.example.turnos_service.reservation.domain.ports.out.CatedraHoldPort;
import com.example.turnos_service.reservation.infrastructure.client.dto.ConfirmHoldResponse;
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
import java.util.function.Supplier;

@Component
public class CatedraHoldClientAdapter implements CatedraHoldPort {

    private static final Logger LOG = LoggerFactory.getLogger(CatedraHoldClientAdapter.class);
    private static final String HOLDS_PATH = "/api/appointment-holds";
    private static final String CONFIRM_PATH = "/api/appointment-holds/{holdId}/confirm";

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
        Hold hold = callCatedra("crear el hold", () -> catedraHoldMapper.toDomain(catedraWebClient
                .post()
                .uri(HOLDS_PATH)
                .bodyValue(catedraHoldMapper.toRequest(professionalId, date, startTime, externalPatientId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, catedraErrorHandler::toException)
                .bodyToMono(CreateHoldResponse.class)
                .block()));

        if (hold == null || hold.getHoldId() == null || hold.getReservationProcessId() == null) {
            throw new CatedraUnavailableException("la cátedra no devolvió los datos del hold", null);
        }
        LOG.info("hold {} creado para el proceso {} con vencimiento {}",
                hold.getHoldId(), hold.getReservationProcessId(), hold.getExpiresAt());
        return hold;
    }

    @Override
    public HoldConfirmation confirmHold(String holdId, String externalPatientId, String patientFirstName, String patientLastName) {
        LOG.info("confirmando en la cátedra el hold {}", holdId);
        HoldConfirmation confirmation = callCatedra("confirmar el hold", () -> catedraHoldMapper.toDomain(catedraWebClient
                .post()
                .uri(CONFIRM_PATH, holdId)
                .bodyValue(catedraHoldMapper.toConfirmRequest(externalPatientId, patientFirstName, patientLastName))
                .retrieve()
                .onStatus(HttpStatusCode::isError, catedraErrorHandler::toException)
                .bodyToMono(ConfirmHoldResponse.class)
                .block()));

        if (confirmation == null || confirmation.getReservationProcessId() == null) {
            throw new CatedraUnavailableException("la cátedra no devolvió los datos de la confirmación", null);
        }
        LOG.info("hold {} confirmado, el proceso {} espera el teléfono hasta {}",
                holdId, confirmation.getReservationProcessId(), confirmation.getExpiresAt());
        return confirmation;
    }

    /**
     * Traduce los errores de la llamada a la cátedra:
     * 4xx → {@link CatedraRejectedException} con su code; 5xx, sin conexión o timeout → {@link CatedraUnavailableException}.
     */
    private <T> T callCatedra(String operation, Supplier<T> call) {
        try {
            return call.get();
        } catch (CatedraApiException e) {
            if (e.getStatus() >= 500) {
                LOG.error("la cátedra no pudo {} por un error {}", operation, e.getStatus());
                throw new CatedraUnavailableException("la cátedra respondió " + e.getStatus() + " al " + operation, e);
            }
            throw new CatedraRejectedException(e.getStatus(), e.getCode());
        } catch (WebClientRequestException e) {
            LOG.error("no se pudo contactar a la cátedra para {}: {}", operation, e.getMessage());
            throw new CatedraUnavailableException("no se pudo contactar a la cátedra para " + operation, e);
        }
    }
}
