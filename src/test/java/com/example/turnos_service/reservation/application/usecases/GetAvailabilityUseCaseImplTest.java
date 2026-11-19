package com.example.turnos_service.reservation.application.usecases;

import com.example.turnos_service.reservation.application.exception.CatalogUnavailableException;
import com.example.turnos_service.reservation.application.exception.CatedraUnavailableException;
import com.example.turnos_service.reservation.application.exception.ProfessionalDisabledException;
import com.example.turnos_service.reservation.application.exception.ProfessionalNotFoundException;
import com.example.turnos_service.reservation.domain.model.AvailableSlot;
import com.example.turnos_service.reservation.domain.model.Occupancy;
import com.example.turnos_service.reservation.domain.model.ProfessionalInfo;
import com.example.turnos_service.reservation.domain.model.WeeklySchedule;
import com.example.turnos_service.reservation.domain.ports.out.CatalogProfessionalPort;
import com.example.turnos_service.reservation.domain.ports.out.CatalogSchedulePort;
import com.example.turnos_service.reservation.domain.ports.out.CatedraOccupancyPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAvailabilityUseCaseImplTest {

    private static final Long PROFESIONAL = 202L;
    private static final LocalDate FECHA = LocalDate.now().plusDays(7);

    @Mock
    CatalogProfessionalPort catalogProfessionalPort;

    @Mock
    CatalogSchedulePort catalogSchedulePort;

    @Mock
    CatedraOccupancyPort catedraOccupancyPort;

    @InjectMocks
    GetAvailabilityUseCaseImpl useCase;

    @Test
    void debeDevolverLosTurnosDelHorarioMenosLosOcupados_cuandoElProfesionalEstaHabilitado() {
        profesionalHabilitado();
        when(catalogSchedulePort.findEnabledByProfessionalId(PROFESIONAL))
                .thenReturn(List.of(horario(FECHA.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.of(11, 0))));
        when(catedraOccupancyPort.findByProfessionalAndDate(PROFESIONAL, FECHA))
                .thenReturn(List.of(ocupado(LocalTime.of(9, 30)), ocupado(LocalTime.of(10, 30))));

        List<AvailableSlot> turnos = useCase.getAvailability(PROFESIONAL, FECHA);

        assertThat(turnos).extracting(AvailableSlot::getStartTime)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(10, 0));
        assertThat(turnos).allSatisfy(turno -> assertThat(turno.getDate()).isEqualTo(FECHA));
    }

    @Test
    void debeOrdenarPorHoraYSinRepetidos_cuandoDosHorariosSeSuperponen() {
        profesionalHabilitado();
        when(catalogSchedulePort.findEnabledByProfessionalId(PROFESIONAL)).thenReturn(List.of(
                horario(FECHA.getDayOfWeek(), LocalTime.of(10, 0), LocalTime.of(11, 0)),
                horario(FECHA.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.of(10, 30))));
        when(catedraOccupancyPort.findByProfessionalAndDate(PROFESIONAL, FECHA)).thenReturn(List.of());

        List<AvailableSlot> turnos = useCase.getAvailability(PROFESIONAL, FECHA);

        assertThat(turnos).extracting(AvailableSlot::getStartTime)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0), LocalTime.of(10, 30));
    }

    @Test
    void debeIgnorarLosHorariosDeOtrosDias_cuandoArmaLosTurnos() {
        profesionalHabilitado();
        when(catalogSchedulePort.findEnabledByProfessionalId(PROFESIONAL)).thenReturn(List.of(
                horario(FECHA.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.of(10, 0)),
                horario(FECHA.getDayOfWeek().plus(1), LocalTime.of(15, 0), LocalTime.of(16, 0))));
        when(catedraOccupancyPort.findByProfessionalAndDate(PROFESIONAL, FECHA)).thenReturn(List.of());

        assertThat(useCase.getAvailability(PROFESIONAL, FECHA)).extracting(AvailableSlot::getStartTime)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(9, 30));
    }

    @Test
    void noDebeIncluirTurnosQueYaEmpezaron_cuandoLaFechaEsHoy() {
        LocalDate hoy = LocalDate.now();
        profesionalHabilitado();
        when(catalogSchedulePort.findEnabledByProfessionalId(PROFESIONAL))
                .thenReturn(List.of(horario(hoy.getDayOfWeek(), LocalTime.of(0, 0), LocalTime.of(23, 30))));
        lenient().when(catedraOccupancyPort.findByProfessionalAndDate(anyLong(), any())).thenReturn(List.of());
        LocalDateTime antes = LocalDateTime.now();

        List<AvailableSlot> turnos = useCase.getAvailability(PROFESIONAL, hoy);

        assertThat(turnos).allSatisfy(turno -> assertThat(LocalDateTime.of(turno.getDate(), turno.getStartTime())).isAfter(antes));
    }

    @Test
    void debeDevolverListaVaciaSinConsultarALaCatedra_cuandoLaFechaYaPaso() {
        LocalDate ayer = LocalDate.now().minusDays(1);
        profesionalHabilitado();
        when(catalogSchedulePort.findEnabledByProfessionalId(PROFESIONAL))
                .thenReturn(List.of(horario(ayer.getDayOfWeek(), LocalTime.of(0, 0), LocalTime.of(23, 30))));

        assertThat(useCase.getAvailability(PROFESIONAL, ayer)).isEmpty();

        verifyNoInteractions(catedraOccupancyPort);
    }

    @Test
    void debeDevolverListaVaciaSinConsultarALaCatedra_cuandoElProfesionalNoTieneHorarios() {
        profesionalHabilitado();
        when(catalogSchedulePort.findEnabledByProfessionalId(PROFESIONAL)).thenReturn(List.of());

        assertThat(useCase.getAvailability(PROFESIONAL, FECHA)).isEmpty();

        verifyNoInteractions(catedraOccupancyPort);
    }

    @Test
    void debeRechazarSinConsultarHorarios_cuandoElProfesionalNoExiste() {
        when(catalogProfessionalPort.findById(PROFESIONAL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getAvailability(PROFESIONAL, FECHA))
                .isInstanceOf(ProfessionalNotFoundException.class);

        verifyNoInteractions(catalogSchedulePort, catedraOccupancyPort);
    }

    @Test
    void debeRechazarSinConsultarHorarios_cuandoElProfesionalEstaDeshabilitado() {
        when(catalogProfessionalPort.findById(PROFESIONAL))
                .thenReturn(Optional.of(ProfessionalInfo.builder().id(PROFESIONAL).enabled(false).build()));

        assertThatThrownBy(() -> useCase.getAvailability(PROFESIONAL, FECHA))
                .isInstanceOf(ProfessionalDisabledException.class);

        verifyNoInteractions(catalogSchedulePort, catedraOccupancyPort);
    }

    @Test
    void debePropagarElError_cuandoCatalogoNoEstaDisponible() {
        when(catalogProfessionalPort.findById(PROFESIONAL))
                .thenThrow(new CatalogUnavailableException("catalogo caído", null));

        assertThatThrownBy(() -> useCase.getAvailability(PROFESIONAL, FECHA))
                .isInstanceOf(CatalogUnavailableException.class);
    }

    @Test
    void debePropagarElError_cuandoLaCatedraNoDevuelveLasOcupaciones() {
        profesionalHabilitado();
        when(catalogSchedulePort.findEnabledByProfessionalId(PROFESIONAL))
                .thenReturn(List.of(horario(FECHA.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.of(10, 0))));
        when(catedraOccupancyPort.findByProfessionalAndDate(PROFESIONAL, FECHA))
                .thenThrow(new CatedraUnavailableException("cátedra caída", null));

        assertThatThrownBy(() -> useCase.getAvailability(PROFESIONAL, FECHA))
                .isInstanceOf(CatedraUnavailableException.class);
        verify(catedraOccupancyPort).findByProfessionalAndDate(PROFESIONAL, FECHA);
    }

    private void profesionalHabilitado() {
        when(catalogProfessionalPort.findById(PROFESIONAL))
                .thenReturn(Optional.of(ProfessionalInfo.builder().id(PROFESIONAL).enabled(true).build()));
    }

    private static WeeklySchedule horario(DayOfWeek dia, LocalTime inicio, LocalTime fin) {
        return WeeklySchedule.builder().dayOfWeek(dia).startTime(inicio).endTime(fin).slotDurationMinutes(30).build();
    }

    private static Occupancy ocupado(LocalTime inicio) {
        return Occupancy.builder().date(FECHA).startTime(inicio).status("HELD").build();
    }
}
