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

    private List<Proyecto> resumenes;
    private Optional<Proyecto> encontrado;
    private Proyecto guardado;
    private Proyecto original;

    @Test
    @DisplayName("Lista sin cargar estructuras ni hacer consultas por proyecto")
    void listaSinCargarEstructurasNiHacerConsultasPorProyecto() {
        dadoQueElNegocioTieneUnProyecto();

        cuandoSeListanLosProyectosDelNegocio();

        entoncesSeListaElProyectoSinConsultarBloquesNiCampos();
    }

    @Test
    @DisplayName("Carga bloques y todos sus campos en lote filtrando por negocio")
    void cargaBloquesYTodosSusCamposEnLoteFiltrandoPorNegocio() {
        dadoUnProyectoConDosBloquesYCamposSoloEnElPrimero();

        cuandoSeBuscaElProyectoPorIdYNegocio();

        entoncesSeCargaLaEstructuraEnLoteFiltrandoPorNegocio();
    }

    @Test
    @DisplayName("No carga hijos cuando el proyecto no pertenece al negocio")
    void noCargaHijosCuandoElProyectoNoPerteneceAlNegocio() {
        dadoQueElProyectoNoEsDelNegocio();

        cuandoSeBuscaElProyectoPorIdYNegocio();

        entoncesNoSeEncuentraElProyectoNiSeCarganHijos();
    }

    @Test
    @DisplayName("Guardar un proyecto conserva campos y no vuelve a insertar sus hijos")
    void guardarUnProyectoConservaCamposYNoVuelveAInsertarSusHijos() {
        dadoQueGuardarDevuelveLaEntidadRecibida();

        cuandoSeGuardaElProyecto();

        entoncesSeGuardaSoloElProyectoSinTocarSusHijos();
    }

    @Test
    @DisplayName("Guarda una copia asignando ids propios y las referencias a sus nuevos padres")
    void guardaUnaCopiaAsignandoIdsPropiosYLasReferenciasASusNuevosPadres() {
        dadoQueLaBaseGeneraLosIds();

        cuandoSeGuardaUnaCopiaConEstructuraDesdeLaPlantilla();

        entoncesLaCopiaTieneIdsPropiosYReferenciasASusNuevosPadres();
    }

    @Test
    @DisplayName("Borra usando tanto el id de proyecto como el id de negocio")
    void borraUsandoTantoElIdDeProyectoComoElIdDeNegocio() {
        cuandoSeBorraElProyecto();

        entoncesSeBorraConElFiltroDeNegocio();
    }

    // --- dado ---
    private void dadoQueElNegocioTieneUnProyecto() {
        when(proyectos.findAllByNegocioIdOrderByCreadoEnDescIdAsc(NEGOCIO_ID.valor()))
                .thenReturn(List.of(mapper.toEntity(ProyectoTestData.proyecto())));
    }

    private void dadoUnProyectoConDosBloquesYCamposSoloEnElPrimero() {
        original = ProyectoTestData.proyecto();
        when(proyectos.findByIdAndNegocioId(PROYECTO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(Optional.of(mapper.toEntity(original)));
        when(bloques.findAllByProyectoIdAndNegocioIdOrderByPosicionAscIdAsc(PROYECTO_ID.valor(), NEGOCIO_ID.valor()))
                .thenReturn(original.getBloques().stream().map(mapper::toEntity).toList());
        when(campos.findAllByBloqueIdInAndNegocioIdOrderByPosicionAscIdAsc(any(), eq(NEGOCIO_ID.valor())))
                .thenReturn(original.getBloques().getFirst().getCampos().stream().map(mapper::toEntity).toList());
    }

    private void dadoQueElProyectoNoEsDelNegocio() {
        when(proyectos.findByIdAndNegocioId(PROYECTO_ID.valor(), NEGOCIO_ID.valor())).thenReturn(Optional.empty());
    }

    private void dadoQueGuardarDevuelveLaEntidadRecibida() {
        when(proyectos.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void dadoQueLaBaseGeneraLosIds() {
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

    // --- cuando ---
    private void cuandoSeListanLosProyectosDelNegocio() {
        resumenes = repository.findAllByNegocioId(NEGOCIO_ID);
    }

    private void cuandoSeBuscaElProyectoPorIdYNegocio() {
        encontrado = repository.findByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID);
    }

    private void cuandoSeGuardaElProyecto() {
        guardado = repository.save(ProyectoTestData.proyecto());
    }

    private void cuandoSeGuardaUnaCopiaConEstructuraDesdeLaPlantilla() {
        guardado = repository.saveConEstructura(
                Proyecto.desdePlantilla(NEGOCIO_ID, ProyectoTestData.plantilla(), AHORA));
    }

    private void cuandoSeBorraElProyecto() {
        repository.deleteByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID);
    }

    // --- entonces ---
    private void entoncesSeListaElProyectoSinConsultarBloquesNiCampos() {
        assertThat(resumenes).extracting("id").containsExactly(PROYECTO_ID);
        verifyNoInteractions(bloques, campos);
    }

    private void entoncesSeCargaLaEstructuraEnLoteFiltrandoPorNegocio() {
        Proyecto result = encontrado.orElseThrow();
        assertThat(result.getBloques()).hasSize(2);
        assertThat(result.getBloques().getFirst().getCampos()).hasSize(2);
        assertThat(result.getBloques().get(1).getCampos()).isEmpty();
        verify(campos).findAllByBloqueIdInAndNegocioIdOrderByPosicionAscIdAsc(
                original.getBloques().stream().map(bloque -> bloque.getId().valor()).toList(), NEGOCIO_ID.valor());
    }

    private void entoncesNoSeEncuentraElProyectoNiSeCarganHijos() {
        assertThat(encontrado).isEmpty();
        verifyNoInteractions(bloques, campos);
    }

    private void entoncesSeGuardaSoloElProyectoSinTocarSusHijos() {
        assertThat(guardado.getId()).isEqualTo(PROYECTO_ID);
        assertThat(guardado.getPlantillaId()).isEqualTo(PLANTILLA_ID);
        assertThat(guardado.getBloques()).hasSize(2);
        verifyNoInteractions(bloques, campos);
    }

    private void entoncesLaCopiaTieneIdsPropiosYReferenciasASusNuevosPadres() {
        assertThat(guardado.getId()).isEqualTo(PROYECTO_ID);
        assertThat(guardado.getBloques()).hasSize(2);
        var bloque = guardado.getBloques().getFirst();
        assertThat(bloque.getId()).isNotNull();
        assertThat(bloque.getProyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(bloque.getCampos()).hasSize(2);
        assertThat(bloque.getCampos().getFirst().getBloqueId()).isEqualTo(bloque.getId());
        assertThat(bloque.getCampos().getFirst().getNegocioId()).isEqualTo(NEGOCIO_ID);
    }

    private void entoncesSeBorraConElFiltroDeNegocio() {
        verify(proyectos).deleteByIdAndNegocioId(PROYECTO_ID.valor(), NEGOCIO_ID.valor());
    }
}
