package com.example.turnos_service.reservation.domain.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;


public enum ReservationStatus {

    INITIATED,
    HOLD_CREATED,
    CONFIRMING,
    WAITING_FOR_PHONE,
    PHONE_SUBMITTED,
    PHONE_REJECTED,
    CONFIRMED,
    CANCELLED,
    EXPIRED,
    INVALID,
    FAILED;

    private static final Set<ReservationStatus> FINAL_STATES = EnumSet.of(CANCELLED, EXPIRED, INVALID, FAILED);

    private static final Map<ReservationStatus, Set<ReservationStatus>> TRANSITIONS = new EnumMap<>(ReservationStatus.class);

    static {
        TRANSITIONS.put(INITIATED, EnumSet.of(HOLD_CREATED, FAILED));
        TRANSITIONS.put(HOLD_CREATED, EnumSet.of(CONFIRMING, EXPIRED, INVALID));
        TRANSITIONS.put(CONFIRMING, EnumSet.of(WAITING_FOR_PHONE, EXPIRED, INVALID, FAILED));
        TRANSITIONS.put(WAITING_FOR_PHONE, EnumSet.of(PHONE_SUBMITTED, PHONE_REJECTED, CONFIRMED, EXPIRED, INVALID));
        TRANSITIONS.put(PHONE_SUBMITTED, EnumSet.of(PHONE_REJECTED, CONFIRMED, EXPIRED, INVALID));
        TRANSITIONS.put(PHONE_REJECTED, EnumSet.of(PHONE_SUBMITTED, CONFIRMED, EXPIRED, INVALID));
        TRANSITIONS.put(CONFIRMED, EnumSet.of(CANCELLED));
        FINAL_STATES.forEach(state -> TRANSITIONS.put(state, EnumSet.noneOf(ReservationStatus.class)));
    }

    public boolean isFinal() {
        return FINAL_STATES.contains(this);
    }

    public boolean canTransitionTo(ReservationStatus next) {
        return next != null && TRANSITIONS.get(this).contains(next);
    }
}
