package com.example.turnos_service.reservation.application.exception;

/**
 * La cátedra todavía no pidió el teléfono (no llegó AdditionalInformationRequested),
 * así que no hay requestEventId con el cual responder (anexo §15.4).
 */
public class PhoneNotRequestedException extends RuntimeException {

    public PhoneNotRequestedException(Long processId) {
        super("La cátedra todavía no pidió el teléfono para el proceso " + processId);
    }
}
