package com.example.turnos_service.reservation.infrastructure.client.adapter;

import com.example.turnos_service.reservation.application.exception.CatedraRejectedException;
import com.example.turnos_service.reservation.application.exception.CatedraUnavailableException;
import com.example.turnos_service.reservation.domain.model.CatedraAppointment;
import com.example.turnos_service.reservation.domain.ports.out.CatedraAppointmentPort;
import com.example.turnos_service.reservation.infrastructure.client.dto.CancelAppointmentRequest;
import com.example.turnos_service.reservation.infrastructure.client.dto.CancelAppointmentResponse;
import com.example.turnos_service.reservation.infrastructure.client.dto.CatedraAppointmentResponse;
import com.example.turnos_service.reservation.infrastructure.client.mapper.CatedraAppointmentMapper;
import com.example.turnos_service.shared.infrastructure.client.catedra.CatedraErrorHandler;
import com.example.turnos_service.shared.infrastructure.client.catedra.exception.CatedraApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reservas de la cuenta técnica en la cátedra: listado por paciente (anexo §11), recorriendo todas las páginas,
 * y cancelación (anexo §12).
 */
@Component
public class CatedraAppointmentClientAdapter implements CatedraAppointmentPort {

    private static final Logger LOG = LoggerFactory.getLogger(CatedraAppointmentClientAdapter.class);
    private static final String APPOINTMENTS_PATH = "/api/appointments";
    private static final String CANCEL_PATH = "/api/appointments/{reservationProcessId}/cancel";
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 50;

    private final WebClient catedraWebClient;
    private final CatedraErrorHandler catedraErrorHandler;
    private final CatedraAppointmentMapper catedraAppointmentMapper;

    public CatedraAppointmentClientAdapter(@Qualifier("catedraWebClient") WebClient catedraWebClient,
                                           CatedraErrorHandler catedraErrorHandler,
                                           CatedraAppointmentMapper catedraAppointmentMapper) {
        this.catedraWebClient = catedraWebClient;
        this.catedraErrorHandler = catedraErrorHandler;
        this.catedraAppointmentMapper = catedraAppointmentMapper;
    }

    @Override
    public List<CatedraAppointment> findByExternalPatientId(String externalPatientId) {
        LOG.info("consultando a la cátedra las reservas del paciente {}", externalPatientId);
        List<CatedraAppointment> appointments = new ArrayList<>();
        for (int page = 0; page < MAX_PAGES; page++) {
            CatedraAppointmentResponse[] pageItems = fetchPage(externalPatientId, page);
            if (pageItems == null) {
                break;
            }
            Arrays.stream(pageItems).map(catedraAppointmentMapper::toDomain).forEach(appointments::add);
            if (pageItems.length < PAGE_SIZE) {
                break;
            }
        }
        LOG.info("la cátedra devolvió {} reservas del paciente {}", appointments.size(), externalPatientId);
        return appointments;
    }

    /**
     * Cancela la reserva. 4xx → {@link CatedraRejectedException} con su code;
     * 5xx, sin conexión o timeout → {@link CatedraUnavailableException}.
     */
    @Override
    public void cancel(String reservationProcessId, String reason) {
        LOG.info("cancelando en la cátedra la reserva del proceso {}", reservationProcessId);
        CancelAppointmentResponse response;
        try {
            response = catedraWebClient
                    .post()
                    .uri(CANCEL_PATH, reservationProcessId)
                    .bodyValue(new CancelAppointmentRequest(reason))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, catedraErrorHandler::toException)
                    .bodyToMono(CancelAppointmentResponse.class)
                    .block();
        } catch (CatedraApiException e) {
            if (e.getStatus() >= 500) {
                LOG.error("la cátedra no pudo cancelar la reserva por un error {}", e.getStatus());
                throw new CatedraUnavailableException("la cátedra respondió " + e.getStatus() + " al cancelar la reserva", e);
            }
            throw new CatedraRejectedException(e.getStatus(), e.getCode());
        } catch (WebClientRequestException e) {
            LOG.error("no se pudo contactar a la cátedra para cancelar la reserva: {}", e.getMessage());
            throw new CatedraUnavailableException("no se pudo contactar a la cátedra para cancelar la reserva", e);
        }
        LOG.info("la cátedra canceló la reserva {} del proceso {} el {}",
                response != null ? response.reservationId() : null, reservationProcessId,
                response != null ? response.cancelledAt() : null);
    }

    /** Listar no tiene rechazos de negocio: cualquier error se informa como {@link CatedraUnavailableException}. */
    private CatedraAppointmentResponse[] fetchPage(String externalPatientId, int page) {
        try {
            return catedraWebClient
                    .get()
                    .uri(uriBuilder -> uriBuilder.path(APPOINTMENTS_PATH)
                            .queryParam("externalPatientId", externalPatientId)
                            .queryParam("page", page)
                            .queryParam("size", PAGE_SIZE)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, catedraErrorHandler::toException)
                    .bodyToMono(CatedraAppointmentResponse[].class)
                    .block();
        } catch (CatedraApiException e) {
            LOG.error("la cátedra respondió {} al listar las reservas", e.getStatus());
            throw new CatedraUnavailableException("la cátedra respondió " + e.getStatus() + " al listar las reservas", e);
        } catch (WebClientRequestException e) {
            LOG.error("no se pudo contactar a la cátedra para listar las reservas: {}", e.getMessage());
            throw new CatedraUnavailableException("no se pudo contactar a la cátedra para listar las reservas", e);
        }
    }
}
