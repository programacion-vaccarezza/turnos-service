package com.example.turnos_service.shared.infrastructure.client.catedra;

import com.example.turnos_service.shared.infrastructure.client.catedra.dto.CatedraProblemResponse;
import com.example.turnos_service.shared.infrastructure.client.catedra.exception.CatedraApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

/**
 * Convierte una respuesta de error de la cátedra en {@link CatedraApiException} con su status y code.
 * Se usa en los adaptadores con {@code .onStatus(HttpStatusCode::isError, catedraErrorHandler::toException)}.
 */
@Component
public class CatedraErrorHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CatedraErrorHandler.class);

    public Mono<? extends Throwable> toException(ClientResponse response) {
        int status = response.statusCode().value();
        return response.bodyToMono(CatedraProblemResponse.class)
                .mapNotNull(CatedraProblemResponse::code)
                .onErrorResume(e -> Mono.empty())
                .defaultIfEmpty("")
                .map(code -> {
                    String errorCode = code.isBlank() ? null : code;
                    LOG.warn("la cátedra respondió {} con code {}", status, errorCode);
                    return new CatedraApiException(status, errorCode);
                });
    }
}
