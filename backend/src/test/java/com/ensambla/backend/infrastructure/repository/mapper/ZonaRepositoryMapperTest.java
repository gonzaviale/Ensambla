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

    private final ZonaRepositoryMapper zonaRepositoryMapper = new ZonaRepositoryMapper();

    @Test
    @DisplayName("Mapea centro, radio y nombre de la entidad al dominio")
    void toDomainShouldMapEveryField() {
        ZonaEntity entity = ZonaEntity.builder().id(UUID.randomUUID()).nombre("San Justo - UNLaM")
                .localidad("San Justo").partido("La Matanza").provincia("Buenos Aires")
                .latitud(new BigDecimal("-34.670300")).longitud(new BigDecimal("-58.562800"))
                .radioMetros(1500).build();

        Zona result = zonaRepositoryMapper.toDomain(entity);

        assertThat(result.getId()).isEqualTo(ZonaId.de(entity.getId()));
        assertThat(result.getNombre()).isEqualTo("San Justo - UNLaM");
        assertThat(result.getLatitud()).isEqualByComparingTo("-34.670300");
        assertThat(result.getLongitud()).isEqualByComparingTo("-58.562800");
        assertThat(result.getRadioMetros()).isEqualTo(1500);
    }
}
