package com.example.turnos_service.reservation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableSlot {

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    public boolean startsAfter(LocalDateTime moment) {
        return LocalDateTime.of(date, startTime).isAfter(moment);
    }
}
