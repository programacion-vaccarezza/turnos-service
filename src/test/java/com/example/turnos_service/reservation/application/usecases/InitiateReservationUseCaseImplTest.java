package com.example.turnos_service.reservation.application.usecases;

import com.example.turnos_service.reservation.application.exception.CatalogUnavailableException;
import com.example.turnos_service.reservation.application.exception.CatedraRejectedException;
import com.example.turnos_service.reservation.application.exception.CatedraUnavailableException;
import com.example.turnos_service.reservation.application.exception.PatientNotFoundException;
import com.example.turnos_service.reservation.application.exception.ProfessionalDisabledException;
import com.example.turnos_service.reservation.application.exception.ProfessionalNotFoundException;
import com.example.turnos_service.reservation.application.exception.ReservationRejectedException;
import com.example.turnos_service.reservation.domain.model.Hold;
import com.example.turnos_service.reservation.domain.model.HoldConfirmation;
import com.example.turnos_service.reservation.domain.model.PatientInfo;
import com.example.turnos_service.reservation.domain.model.ProfessionalInfo;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.ports.out.CatalogProfessionalPort;
import com.example.turnos_service.reservation.domain.ports.out.CatalogUserPort;
import com.example.turnos_service.reservation.domain.ports.out.CatedraHoldPort;
import com.example.turnos_service.reservation.domain.ports.out.ReservationProcessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.example.turnos_service.reservation.domain.model.ReservationStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitiateReservationUseCaseImplTest {

    private static final String LOGIN = "juan.lopez";
    private static final Long PROFESIONAL = 201L;
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 28);
    private static final LocalTime HORA = LocalTime.of(17, 30);
    private static final Instant VENCE_HOLD = Instant.parse("2026-10-24T19:22:00Z");
    private static final Instant VENCE_CONFIRM = Instant.parse("2026-10-24T19:22:19Z");

    @Mock
    CatalogProfessionalPort catalogProfessionalPort;

    @Mock
    CatalogUserPort catalogUserPort;

    @Mock
    ReservationProcessRepository reservationProcessRepository;

    @Mock
    CatedraHoldPort catedraHoldPort;

    @InjectMocks
    InitiateReservationUseCaseImpl useCase;

    /** Estado del proceso en cada guardado, en orden: permite verificar el recorrido por la máquina de estados. */
    private final List<ReservationStatus> estadosGuardados = new ArrayList<>();

    @BeforeEach
    void setUp() {
        lenient().when(reservationProcessRepository.save(any(ReservationProcess.class))).thenAnswer(invocation -> {
            ReservationProcess process = invocation.getArgument(0);
            if (process.getId() == null) {
                process.setId(1L);
            }
            estadosGuardados.add(process.getStatus());
            return process;
        });
    }

    // ---------- camino feliz ----------

    @Test
    void debeRecorrerLaMaquinaDeEstadosHastaEsperarElTelefono_cuandoTodoSaleBien() {
        datosDeCatalogoValidos();
        holdCreado();
        when(catedraHoldPort.confirmHold("hold-1", LOGIN, "Juan", "Lopez"))
                .thenReturn(new HoldConfirmation("process-1", VENCE_CONFIRM));

        ReservationProcess resultado = useCase.initiateReservation(LOGIN, PROFESIONAL, FECHA, HORA);

        assertThat(estadosGuardados).containsExactly(INITIATED, HOLD_CREATED, CONFIRMING, WAITING_FOR_PHONE);
        assertThat(resultado.getStatus()).isEqualTo(WAITING_FOR_PHONE);
        assertThat(resultado.getHoldId()).isEqualTo("hold-1");
        assertThat(resultado.getReservationProcessId()).isEqualTo("process-1");
        assertThat(resultado.getExpiresAt()).isEqualTo(VENCE_CONFIRM);
        assertThat(resultado.getFailureReason()).isNull();
    }

    @Test
    void debeAsociarElProcesoAlUsuarioYUsarSuLoginComoPaciente_cuandoIniciaLaReserva() {
        datosDeCatalogoValidos();
        holdCreado();
        when(catedraHoldPort.confirmHold("hold-1", LOGIN, "Juan", "Lopez"))
                .thenReturn(new HoldConfirmation("process-1", VENCE_CONFIRM));

        ReservationProcess resultado = useCase.initiateReservation(LOGIN, PROFESIONAL, FECHA, HORA);

        assertThat(resultado.getOwnerLogin()).isEqualTo(LOGIN);
        assertThat(resultado.getPatientFirstName()).isEqualTo("Juan");
        assertThat(resultado.getPatientLastName()).isEqualTo("Lopez");
        verify(catedraHoldPort).createHold(PROFESIONAL, FECHA, HORA, LOGIN);
        verify(catedraHoldPort).confirmHold("hold-1", LOGIN, "Juan", "Lopez");
    }

    // ---------- validaciones previas: no se guarda nada ni se llama a la cátedra ----------

    @Test
    void debeRechazarSinTocarLaCatedra_cuandoElProfesionalNoExiste() {
        when(catalogProfessionalPort.findById(PROFESIONAL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.initiateReservation(LOGIN, PROFESIONAL, FECHA, HORA))
                .isInstanceOf(ProfessionalNotFoundException.class);
        verifyNoInteractions(catalogUserPort, reservationProcessRepository, catedraHoldPort);
    }

    @Test
    void debeRechazarSinTocarLaCatedra_cuandoElProfesionalEstaDeshabilitado() {
        when(catalogProfessionalPort.findById(PROFESIONAL))
                .thenReturn(Optional.of(new ProfessionalInfo(PROFESIONAL, "Ana", "Torres", false)));

        assertThatThrownBy(() -> useCase.initiateReservation(LOGIN, PROFESIONAL, FECHA, HORA))
                .isInstanceOf(ProfessionalDisabledException.class);
        verifyNoInteractions(catalogUserPort, reservationProcessRepository, catedraHoldPort);
    }

    @Test
    void debeRechazarSinTocarLaCatedra_cuandoElUsuarioNoExisteEnCatalogo() {
        when(catalogProfessionalPort.findById(PROFESIONAL))
                .thenReturn(Optional.of(new ProfessionalInfo(PROFESIONAL, "Ana", "Torres", true)));
        when(catalogUserPort.findByLogin(LOGIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.initiateReservation(LOGIN, PROFESIONAL, FECHA, HORA))
                .isInstanceOf(PatientNotFoundException.class);
        verifyNoInteractions(reservationProcessRepository, catedraHoldPort);
    }

    @Test
    void debeNoIniciarLaReserva_cuandoCatalogoNoEstaDisponible() {
        when(catalogProfessionalPort.findById(PROFESIONAL))
                .thenThrow(new CatalogUnavailableException("catalogo caído", null));

        assertThatThrownBy(() -> useCase.initiateReservation(LOGIN, PROFESIONAL, FECHA, HORA))
                .isInstanceOf(CatalogUnavailableException.class);
        verifyNoInteractions(reservationProcessRepository, catedraHoldPort);
    }

    // ---------- creación del hold ----------

    @Test
    void debeFallarElProcesoConElCodigo_cuandoLaCatedraRechazaElHold() {
        datosDeCatalogoValidos();
        when(catedraHoldPort.createHold(PROFESIONAL, FECHA, HORA, LOGIN))
                .thenThrow(new CatedraRejectedException(409, "SLOT_ALREADY_HELD"));

        assertThatThrownBy(() -> useCase.initiateReservation(LOGIN, PROFESIONAL, FECHA, HORA))
                .isInstanceOf(ReservationRejectedException.class)
                .extracting("status", "code")
                .containsExactly(FAILED, "SLOT_ALREADY_HELD");
        assertThat(estadosGuardados).containsExactly(INITIATED, FAILED);
        verify(catedraHoldPort, never()).confirmHold(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void debeFallarElProcesoYPropagarElError_cuandoLaCatedraNoRespondeAlCrearElHold() {
        datosDeCatalogoValidos();
        when(catedraHoldPort.createHold(PROFESIONAL, FECHA, HORA, LOGIN))
                .thenThrow(new CatedraUnavailableException("sin respuesta", null));

        assertThatThrownBy(() -> useCase.initiateReservation(LOGIN, PROFESIONAL, FECHA, HORA))
                .isInstanceOf(CatedraUnavailableException.class);
        assertThat(estadosGuardados).containsExactly(INITIATED, FAILED);
        verify(catedraHoldPort, never()).confirmHold(anyString(), anyString(), anyString(), anyString());
    }

    // ---------- confirmación del hold ----------

    @Test
    void debeEsperarElTelefono_cuandoLaCatedraIndicaQueElProcesoYaEstabaConfirmado() {
        datosDeCatalogoValidos();
        holdCreado();
        when(catedraHoldPort.confirmHold("hold-1", LOGIN, "Juan", "Lopez"))
                .thenThrow(new CatedraRejectedException(409, "RESERVATION_PROCESS_ALREADY_CONFIRMED"));

        ReservationProcess resultado = useCase.initiateReservation(LOGIN, PROFESIONAL, FECHA, HORA);

        assertThat(resultado.getStatus()).isEqualTo(WAITING_FOR_PHONE);
        assertThat(estadosGuardados).containsExactly(INITIATED, HOLD_CREATED, CONFIRMING, WAITING_FOR_PHONE);
    }

    @Test
    void debeVencerElProceso_cuandoElHoldVencioAntesDeConfirmar() {
        datosDeCatalogoValidos();
        holdCreado();
        when(catedraHoldPort.confirmHold("hold-1", LOGIN, "Juan", "Lopez"))
                .thenThrow(new CatedraRejectedException(404, "HOLD_EXPIRED"));

        assertThatThrownBy(() -> useCase.initiateReservation(LOGIN, PROFESIONAL, FECHA, HORA))
                .isInstanceOf(ReservationRejectedException.class)
                .extracting("status", "code")
                .containsExactly(EXPIRED, "HOLD_EXPIRED");
        assertThat(estadosGuardados).containsExactly(INITIATED, HOLD_CREATED, CONFIRMING, EXPIRED);
    }

    @Test
    void debeFallarElProceso_cuandoLaCatedraRechazaElConfirmPorOtroMotivo() {
        datosDeCatalogoValidos();
        holdCreado();
        when(catedraHoldPort.confirmHold("hold-1", LOGIN, "Juan", "Lopez"))
                .thenThrow(new CatedraRejectedException(404, "HOLD_NOT_FOUND"));

        assertThatThrownBy(() -> useCase.initiateReservation(LOGIN, PROFESIONAL, FECHA, HORA))
                .isInstanceOf(ReservationRejectedException.class)
                .extracting("status", "code")
                .containsExactly(FAILED, "HOLD_NOT_FOUND");
        assertThat(estadosGuardados).containsExactly(INITIATED, HOLD_CREATED, CONFIRMING, FAILED);
    }

    @Test
    void debeQuedarEnConfirmandoSinError_cuandoElConfirmNoRecibeRespuesta() {
        datosDeCatalogoValidos();
        holdCreado();
        when(catedraHoldPort.confirmHold("hold-1", LOGIN, "Juan", "Lopez"))
                .thenThrow(new CatedraUnavailableException("timeout", null));

        ReservationProcess resultado = useCase.initiateReservation(LOGIN, PROFESIONAL, FECHA, HORA);

        assertThat(resultado.getStatus()).isEqualTo(CONFIRMING);
        assertThat(estadosGuardados).containsExactly(INITIATED, HOLD_CREATED, CONFIRMING);
    }

    // ---------- helpers ----------

    private void datosDeCatalogoValidos() {
        when(catalogProfessionalPort.findById(PROFESIONAL))
                .thenReturn(Optional.of(new ProfessionalInfo(PROFESIONAL, "Ana", "Torres", true)));
        when(catalogUserPort.findByLogin(LOGIN))
                .thenReturn(Optional.of(new PatientInfo(LOGIN, "Juan", "Lopez")));
    }

    private void holdCreado() {
        when(catedraHoldPort.createHold(PROFESIONAL, FECHA, HORA, LOGIN))
                .thenReturn(new Hold("hold-1", "process-1", VENCE_HOLD));
    }
}
