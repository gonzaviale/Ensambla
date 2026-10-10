package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import com.ensambla.backend.infrastructure.repository.entity.ProyectoEntity;
import com.ensambla.backend.support.ProyectoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProyectoRepositoryMapperTest {

    private final ProyectoRepositoryMapper proyectoRepositoryMapper = new ProyectoRepositoryMapper();

    @Test
    @DisplayName("Mapea todos los campos de proyecto entre entidad y dominio")
    void mappingShouldPreserveProyectoFields() {
        Proyecto original = ProyectoTestData.proyecto();

        ProyectoEntity entity = proyectoRepositoryMapper.toEntity(original);
        Proyecto result = proyectoRepositoryMapper.toDomain(entity);

        assertThat(result.getId()).isEqualTo(original.getId());
        assertThat(result.getNegocioId()).isEqualTo(original.getNegocioId());
        assertThat(result.getNombre()).isEqualTo(original.getNombre());
        assertThat(result.getDescripcion()).isEqualTo(original.getDescripcion());
        assertThat(result.getPlantillaId()).isEqualTo(original.getPlantillaId());
        assertThat(result.getCreadoEn()).isEqualTo(original.getCreadoEn());
        assertThat(result.getActualizadoEn()).isEqualTo(original.getActualizadoEn());
    }

    @Test
    @DisplayName("Mapea todos los campos de bloque entre entidad y dominio")
    void mappingShouldPreserveBloqueFields() {
        Bloque original = ProyectoTestData.proyecto().getBloques().getFirst();

        BloqueEntity entity = proyectoRepositoryMapper.toEntity(original);
        Bloque result = proyectoRepositoryMapper.toDomain(entity);

        assertThat(result.getId()).isEqualTo(original.getId());
        assertThat(result.getNegocioId()).isEqualTo(original.getNegocioId());
        assertThat(result.getProyectoId()).isEqualTo(original.getProyectoId());
        assertThat(result.getNombre()).isEqualTo(original.getNombre());
        assertThat(result.getPosicion()).isEqualTo(original.getPosicion());
        assertThat(result.getCreadoEn()).isEqualTo(original.getCreadoEn());
        assertThat(result.getActualizadoEn()).isEqualTo(original.getActualizadoEn());
    }

    @Test
    @DisplayName("Mapea todos los campos de campo entre entidad y dominio")
    void mappingShouldPreserveCampoFields() {
        Campo original = ProyectoTestData.proyecto().getBloques().getFirst().getCampos().getFirst();

        CampoEntity entity = proyectoRepositoryMapper.toEntity(original);
        Campo result = proyectoRepositoryMapper.toDomain(entity);

        assertThat(result.getId()).isEqualTo(original.getId());
        assertThat(result.getNegocioId()).isEqualTo(original.getNegocioId());
        assertThat(result.getBloqueId()).isEqualTo(original.getBloqueId());
        assertThat(result.getNombre()).isEqualTo(original.getNombre());
        assertThat(result.getTipo()).isEqualTo(original.getTipo());
        assertThat(result.getObligatorio()).isEqualTo(original.getObligatorio());
        assertThat(result.getValorInicial()).isEqualTo(original.getValorInicial());
        assertThat(result.getOpciones()).isEqualTo(original.getOpciones());
        assertThat(result.getPosicion()).isEqualTo(original.getPosicion());
        assertThat(result.getCreadoEn()).isEqualTo(original.getCreadoEn());
        assertThat(result.getActualizadoEn()).isEqualTo(original.getActualizadoEn());
    }
}

