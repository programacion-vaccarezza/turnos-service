package com.example.turnos_service.reservation.domain.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
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

    /**
     * Camino más corto de transiciones válidas hasta {@code target}, sin incluir el estado actual.
     * Vacío si ya está en {@code target} o si no se puede llegar (por ejemplo, desde un estado final).
     */
    public List<ReservationStatus> pathTo(ReservationStatus target) {
        Map<ReservationStatus, ReservationStatus> previous = new EnumMap<>(ReservationStatus.class);
        Deque<ReservationStatus> pending = new ArrayDeque<>(List.of(this));
        while (!pending.isEmpty()) {
            ReservationStatus current = pending.poll();
            for (ReservationStatus next : TRANSITIONS.get(current)) {
                if (next == this || previous.containsKey(next)) {
                    continue;
                }
                previous.put(next, current);
                if (next == target) {
                    return buildPath(previous, target);
                }
                pending.add(next);
            }
        }
        return List.of();
    }

    private List<ReservationStatus> buildPath(Map<ReservationStatus, ReservationStatus> previous, ReservationStatus target) {
        List<ReservationStatus> path = new ArrayList<>();
        for (ReservationStatus step = target; step != this; step = previous.get(step)) {
            path.add(step);
        }
        Collections.reverse(path);
        return path;
    }
}
