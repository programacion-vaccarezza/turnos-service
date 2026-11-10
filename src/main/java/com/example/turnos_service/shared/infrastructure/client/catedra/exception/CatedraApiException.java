package com.example.turnos_service.shared.infrastructure.client.catedra.exception;

import lombok.Getter;

// Error devuelto por la API de la cátedra, con su status HTTP y su code
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
