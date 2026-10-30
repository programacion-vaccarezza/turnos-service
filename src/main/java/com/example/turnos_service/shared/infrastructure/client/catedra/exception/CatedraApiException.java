package com.example.turnos_service.shared.infrastructure.client.catedra.exception;

import lombok.Getter;

/**
 * Error funcional devuelto por la API de la cátedra.
 * Las decisiones se toman por {@code status} y {@code code}, nunca por el texto del detalle (anexo §13).
 * {@code code} puede ser null: los 401/403 de Spring Security de la cátedra pueden no incluirlo.
 */
@Getter
public class CatedraApiException extends RuntimeException {

    private final int status;
    private final String code;

    public CatedraApiException(int status, String code) {
        super("la cátedra respondió " + status + (code != null ? " " + code : ""));
        this.status = status;
        this.code = code;
    }
}
