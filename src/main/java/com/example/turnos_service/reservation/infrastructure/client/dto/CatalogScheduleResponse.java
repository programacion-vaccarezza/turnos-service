package com.example.turnos_service.reservation.infrastructure.client.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record CatalogScheduleResponse(Long id, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                                      int slotDurationMinutes, boolean enabled) {
}
