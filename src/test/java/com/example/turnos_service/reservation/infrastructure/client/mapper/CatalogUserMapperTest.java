package com.example.turnos_service.reservation.infrastructure.client.mapper;

import com.example.turnos_service.reservation.domain.model.PatientInfo;
import com.example.turnos_service.reservation.infrastructure.client.dto.CatalogUserResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogUserMapperTest {

    private final CatalogUserMapper mapper = new CatalogUserMapper();

    @Test
    void debeConvertirADominio_cuandoLaRespuestaTieneDatos() {
        CatalogUserResponse response = new CatalogUserResponse("juan.perez", "Juan", "Perez");

        PatientInfo patientInfo = mapper.toDomain(response);

        assertThat(patientInfo)
                .extracting("login", "firstName", "lastName")
                .containsExactly("juan.perez", "Juan", "Perez");
    }

    @Test
    void debeDevolverNull_cuandoLaRespuestaEsNula() {
        PatientInfo patientInfo = mapper.toDomain(null);

        assertThat(patientInfo).isNull();
    }
}
