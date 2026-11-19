package com.example.turnos_service.reservation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Nombre y apellido del usuario, obtenidos de catalogo-service
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientInfo {

    private String login;
    private String firstName;
    private String lastName;
}
