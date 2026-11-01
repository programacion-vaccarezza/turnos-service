package com.example.turnos_service.reservation.application.usecases;

import com.example.turnos_service.reservation.application.exception.ProfessionalDisabledException;
import com.example.turnos_service.reservation.application.exception.ProfessionalNotFoundException;
import com.example.turnos_service.reservation.domain.model.AvailableSlot;
import com.example.turnos_service.reservation.domain.model.Occupancy;
import com.example.turnos_service.reservation.domain.model.ProfessionalInfo;
import com.example.turnos_service.reservation.domain.ports.in.GetAvailabilityUseCase;
import com.example.turnos_service.reservation.domain.ports.out.CatalogProfessionalPort;
import com.example.turnos_service.reservation.domain.ports.out.CatalogSchedulePort;
import com.example.turnos_service.reservation.domain.ports.out.CatedraOccupancyPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Calcula los turnos disponibles de un profesional en una fecha (enunciado §7): genera los turnos del día con sus
 * horarios vigentes y descarta los que ya empezaron y los ocupados. Sin ocupaciones no se responde disponibilidad:
 * si la cátedra no está disponible, el error se propaga.
 */
@Component
@RequiredArgsConstructor
public class GetAvailabilityUseCaseImpl implements GetAvailabilityUseCase {

    private final CatalogProfessionalPort catalogProfessionalPort;
    private final CatalogSchedulePort catalogSchedulePort;
    private final CatedraOccupancyPort catedraOccupancyPort;

    @Override
    public List<AvailableSlot> getAvailability(Long professionalId, LocalDate date) {
        ProfessionalInfo professional = catalogProfessionalPort.findById(professionalId)
                .orElseThrow(() -> new ProfessionalNotFoundException(professionalId));
        if (!professional.isEnabled()) {
            throw new ProfessionalDisabledException(professionalId);
        }

        LocalDateTime now = LocalDateTime.now();
        List<AvailableSlot> slots = catalogSchedulePort.findEnabledByProfessionalId(professionalId).stream()
                .flatMap(schedule -> schedule.slotsOn(date).stream())
                .filter(slot -> slot.startsAfter(now))
                .toList();
        if (slots.isEmpty()) {
            return List.of();
        }

        Set<LocalTime> occupied = catedraOccupancyPort.findByProfessionalAndDate(professionalId, date).stream()
                .map(Occupancy::getStartTime)
                .collect(Collectors.toSet());
        // TreeMap: ordena por hora y, si dos horarios se superponen, deja un solo turno por hora de inicio.
        TreeMap<LocalTime, AvailableSlot> available = slots.stream()
                .filter(slot -> !occupied.contains(slot.getStartTime()))
                .collect(Collectors.toMap(AvailableSlot::getStartTime, Function.identity(), (first, second) -> first, TreeMap::new));
        return new ArrayList<>(available.values());
    }
}
