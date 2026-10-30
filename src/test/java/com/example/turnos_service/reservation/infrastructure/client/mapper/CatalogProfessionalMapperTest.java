package com.example.turnos_service.reservation.infrastructure.client.mapper;

import com.example.turnos_service.reservation.domain.model.ProfessionalInfo;
import com.example.turnos_service.reservation.infrastructure.client.dto.CatalogProfessionalResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogProfessionalMapperTest {

    private final CatalogProfessionalMapper mapper = new CatalogProfessionalMapper();

    @Test
    void debeConvertirADominio_cuandoElProfesionalEstaHabilitado() {
        CatalogProfessionalResponse response = new CatalogProfessionalResponse(201L, 101L, "Ana", "Torres", true);

        ProfessionalInfo professional = mapper.toDomain(response);

        assertThat(professional)
                .extracting("id", "firstName", "lastName", "enabled")
                .containsExactly(201L, "Ana", "Torres", true);
    }

    @Test
    void debeConservarQueEstaDeshabilitado_cuandoElProfesionalNoEstaHabilitado() {
        CatalogProfessionalResponse response = new CatalogProfessionalResponse(202L, 101L, "Luis", "Gomez", false);

        ProfessionalInfo professional = mapper.toDomain(response);

        assertThat(professional.isEnabled()).isFalse();
    }

    @Test
    void debeDevolverNull_cuandoLaRespuestaEsNula() {
        assertThat(mapper.toDomain(null)).isNull();
    }
}
