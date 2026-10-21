package com.example.turnos_service.reservation.application.exception;

import lombok.Getter;

/**
 * La cátedra rechazó la operación con un error funcional (4xx).
 * {@code code} es el código estable del anexo §13 (ej. SLOT_ALREADY_HELD, HOLD_EXPIRED); puede ser null.
 */
@Getter
public class CatedraRejectedException extends RuntimeException {

    private final int status;
    private final String code;

    public CatedraRejectedException(int status, String code) {
        super("la cátedra rechazó la operación con " + status + (code != null ? " " + code : ""));
        this.status = status;
        this.code = code;
    }
}
