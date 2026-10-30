package com.example.turnos_service.reservation.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitPhoneRequest {

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 30, message = "El teléfono debe tener como máximo 30 caracteres")
    private String phoneNumber;
}
