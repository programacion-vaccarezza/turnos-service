package com.example.turnos_service.reservation.infrastructure.client.adapter;

import com.example.turnos_service.reservation.application.exception.CatedraRejectedException;
import com.example.turnos_service.reservation.application.exception.CatedraUnavailableException;
import com.example.turnos_service.reservation.domain.model.CatedraAppointment;
import com.example.turnos_service.reservation.infrastructure.client.dto.CancelAppointmentRequest;
import com.example.turnos_service.reservation.infrastructure.client.dto.CancelAppointmentResponse;
import com.example.turnos_service.reservation.infrastructure.client.dto.CatedraAppointmentResponse;
import com.example.turnos_service.reservation.infrastructure.client.mapper.CatedraAppointmentMapper;
import com.example.turnos_service.shared.infrastructure.client.catedra.CatedraErrorHandler;
import com.example.turnos_service.shared.infrastructure.client.catedra.exception.CatedraApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatedraAppointmentClientAdapterTest {

    private static final String LOGIN = "juan.lopez";
    private static final String PROCESO = "process-1";
    private static final int TAMANIO_PAGINA = 100;
    private static final int MAXIMO_PAGINAS = 50;
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 20);
    private static final Instant CREADA = Instant.parse("2026-11-04T15:16:40Z");

    @Mock
    WebClient webClient;

    @Mock
    WebClient.RequestHeadersUriSpec<?> getUriSpec;

    @Mock
    WebClient.RequestBodyUriSpec postUriSpec;

    @Mock
    WebClient.RequestBodySpec bodySpec;

    @Mock
    WebClient.RequestHeadersSpec<?> headersSpec;

    @Mock
    WebClient.ResponseSpec responseSpec;

    @Captor
    ArgumentCaptor<Function<UriBuilder, URI>> uriCaptor;

    private CatedraAppointmentClientAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CatedraAppointmentClientAdapter(webClient, new CatedraErrorHandler(), new CatedraAppointmentMapper());

        lenient().doReturn(getUriSpec).when(webClient).get();
        lenient().doReturn(headersSpec).when(getUriSpec).uri(ArgumentMatchers.<Function<UriBuilder, URI>>any());
        lenient().when(webClient.post()).thenReturn(postUriSpec);
        lenient().when(postUriSpec.uri(anyString(), any(Object.class))).thenReturn(bodySpec);
        lenient().doReturn(headersSpec).when(bodySpec).bodyValue(any());
        doReturn(responseSpec).when(headersSpec).retrieve();
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    }

    @Test
    void debeConvertirLosDatosDeLaReserva_cuandoLaCatedraLaLista() {
        when(responseSpec.bodyToMono(CatedraAppointmentResponse[].class)).thenReturn(Mono.just(pagina(1)));

        List<CatedraAppointment> resultado = adapter.findByExternalPatientId(LOGIN);

        assertThat(resultado).singleElement()
                .extracting(CatedraAppointment::getReservationId, CatedraAppointment::getReservationProcessId,
                        CatedraAppointment::getExternalPatientId, CatedraAppointment::getAppointmentDate,
                        CatedraAppointment::getProfessionalId, CatedraAppointment::getProfessionalFirstName,
                        CatedraAppointment::getStatus)
                .containsExactly(1502L, PROCESO, LOGIN, FECHA, 202L, "Martin", "CONFIRMED");
    }

    @Test
    void debePedirLaPrimeraPaginaDelPaciente_cuandoListaLasReservas() {
        when(responseSpec.bodyToMono(CatedraAppointmentResponse[].class)).thenReturn(Mono.just(pagina(0)));

        adapter.findByExternalPatientId(LOGIN);

        verify(getUriSpec).uri(uriCaptor.capture());
        assertThat(uriPedida(uriCaptor.getValue()))
                .isEqualTo("/api/appointments?externalPatientId=juan.lopez&page=0&size=100");
    }

    @Test
    void debePedirUnaSolaPagina_cuandoLaPrimeraNoEstaCompleta() {
        when(responseSpec.bodyToMono(CatedraAppointmentResponse[].class)).thenReturn(Mono.just(pagina(TAMANIO_PAGINA - 1)));

        assertThat(adapter.findByExternalPatientId(LOGIN)).hasSize(TAMANIO_PAGINA - 1);

        verify(responseSpec, times(1)).bodyToMono(CatedraAppointmentResponse[].class);
    }

    @Test
    void debeRecorrerLasPaginasYJuntarLasReservas_cuandoLaPrimeraEstaCompleta() {
        when(responseSpec.bodyToMono(CatedraAppointmentResponse[].class))
                .thenReturn(Mono.just(pagina(TAMANIO_PAGINA)), Mono.just(pagina(3)));

        assertThat(adapter.findByExternalPatientId(LOGIN)).hasSize(TAMANIO_PAGINA + 3);

        verify(getUriSpec, times(2)).uri(uriCaptor.capture());
        assertThat(uriPedida(uriCaptor.getAllValues().get(1)))
                .isEqualTo("/api/appointments?externalPatientId=juan.lopez&page=1&size=100");
    }

    @Test
    void debeTerminar_cuandoLaPaginaSiguienteVieneVacia() {
        when(responseSpec.bodyToMono(CatedraAppointmentResponse[].class))
                .thenReturn(Mono.just(pagina(TAMANIO_PAGINA)), Mono.just(pagina(0)));

        assertThat(adapter.findByExternalPatientId(LOGIN)).hasSize(TAMANIO_PAGINA);

        verify(responseSpec, times(2)).bodyToMono(CatedraAppointmentResponse[].class);
    }

    @Test
    void debeCortarEnElMaximoDePaginas_cuandoLaCatedraSiempreDevuelvePaginasCompletas() {
        when(responseSpec.bodyToMono(CatedraAppointmentResponse[].class)).thenReturn(Mono.just(pagina(TAMANIO_PAGINA)));

        assertThat(adapter.findByExternalPatientId(LOGIN)).hasSize(TAMANIO_PAGINA * MAXIMO_PAGINAS);

        verify(responseSpec, times(MAXIMO_PAGINAS)).bodyToMono(CatedraAppointmentResponse[].class);
    }

    @Test
    void debeDevolverListaVacia_cuandoLaCatedraNoDevuelveCuerpo() {
        when(responseSpec.bodyToMono(CatedraAppointmentResponse[].class)).thenReturn(Mono.empty());

        assertThat(adapter.findByExternalPatientId(LOGIN)).isEmpty();
    }

    @Test
    void debeLanzarNoDisponible_cuandoLaCatedraRechazaElListado() {
        when(responseSpec.bodyToMono(CatedraAppointmentResponse[].class))
                .thenReturn(Mono.error(new CatedraApiException(401, null)));

        assertThatThrownBy(() -> adapter.findByExternalPatientId(LOGIN))
                .isInstanceOf(CatedraUnavailableException.class)
                .hasCauseInstanceOf(CatedraApiException.class);
    }

    @Test
    void debeLanzarNoDisponible_cuandoLaCatedraRespondeErrorDelServidorAlListar() {
        when(responseSpec.bodyToMono(CatedraAppointmentResponse[].class))
                .thenReturn(Mono.error(new CatedraApiException(500, null)));

        assertThatThrownBy(() -> adapter.findByExternalPatientId(LOGIN))
                .isInstanceOf(CatedraUnavailableException.class);
    }

    @Test
    void debeLanzarNoDisponible_cuandoLaCatedraNoRespondeAlListar() {
        when(responseSpec.bodyToMono(CatedraAppointmentResponse[].class))
                .thenReturn(Mono.error(sinRespuesta(HttpMethod.GET)));

        assertThatThrownBy(() -> adapter.findByExternalPatientId(LOGIN))
                .isInstanceOf(CatedraUnavailableException.class)
                .hasCauseInstanceOf(WebClientRequestException.class);
    }

    @Test
    void debeEnviarElProcesoYElMotivo_cuandoCancela() {
        when(responseSpec.bodyToMono(CancelAppointmentResponse.class)).thenReturn(Mono.just(cancelada()));

        adapter.cancel(PROCESO, "Cambio de horario");

        verify(postUriSpec).uri("/api/appointments/{reservationProcessId}/cancel", PROCESO);
        verify(bodySpec).bodyValue(new CancelAppointmentRequest("Cambio de horario"));
    }

    @Test
    void debeEnviarElMotivoNulo_cuandoElUsuarioNoIndicaMotivo() {
        when(responseSpec.bodyToMono(CancelAppointmentResponse.class)).thenReturn(Mono.just(cancelada()));

        adapter.cancel(PROCESO, null);

        verify(bodySpec).bodyValue(new CancelAppointmentRequest(null));
    }

    @Test
    void noDebeFallar_cuandoLaCatedraCancelaSinDevolverCuerpo() {
        when(responseSpec.bodyToMono(CancelAppointmentResponse.class)).thenReturn(Mono.empty());

        assertThatCode(() -> adapter.cancel(PROCESO, null)).doesNotThrowAnyException();
    }

    @Test
    void debeLanzarRechazoConCode_cuandoLaCatedraRechazaLaCancelacion() {
        when(responseSpec.bodyToMono(CancelAppointmentResponse.class))
                .thenReturn(Mono.error(new CatedraApiException(409, "RESERVATION_NOT_CANCELLABLE")));

        assertThatThrownBy(() -> adapter.cancel(PROCESO, null))
                .isInstanceOf(CatedraRejectedException.class)
                .extracting("status", "code")
                .containsExactly(409, "RESERVATION_NOT_CANCELLABLE");
    }

    @Test
    void debeLanzarNoDisponible_cuandoLaCatedraRespondeErrorDelServidorAlCancelar() {
        when(responseSpec.bodyToMono(CancelAppointmentResponse.class))
                .thenReturn(Mono.error(new CatedraApiException(503, null)));

        assertThatThrownBy(() -> adapter.cancel(PROCESO, null))
                .isInstanceOf(CatedraUnavailableException.class)
                .hasCauseInstanceOf(CatedraApiException.class);
    }

    @Test
    void debeLanzarNoDisponible_cuandoLaCatedraDevuelveUnaRespuestaInvalidaAlListar() {
        when(responseSpec.bodyToMono(CatedraAppointmentResponse[].class))
                .thenReturn(Mono.error(new DecodingException("respuesta inválida")));

        assertThatThrownBy(() -> adapter.findByExternalPatientId(LOGIN))
                .isInstanceOf(CatedraUnavailableException.class)
                .hasCauseInstanceOf(DecodingException.class);
    }

    @Test
    void debeLanzarNoDisponible_cuandoLaCatedraDevuelveUnaRespuestaInvalidaAlCancelar() {
        when(responseSpec.bodyToMono(CancelAppointmentResponse.class))
                .thenReturn(Mono.error(new DecodingException("respuesta inválida")));

        assertThatThrownBy(() -> adapter.cancel(PROCESO, null))
                .isInstanceOf(CatedraUnavailableException.class)
                .hasCauseInstanceOf(DecodingException.class);
    }

    @Test
    void debeLanzarNoDisponible_cuandoLaCatedraNoRespondeAlCancelar() {
        when(responseSpec.bodyToMono(CancelAppointmentResponse.class))
                .thenReturn(Mono.error(sinRespuesta(HttpMethod.POST)));

        assertThatThrownBy(() -> adapter.cancel(PROCESO, null))
                .isInstanceOf(CatedraUnavailableException.class)
                .hasCauseInstanceOf(WebClientRequestException.class);
    }

    private String uriPedida(Function<UriBuilder, URI> uri) {
        return uri.apply(new DefaultUriBuilderFactory().builder()).toString();
    }

    private CatedraAppointmentResponse[] pagina(int cantidad) {
        return IntStream.range(0, cantidad)
                .mapToObj(i -> new CatedraAppointmentResponse(1502L, PROCESO, LOGIN, FECHA,
                        LocalTime.of(18, 0), LocalTime.of(18, 30), 202L, "Martin", "Rivas", "CONFIRMED",
                        CREADA, CREADA, null, null))
                .toArray(CatedraAppointmentResponse[]::new);
    }

    private CancelAppointmentResponse cancelada() {
        return new CancelAppointmentResponse(1502L, PROCESO, "CANCELLED", CREADA);
    }

    private WebClientRequestException sinRespuesta(HttpMethod metodo) {
        return new WebClientRequestException(new TimeoutException("sin respuesta"), metodo,
                URI.create("http://catedra"), HttpHeaders.EMPTY);
    }
}
