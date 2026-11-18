package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.infrastructure.repository.entity.ZonaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ZonaRepositoryMapperTest {

    private static final UUID ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final int RADIO_METROS = 1500;

    private final ZonaRepositoryMapper zonaRepositoryMapper = new ZonaRepositoryMapper();

    @Test
    @DisplayName("Mapea centro, radio y nombre de la entidad al dominio")
    void unaEntidadSeLeeComoZonaConSuCentroSuRadioYSuNombre() {
        ZonaEntity entidad = dadoUnaEntidadDeLaZonaSanJustoUnlam();

        Zona zona = cuandoSeConvierteEnZona(entidad);

        entoncesLaZonaTieneElCentroElRadioYElNombreDeLaEntidad(zona);
    }

    // --- dado ---
    private ZonaEntity dadoUnaEntidadDeLaZonaSanJustoUnlam() {
        return ZonaEntity.builder()
                .id(ID)
                .nombre("San Justo - UNLaM")
                .localidad("San Justo")
                .partido("La Matanza")
                .provincia("Buenos Aires")
                .latitud(new BigDecimal("-34.670300"))
                .longitud(new BigDecimal("-58.562800"))
                .radioMetros(RADIO_METROS)
                .build();
    }

    // --- cuando ---
    private Zona cuandoSeConvierteEnZona(ZonaEntity entidad) {
        return zonaRepositoryMapper.toDomain(entidad);
    }

    // --- entonces ---
    private void entoncesLaZonaTieneElCentroElRadioYElNombreDeLaEntidad(Zona zona) {
        assertThat(zona.getId()).isEqualTo(ZonaId.de(ID));
        assertThat(zona.getNombre()).isEqualTo("San Justo - UNLaM");
        assertThat(zona.getLatitud()).isEqualByComparingTo("-34.670300");
        assertThat(zona.getLongitud()).isEqualByComparingTo("-58.562800");
        assertThat(zona.getRadioMetros()).isEqualTo(RADIO_METROS);
    }
}
