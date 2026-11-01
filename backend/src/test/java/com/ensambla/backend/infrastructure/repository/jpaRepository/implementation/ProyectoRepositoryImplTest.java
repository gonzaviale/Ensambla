package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import com.ensambla.backend.infrastructure.repository.entity.ProyectoEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IBloqueJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICampoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IProyectoJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.ProyectoRepositoryMapper;
import com.ensambla.backend.support.ProyectoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PLANTILLA_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProyectoRepositoryImplTest {

    private final IProyectoJpaRepository proyectos = mock(IProyectoJpaRepository.class);
    private final IBloqueJpaRepository bloques = mock(IBloqueJpaRepository.class);
    private final ICampoJpaRepository campos = mock(ICampoJpaRepository.class);
    private final ProyectoRepositoryMapper mapper = new ProyectoRepositoryMapper();
    private final ProyectoRepositoryImpl repository = new ProyectoRepositoryImpl(proyectos, bloques, campos, mapper);

    @Test
    @DisplayName("Lista sin cargar estructuras ni hacer consultas por proyecto")
    void findAllShouldMapSummaries() {
        when(proyectos.findAllByNegocioIdOrderByCreadoEnDescIdAsc(NEGOCIO_ID.valor()))
                .thenReturn(List.of(mapper.toEntity(ProyectoTestData.proyecto())));

        var result = repository.findAllByNegocioId(NEGOCIO_ID);

        assertThat(result).extracting("id").containsExactly(PROYECTO_ID);
        verifyNoInteractions(bloques, campos);
    }

    @Test
    @DisplayName("Carga bloques y todos sus campos en lote filtrando por negocio")
    void findByIdShouldLoadStructureInBatches() {
        Proyecto original = ProyectoTestData.proyecto();
        when(proyectos.findByIdAndNegocioId(PROYECTO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(Optional.of(mapper.toEntity(original)));
        when(bloques.findAllByProyectoIdAndNegocioIdOrderByPosicionAscIdAsc(PROYECTO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(original.getBloques().stream().map(mapper::toEntity).toList());
        when(campos.findAllByBloqueIdInAndNegocioIdOrderByPosicionAscIdAsc(any(), eq(NEGOCIO_ID.valor())))
                .thenReturn(original.getBloques().getFirst().getCampos().stream().map(mapper::toEntity).toList());

        var result = repository.findByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID).orElseThrow();

        assertThat(result.getBloques()).hasSize(2);
        assertThat(result.getBloques().getFirst().getCampos()).hasSize(2);
        assertThat(result.getBloques().get(1).getCampos()).isEmpty();
        verify(campos).findAllByBloqueIdInAndNegocioIdOrderByPosicionAscIdAsc(
                original.getBloques().stream().map(bloque -> bloque.getId().valor()).toList(), NEGOCIO_ID.valor());
    }

    @Test
    @DisplayName("No carga hijos cuando el proyecto no pertenece al negocio")
    void findByIdShouldNotLoadForeignStructure() {
        when(proyectos.findByIdAndNegocioId(PROYECTO_ID.valor(), NEGOCIO_ID.valor())).thenReturn(Optional.empty());

        var result = repository.findByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID);

        assertThat(result).isEmpty();
        verifyNoInteractions(bloques, campos);
    }

    @Test
    @DisplayName("Guardar un proyecto conserva campos y no vuelve a insertar sus hijos")
    void saveShouldOnlyPersistProyecto() {
        when(proyectos.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = repository.save(ProyectoTestData.proyecto());

        assertThat(result.getId()).isEqualTo(PROYECTO_ID);
        assertThat(result.getPlantillaId()).isEqualTo(PLANTILLA_ID);
        assertThat(result.getBloques()).hasSize(2);
        verifyNoInteractions(bloques, campos);
    }

    @Test
    @DisplayName("Guarda una copia asignando ids propios y las referencias a sus nuevos padres")
    void saveConEstructuraShouldAssignParentIds() {
        givenGeneratedIds();

        var result = repository.saveConEstructura(Proyecto.desdePlantilla(NEGOCIO_ID, ProyectoTestData.plantilla(), AHORA));

        assertThat(result.getId()).isEqualTo(PROYECTO_ID);
        assertThat(result.getBloques()).hasSize(2);
        var bloque = result.getBloques().getFirst();
        assertThat(bloque.getId()).isNotNull();
        assertThat(bloque.getProyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(bloque.getCampos()).hasSize(2);
        assertThat(bloque.getCampos().getFirst().getBloqueId()).isEqualTo(bloque.getId());
        assertThat(bloque.getCampos().getFirst().getNegocioId()).isEqualTo(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Borra usando tanto el id de proyecto como el id de negocio")
    void deleteShouldKeepNegocioFilter() {
        repository.deleteByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID);

        verify(proyectos).deleteByIdAndNegocioId(PROYECTO_ID.valor(), NEGOCIO_ID.valor());
    }

    // --- arrange ---
    private void givenGeneratedIds() {
        when(proyectos.save(any())).thenAnswer(invocation -> {
            ProyectoEntity entity = invocation.getArgument(0);
            entity.setId(PROYECTO_ID.valor());
            return entity;
        });
        when(bloques.save(any())).thenAnswer(invocation -> {
            BloqueEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });
        when(campos.save(any())).thenAnswer(invocation -> {
            CampoEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });
    }
}

