package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.plantilla.Plantilla;
import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.infrastructure.repository.entity.PlantillaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlantillaRepositoryMapperTest {

    private static final UUID ID = UUID.fromString("e0000000-0000-4000-8000-000000000001");
    private static final Map<String, Object> ESTRUCTURA = Map.of("bloques", List.of());
    private static final OffsetDateTime CREADO_EN = OffsetDateTime.parse("2026-01-01T10:00:00Z");
    private static final OffsetDateTime ACTUALIZADO_EN = OffsetDateTime.parse("2026-01-02T10:00:00Z");

    private final PlantillaRepositoryMapper mapper = new PlantillaRepositoryMapper();

    @Test
    @DisplayName("Mapea todos los campos de la entidad al modelo")
    void unaEntidadSeLeeComoPlantillaConTodosSusDatos() {
        PlantillaEntity entidad = dadoUnaEntidadDePlantillaDeClientes();

        Plantilla plantilla = cuandoSeConvierteEnPlantilla(entidad);

        entoncesLaPlantillaTieneTodosLosDatosDeLaEntidad(plantilla);
    }

    // --- dado ---
    private PlantillaEntity dadoUnaEntidadDePlantillaDeClientes() {
        return PlantillaEntity.builder()
                .id(ID)
                .nombre("Clientes")
                .descripcion("Tu agenda de clientes")
                .icono("users")
                .estructura(ESTRUCTURA)
                .activa(true)
                .creadoEn(CREADO_EN)
                .actualizadoEn(ACTUALIZADO_EN)
                .build();
    }

    // --- cuando ---
    private Plantilla cuandoSeConvierteEnPlantilla(PlantillaEntity entidad) {
        return mapper.toDomain(entidad);
    }

    // --- entonces ---
    private void entoncesLaPlantillaTieneTodosLosDatosDeLaEntidad(Plantilla plantilla) {
        assertThat(plantilla.getId()).isEqualTo(PlantillaId.de(ID));
        assertThat(plantilla.getNombre()).isEqualTo("Clientes");
        assertThat(plantilla.getDescripcion()).isEqualTo("Tu agenda de clientes");
        assertThat(plantilla.getIcono()).isEqualTo("users");
        assertThat(plantilla.getEstructura()).isEqualTo(ESTRUCTURA);
        assertThat(plantilla.getActiva()).isTrue();
        assertThat(plantilla.getCreadoEn()).isEqualTo(CREADO_EN);
        assertThat(plantilla.getActualizadoEn()).isEqualTo(ACTUALIZADO_EN);
    }
}
