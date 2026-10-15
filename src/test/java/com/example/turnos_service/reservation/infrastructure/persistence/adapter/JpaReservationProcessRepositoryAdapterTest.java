package com.example.turnos_service.reservation.infrastructure.persistence.adapter;

import com.example.turnos_service.reservation.domain.model.ReservationProcess;
import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import com.example.turnos_service.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import com.example.turnos_service.reservation.infrastructure.persistence.mapper.ReservationProcessMapper;
import com.example.turnos_service.reservation.infrastructure.persistence.repository.JpaReservationProcessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaReservationProcessRepositoryAdapterTest {

    @Mock
    JpaReservationProcessRepository jpaRepository;

    @Captor
    ArgumentCaptor<ReservationProcessEntity> entityCaptor;

    private JpaReservationProcessRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaReservationProcessRepositoryAdapter(jpaRepository, new ReservationProcessMapper());
    }

    @Test
    void debeGuardarYDevolverElProcesoConId_cuandoSeGuardaUnProcesoNuevo() {
        ReservationProcess nuevo = procesoIniciado(null);
        when(jpaRepository.save(any(ReservationProcessEntity.class)))
                .thenReturn(entidadIniciada(1L));

        ReservationProcess guardado = adapter.save(nuevo);

        verify(jpaRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue())
                .extracting("id", "ownerLogin", "status")
                .containsExactly(null, "juan.perez", ReservationStatus.INITIATED);
        assertThat(guardado)
                .extracting("id", "ownerLogin", "status")
                .containsExactly(1L, "juan.perez", ReservationStatus.INITIATED);
    }

    @Test
    void debeDevolverElProceso_cuandoExisteElId() {
        when(jpaRepository.findById(1L)).thenReturn(Optional.of(entidadIniciada(1L)));

        Optional<ReservationProcess> resultado = adapter.findById(1L);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(1L);
        assertThat(resultado.get().getOwnerLogin()).isEqualTo("juan.perez");
    }

    @Test
    void debeDevolverVacio_cuandoNoExisteElId() {
        when(jpaRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<ReservationProcess> resultado = adapter.findById(99L);

        assertThat(resultado).isEmpty();
    }

    @Test
    void debeDevolverElProceso_cuandoExisteElReservationProcessIdDeLaCatedra() {
        ReservationProcessEntity entidad = entidadIniciada(1L);
        entidad.setReservationProcessId("process-a92b47d0");
        when(jpaRepository.findByReservationProcessId("process-a92b47d0")).thenReturn(Optional.of(entidad));

        Optional<ReservationProcess> resultado = adapter.findByReservationProcessId("process-a92b47d0");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getReservationProcessId()).isEqualTo("process-a92b47d0");
    }

    @Test
    void debeDevolverVacio_cuandoNoExisteElReservationProcessIdDeLaCatedra() {
        when(jpaRepository.findByReservationProcessId("process-inexistente")).thenReturn(Optional.empty());

        Optional<ReservationProcess> resultado = adapter.findByReservationProcessId("process-inexistente");

        assertThat(resultado).isEmpty();
    }

    private ReservationProcess procesoIniciado(Long id) {
        return ReservationProcess.builder()
                .id(id)
                .ownerLogin("juan.perez")
                .professionalId(15L)
                .appointmentDate(LocalDate.of(2026, 9, 14))
                .startTime(LocalTime.of(9, 30))
                .patientFirstName("Juan")
                .patientLastName("Perez")
                .status(ReservationStatus.INITIATED)
                .build();
    }

    private ReservationProcessEntity entidadIniciada(Long id) {
        return ReservationProcessEntity.builder()
                .id(id)
                .ownerLogin("juan.perez")
                .professionalId(15L)
                .appointmentDate(LocalDate.of(2026, 9, 14))
                .startTime(LocalTime.of(9, 30))
                .patientFirstName("Juan")
                .patientLastName("Perez")
                .status(ReservationStatus.INITIATED)
                .version(0L)
                .build();
    }
}
