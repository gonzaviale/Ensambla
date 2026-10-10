package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.plantilla.Plantilla;
import com.ensambla.backend.infrastructure.repository.entity.PlantillaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlantillaRepositoryMapperTest {

    private static final UUID ID = UUID.randomUUID();
    private static final Map<String, Object> ESTRUCTURA = Map.of("bloques", List.of());
    private static final OffsetDateTime CREADO_EN = OffsetDateTime.parse("2026-01-01T10:00:00Z");
    private static final OffsetDateTime ACTUALIZADO_EN = OffsetDateTime.parse("2026-01-02T10:00:00Z");

    private PlantillaRepositoryMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PlantillaRepositoryMapper();
    }

    @Test
    @DisplayName("Mapea todos los campos de la entidad al modelo")
    void toDomainShouldMapEveryEntityField() {
        PlantillaEntity entity = PlantillaEntity.builder()
                .id(ID)
                .nombre("Clientes")
                .descripcion("Tu agenda de clientes")
                .icono("users")
                .estructura(ESTRUCTURA)
                .activa(true)
                .creadoEn(CREADO_EN)
                .actualizadoEn(ACTUALIZADO_EN)
                .build();

        Plantilla result = mapper.toDomain(entity);

        assertThat(result.getId()).isEqualTo(ID);
        assertThat(result.getNombre()).isEqualTo("Clientes");
        assertThat(result.getDescripcion()).isEqualTo("Tu agenda de clientes");
        assertThat(result.getIcono()).isEqualTo("users");
        assertThat(result.getEstructura()).isEqualTo(ESTRUCTURA);
        assertThat(result.getActiva()).isTrue();
        assertThat(result.getCreadoEn()).isEqualTo(CREADO_EN);
        assertThat(result.getActualizadoEn()).isEqualTo(ACTUALIZADO_EN);
    }
}
