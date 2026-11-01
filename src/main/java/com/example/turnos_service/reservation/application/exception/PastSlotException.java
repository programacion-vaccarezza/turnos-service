package com.example.turnos_service.reservation.application.exception;

import java.time.LocalDate;
import java.time.LocalTime;

public class PastSlotException extends RuntimeException {

    public PastSlotException(LocalDate date, LocalTime startTime) {
        super("El horario " + date + " " + startTime + " ya pasó");
    }
}
