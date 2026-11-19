package com.example.turnos_service.reservation.application.exception;

import java.time.LocalDate;
import java.time.LocalTime;

// Se pidió un turno que ya empezó
public class PastSlotException extends RuntimeException {

    public PastSlotException(LocalDate date, LocalTime startTime) {
        super("El horario " + date + " " + startTime + " ya pasó");
    }
}
