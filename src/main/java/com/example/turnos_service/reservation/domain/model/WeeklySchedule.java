package com.example.turnos_service.reservation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

// Horario semanal de atención de un profesional
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklySchedule {

    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private int slotDurationMinutes;

    // Divide el horario en turnos para la fecha, si coincide el día de la semana
    public List<AvailableSlot> slotsOn(LocalDate date) {
        List<AvailableSlot> slots = new ArrayList<>();
        if (date.getDayOfWeek() != dayOfWeek || slotDurationMinutes <= 0) {
            return slots;
        }
        LocalTime start = startTime;
        LocalTime end = start.plusMinutes(slotDurationMinutes);
        while (end.isAfter(start) && !end.isAfter(endTime)) {
            slots.add(new AvailableSlot(date, start, end));
            start = end;
            end = start.plusMinutes(slotDurationMinutes);
        }
        return slots;
    }
}
