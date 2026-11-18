package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.TipoObjetivo;
import com.ensambla.backend.infrastructure.repository.entity.ObjetivoEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.META;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.TEXTO;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static com.ensambla.backend.support.ObjetivoTestData.objetivoSinMeta;
import static org.assertj.core.api.Assertions.assertThat;

class ObjetivoRepositoryMapperTest {

    private final ObjetivoRepositoryMapper mapper = new ObjetivoRepositoryMapper();
    private ObjetivoEntity entidad;
    private Objetivo dominio;

    @Test
    @DisplayName("Mapea la entidad al dominio con todos sus campos")
    void mapeaLaEntidadAlDominioConTodosSusCampos() {
        dadoUnaEntidadCompleta();

        cuandoSeLeeLaEntidad();

        entoncesElDominioTraeTodosLosCampos();
    }

    @Test
    @DisplayName("Una entidad sin ids se mapea con ids nulos")
    void unaEntidadSinIdsSeMapeaConIdsNulos() {
        entidad = ObjetivoEntity.builder().build();

        cuandoSeLeeLaEntidad();

        entoncesElDominioNoTieneIds();
    }

    @Test
    @DisplayName("Mapea el dominio a la entidad con todos sus campos")
    void mapeaElDominioALaEntidadConTodosSusCampos() {
        dadoUnObjetivoAlcanzado();

        cuandoSePasaElObjetivoALaEntidad();

        entoncesLaEntidadTraeTodosLosCampos();
    }

    @Test
    @DisplayName("Un objetivo armado con Sami se mapea a la entidad sin meta")
    void unObjetivoArmadoConSamiSeMapeaALaEntidadSinMeta() {
        dominio = objetivoSinMeta();

        cuandoSePasaElObjetivoALaEntidad();

        entoncesLaEntidadNoTieneMetaYLoArmoSami();
    }

    @Test
    @DisplayName("Un objetivo nuevo, sin id, se mapea con id nulo para que la base lo asigne")
    void unObjetivoNuevoSinIdSeMapeaConIdNuloParaQueLaBaseLoAsigne() {
        dominio = Objetivo.nuevo(NEGOCIO_ID, TEXTO, META, AHORA);

        cuandoSePasaElObjetivoALaEntidad();

        entoncesLaEntidadNoTieneId();
    }

    // --- arrange ---
    private void dadoUnaEntidadCompleta() {
        entidad = ObjetivoEntity.builder()
                .id(OBJETIVO_ID.valor())
                .negocioId(NEGOCIO_ID.valor())
                .texto(TEXTO)
                .tipo(TipoObjetivo.HIBRIDO)
                .meta(META)
                .armadoConSami(true)
                .creadoEn(AHORA)
                .alcanzadoEn(AHORA.plusDays(3))
                .build();
    }

    private void dadoUnObjetivoAlcanzado() {
        dominio = objetivo();
        dominio.marcarAlcanzado(AHORA.plusDays(3));
    }

    // --- act ---
    private void cuandoSeLeeLaEntidad() {
        dominio = mapper.toDomain(entidad);
    }

    private void cuandoSePasaElObjetivoALaEntidad() {
        entidad = mapper.toEntity(dominio);
    }

    // --- assert ---
    private void entoncesElDominioTraeTodosLosCampos() {
        assertThat(dominio.getId()).isEqualTo(OBJETIVO_ID);
        assertThat(dominio.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(dominio.getTexto()).isEqualTo(TEXTO);
        assertThat(dominio.getTipo()).isEqualTo(TipoObjetivo.HIBRIDO);
        assertThat(dominio.getMeta()).isEqualTo(META);
        assertThat(dominio.isArmadoConSami()).isTrue();
        assertThat(dominio.getCreadoEn()).isEqualTo(AHORA);
        assertThat(dominio.getAlcanzadoEn()).isEqualTo(AHORA.plusDays(3));
    }

    private void entoncesElDominioNoTieneIds() {
        assertThat(dominio.getId()).isNull();
        assertThat(dominio.getNegocioId()).isNull();
    }

    private void entoncesLaEntidadTraeTodosLosCampos() {
        assertThat(entidad.getAlcanzadoEn()).isEqualTo(AHORA.plusDays(3));
        assertThat(entidad.getId()).isEqualTo(OBJETIVO_ID.valor());
        assertThat(entidad.getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(entidad.getTexto()).isEqualTo(TEXTO);
        assertThat(entidad.getTipo()).isEqualTo(TipoObjetivo.MONETARIO);
        assertThat(entidad.getMeta()).isEqualTo(META);
        assertThat(entidad.isArmadoConSami()).isFalse();
        assertThat(entidad.getCreadoEn()).isEqualTo(AHORA);
    }

    private void entoncesLaEntidadNoTieneMetaYLoArmoSami() {
        assertThat(entidad.getTipo()).isEqualTo(TipoObjetivo.SIN_DATOS);
        assertThat(entidad.getMeta()).isNull();
        assertThat(entidad.isArmadoConSami()).isTrue();
    }

    private void entoncesLaEntidadNoTieneId() {
        assertThat(entidad.getId()).isNull();
    }
}
