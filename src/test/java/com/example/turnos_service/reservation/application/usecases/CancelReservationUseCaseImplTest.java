package com.example.turnos_service.reservation.application.usecases;

import com.example.turnos_service.reservation.application.exception.CatedraRejectedException;
import com.example.turnos_service.reservation.application.exception.CatedraUnavailableException;
import com.example.turnos_service.reservation.application.exception.ConcurrentReservationUpdateException;
import com.example.turnos_service.reservation.application.exception.InvalidStateTransitionException;
import com.example.turnos_service.reservation.application.exception.ReservationNotFoundException;
import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.domain.ports.out.CatedraAppointmentPort;
import com.example.turnos_service.reservation.domain.ports.out.ReservationProcessRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.example.turnos_service.reservation.domain.model.ReservationStatus.CANCELLED;
import static com.example.turnos_service.reservation.domain.model.ReservationStatus.CONFIRMED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelReservationUseCaseImplTest {

    private static final String LOGIN = "juan.lopez";
    private static final Long ID = 57L;
    private static final String PROCESO = "process-1";
    private static final String MOTIVO = "Cambio de horario";

    @Mock
    ReservationProcessRepository reservationProcessRepository;

    @Mock
    CatedraAppointmentPort catedraAppointmentPort;

    @InjectMocks
    CancelReservationUseCaseImpl useCase;

    @Test
    void debeCancelarEnLaCatedraYGuardarCancelled_cuandoLaReservaEstaConfirmada() {
        ReservationProcess process = proceso(CONFIRMED);
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.of(process));
        when(reservationProcessRepository.save(any(ReservationProcess.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationProcess resultado = useCase.cancelReservation(LOGIN, ID, MOTIVO);

        InOrder orden = inOrder(catedraAppointmentPort, reservationProcessRepository);
        orden.verify(catedraAppointmentPort).cancel(PROCESO, MOTIVO);
        orden.verify(reservationProcessRepository).save(process);
        assertThat(resultado.getStatus()).isEqualTo(CANCELLED);
    }

    @Test
    void debeCancelarSinMotivo_cuandoElUsuarioNoLoIndica() {
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.of(proceso(CONFIRMED)));
        when(reservationProcessRepository.save(any(ReservationProcess.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.cancelReservation(LOGIN, ID, null);

        verify(catedraAppointmentPort).cancel(PROCESO, null);
    }

    @Test
    void debeResponderNoEncontrado_cuandoLaReservaEsDeOtroUsuario() {
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.of(proceso(CONFIRMED)));

        assertThatThrownBy(() -> useCase.cancelReservation("maria.prueba", ID, MOTIVO))
                .isInstanceOf(ReservationNotFoundException.class);

        verifyNoInteractions(catedraAppointmentPort);
        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void debeResponderNoEncontrado_cuandoLaReservaNoExiste() {
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.cancelReservation(LOGIN, ID, MOTIVO))
                .isInstanceOf(ReservationNotFoundException.class);

        verifyNoInteractions(catedraAppointmentPort);
    }

    @Test
    void debeDevolverLaReservaSinLlamarALaCatedra_cuandoYaEstabaCancelada() {
        ReservationProcess process = proceso(CANCELLED);
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.of(process));

        ReservationProcess resultado = useCase.cancelReservation(LOGIN, ID, MOTIVO);

        assertThat(resultado).isSameAs(process);
        verifyNoInteractions(catedraAppointmentPort);
        verify(reservationProcessRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"CONFIRMED", "CANCELLED"}, mode = EnumSource.Mode.EXCLUDE)
    void debeRechazarSinLlamarALaCatedra_cuandoElEstadoNoPermiteCancelar(ReservationStatus estado) {
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.of(proceso(estado)));

        assertThatThrownBy(() -> useCase.cancelReservation(LOGIN, ID, MOTIVO))
                .isInstanceOf(InvalidStateTransitionException.class);

        verifyNoInteractions(catedraAppointmentPort);
        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void noDebeCambiarElEstado_cuandoLaCatedraRechazaLaCancelacion() {
        ReservationProcess process = proceso(CONFIRMED);
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.of(process));
        doThrow(new CatedraRejectedException(409, "RESERVATION_NOT_CANCELLABLE"))
                .when(catedraAppointmentPort).cancel(PROCESO, MOTIVO);

        assertThatThrownBy(() -> useCase.cancelReservation(LOGIN, ID, MOTIVO))
                .isInstanceOf(CatedraRejectedException.class);

        assertThat(process.getStatus()).isEqualTo(CONFIRMED);
        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void noDebeCambiarElEstado_cuandoLaCatedraNoEstaDisponible() {
        ReservationProcess process = proceso(CONFIRMED);
        when(reservationProcessRepository.findById(ID)).thenReturn(Optional.of(process));
        doThrow(new CatedraUnavailableException("sin conexión", null))
                .when(catedraAppointmentPort).cancel(PROCESO, MOTIVO);

        assertThatThrownBy(() -> useCase.cancelReservation(LOGIN, ID, MOTIVO))
                .isInstanceOf(CatedraUnavailableException.class);

        assertThat(process.getStatus()).isEqualTo(CONFIRMED);
        verify(reservationProcessRepository, never()).save(any());
    }

    @Test
    void debeDevolverElEstadoQueDejoElEvento_cuandoKafkaActualizoElProcesoAntesDeGuardar() {
        ReservationProcess actualizado = proceso(CANCELLED);
        when(reservationProcessRepository.findById(ID))
                .thenReturn(Optional.of(proceso(CONFIRMED)), Optional.of(actualizado));
        when(reservationProcessRepository.save(any(ReservationProcess.class)))
                .thenThrow(new ConcurrentReservationUpdateException(ID, null));

        ReservationProcess resultado = useCase.cancelReservation(LOGIN, ID, MOTIVO);

        assertThat(resultado).isSameAs(actualizado);
    }

    @Test
    void debePropagarElConflicto_cuandoElProcesoYaNoSePuedeReleer() {
        ConcurrentReservationUpdateException conflicto = new ConcurrentReservationUpdateException(ID, null);
        when(reservationProcessRepository.findById(ID))
                .thenReturn(Optional.of(proceso(CONFIRMED)), Optional.empty());
        when(reservationProcessRepository.save(any(ReservationProcess.class))).thenThrow(conflicto);

        assertThatThrownBy(() -> useCase.cancelReservation(LOGIN, ID, MOTIVO)).isSameAs(conflicto);
    }

    private static ReservationProcess proceso(ReservationStatus estado) {
        return ReservationProcess.builder()
                .id(ID).ownerLogin(LOGIN).reservationProcessId(PROCESO).status(estado).build();
    }
}
