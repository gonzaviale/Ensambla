package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.ProyectoTestData.proyecto;
import static org.assertj.core.api.Assertions.assertThat;

class ProyectoRepositoryMapperTest {

    private final ProyectoRepositoryMapper proyectoRepositoryMapper = new ProyectoRepositoryMapper();

    @Test
    @DisplayName("Mapea todos los campos de proyecto entre entidad y dominio")
    void unProyectoGuardadoYLeidoConservaTodosSusDatos() {
        Proyecto original = dadoUnProyecto();

        Proyecto resultado = cuandoSeGuardaYSeLeeElProyecto(original);

        entoncesElProyectoConservaTodosSusDatos(resultado, original);
    }

    @Test
    @DisplayName("Mapea todos los campos de bloque entre entidad y dominio")
    void unBloqueGuardadoYLeidoConservaTodosSusDatos() {
        Bloque original = dadoUnBloque();

        Bloque resultado = cuandoSeGuardaYSeLeeElBloque(original);

        entoncesElBloqueConservaTodosSusDatos(resultado, original);
    }

    @Test
    @DisplayName("Mapea todos los campos de campo entre entidad y dominio")
    void unCampoGuardadoYLeidoConservaTodosSusDatos() {
        Campo original = dadoUnCampo();

        Campo resultado = cuandoSeGuardaYSeLeeElCampo(original);

        entoncesElCampoConservaTodosSusDatos(resultado, original);
    }

    // --- dado ---
    private Proyecto dadoUnProyecto() {
        return proyecto();
    }

    private Bloque dadoUnBloque() {
        return proyecto().getBloques().getFirst();
    }

    private Campo dadoUnCampo() {
        return dadoUnBloque().getCampos().getFirst();
    }

    // --- cuando ---
    private Proyecto cuandoSeGuardaYSeLeeElProyecto(Proyecto proyecto) {
        return proyectoRepositoryMapper.toDomain(proyectoRepositoryMapper.toEntity(proyecto));
    }

    private Bloque cuandoSeGuardaYSeLeeElBloque(Bloque bloque) {
        return proyectoRepositoryMapper.toDomain(proyectoRepositoryMapper.toEntity(bloque));
    }

    private Campo cuandoSeGuardaYSeLeeElCampo(Campo campo) {
        return proyectoRepositoryMapper.toDomain(proyectoRepositoryMapper.toEntity(campo));
    }

    // --- entonces ---
    private void entoncesElProyectoConservaTodosSusDatos(Proyecto resultado, Proyecto original) {
        assertThat(resultado.getId()).isEqualTo(original.getId());
        assertThat(resultado.getNegocioId()).isEqualTo(original.getNegocioId());
        assertThat(resultado.getNombre()).isEqualTo(original.getNombre());
        assertThat(resultado.getDescripcion()).isEqualTo(original.getDescripcion());
        assertThat(resultado.getPlantillaId()).isEqualTo(original.getPlantillaId());
        assertThat(resultado.getCreadoEn()).isEqualTo(original.getCreadoEn());
        assertThat(resultado.getActualizadoEn()).isEqualTo(original.getActualizadoEn());
    }

    private void entoncesElBloqueConservaTodosSusDatos(Bloque resultado, Bloque original) {
        assertThat(resultado.getId()).isEqualTo(original.getId());
        assertThat(resultado.getNegocioId()).isEqualTo(original.getNegocioId());
        assertThat(resultado.getProyectoId()).isEqualTo(original.getProyectoId());
        assertThat(resultado.getNombre()).isEqualTo(original.getNombre());
        assertThat(resultado.getPosicion()).isEqualTo(original.getPosicion());
        assertThat(resultado.getCreadoEn()).isEqualTo(original.getCreadoEn());
        assertThat(resultado.getActualizadoEn()).isEqualTo(original.getActualizadoEn());
    }

    private void entoncesElCampoConservaTodosSusDatos(Campo resultado, Campo original) {
        assertThat(resultado.getId()).isEqualTo(original.getId());
        assertThat(resultado.getNegocioId()).isEqualTo(original.getNegocioId());
        assertThat(resultado.getBloqueId()).isEqualTo(original.getBloqueId());
        assertThat(resultado.getNombre()).isEqualTo(original.getNombre());
        assertThat(resultado.getTipo()).isEqualTo(original.getTipo());
        assertThat(resultado.getObligatorio()).isEqualTo(original.getObligatorio());
        assertThat(resultado.getValorInicial()).isEqualTo(original.getValorInicial());
        assertThat(resultado.getOpciones()).isEqualTo(original.getOpciones());
        assertThat(resultado.getBloqueRelacionadoId()).isEqualTo(original.getBloqueRelacionadoId());
        assertThat(resultado.getPosicion()).isEqualTo(original.getPosicion());
        assertThat(resultado.getCreadoEn()).isEqualTo(original.getCreadoEn());
        assertThat(resultado.getActualizadoEn()).isEqualTo(original.getActualizadoEn());
    }
}
