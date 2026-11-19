package com.example.turnos_service.reservation.application.exception;

import com.example.turnos_service.reservation.domain.model.ReservationStatus;
import lombok.Getter;

@Getter
public class ReservationRejectedException extends RuntimeException {

    private final Long processId;
    private final ReservationStatus status;
    private final String code;

    public ReservationRejectedException(Long processId, ReservationStatus status, String code) {
        super("La reserva " + processId + " terminó en " + status + (code != null ? " (" + code + ")" : ""));
        this.processId = processId;
        this.status = status;
        this.code = code;
    }
}
