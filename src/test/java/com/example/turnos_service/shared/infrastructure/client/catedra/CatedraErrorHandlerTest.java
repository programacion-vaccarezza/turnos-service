package com.example.turnos_service.shared.infrastructure.client.catedra;

import com.example.turnos_service.shared.infrastructure.client.catedra.dto.CatedraProblemResponse;
import com.example.turnos_service.shared.infrastructure.client.catedra.exception.CatedraApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatedraErrorHandlerTest {

    @Mock
    ClientResponse clientResponse;

    private final CatedraErrorHandler handler = new CatedraErrorHandler();

    @Test
    void debeIncluirStatusYCode_cuandoLaCatedraRespondeUnErrorConCode() {
        when(clientResponse.statusCode()).thenReturn(HttpStatusCode.valueOf(409));
        when(clientResponse.bodyToMono(CatedraProblemResponse.class))
                .thenReturn(Mono.just(new CatedraProblemResponse(409, "SLOT_ALREADY_HELD", "Slot already held")));

        Throwable error = handler.toException(clientResponse).block();

        assertThat(error).isInstanceOf(CatedraApiException.class);
        assertThat((CatedraApiException) error)
                .extracting("status", "code")
                .containsExactly(409, "SLOT_ALREADY_HELD");
    }

    @Test
    void debeDejarCodeNulo_cuandoElErrorNoTraeCode() {
        when(clientResponse.statusCode()).thenReturn(HttpStatusCode.valueOf(403));
        when(clientResponse.bodyToMono(CatedraProblemResponse.class))
                .thenReturn(Mono.just(new CatedraProblemResponse(403, null, "Forbidden")));

        CatedraApiException error = (CatedraApiException) handler.toException(clientResponse).block();

        assertThat(error.getStatus()).isEqualTo(403);
        assertThat(error.getCode()).isNull();
    }

    @Test
    void debeDejarCodeNulo_cuandoElErrorVieneSinCuerpo() {
        when(clientResponse.statusCode()).thenReturn(HttpStatusCode.valueOf(401));
        when(clientResponse.bodyToMono(CatedraProblemResponse.class)).thenReturn(Mono.empty());

        CatedraApiException error = (CatedraApiException) handler.toException(clientResponse).block();

        assertThat(error.getStatus()).isEqualTo(401);
        assertThat(error.getCode()).isNull();
    }

    @Test
    void debeDejarCodeNulo_cuandoElCuerpoNoEsJson() {
        when(clientResponse.statusCode()).thenReturn(HttpStatusCode.valueOf(502));
        when(clientResponse.bodyToMono(CatedraProblemResponse.class))
                .thenReturn(Mono.error(new DecodingException("no es JSON")));

        CatedraApiException error = (CatedraApiException) handler.toException(clientResponse).block();

        assertThat(error.getStatus()).isEqualTo(502);
        assertThat(error.getCode()).isNull();
    }
}
