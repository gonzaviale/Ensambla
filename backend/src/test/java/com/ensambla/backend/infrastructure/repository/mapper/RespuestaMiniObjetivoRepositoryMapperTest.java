package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.objetivo.HizoManual;
import com.ensambla.backend.domain.model.objetivo.QuePaso;
import com.ensambla.backend.domain.model.objetivo.RespuestaMiniObjetivo;
import com.ensambla.backend.infrastructure.repository.entity.RespuestaMiniObjetivoEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.RespuestaMiniObjetivoTestData.COMENTARIO;
import static com.ensambla.backend.support.RespuestaMiniObjetivoTestData.RESPUESTA_ID;
import static com.ensambla.backend.support.RespuestaMiniObjetivoTestData.SEMANA_RESPUESTA;
import static com.ensambla.backend.support.RespuestaMiniObjetivoTestData.respuesta;
import static org.assertj.core.api.Assertions.assertThat;

class RespuestaMiniObjetivoRepositoryMapperTest {

    private final RespuestaMiniObjetivoRepositoryMapper mapper = new RespuestaMiniObjetivoRepositoryMapper();
    private RespuestaMiniObjetivoEntity entidad;
    private RespuestaMiniObjetivo dominio;

    @Test
    @DisplayName("Mapea el dominio a la entidad con todos sus campos")
    void mapeaElDominioALaEntidadConTodosSusCampos() {
        cuandoSePasaLaRespuestaALaEntidad();

        entoncesLaEntidadTraeTodosLosCampos();
    }

    @Test
    @DisplayName("Mapea la entidad al dominio con todos sus campos")
    void mapeaLaEntidadAlDominioConTodosSusCampos() {
        dadoLaEntidadDeLaRespuesta();

        cuandoSeLeeLaEntidad();

        entoncesElDominioTraeTodosLosCampos();
    }

    @Test
    @DisplayName("Una entidad sin ids se mapea con ids nulos")
    void unaEntidadSinIdsSeMapeaConIdsNulos() {
        entidad = RespuestaMiniObjetivoEntity.builder().build();

        cuandoSeLeeLaEntidad();

        entoncesElDominioNoTieneIds();
    }

    // --- arrange ---
    private void dadoLaEntidadDeLaRespuesta() {
        entidad = mapper.toEntity(respuesta());
    }

    // --- act ---
    private void cuandoSePasaLaRespuestaALaEntidad() {
        entidad = mapper.toEntity(respuesta());
    }

    private void cuandoSeLeeLaEntidad() {
        dominio = mapper.toDomain(entidad);
    }

    // --- assert ---
    private void entoncesLaEntidadTraeTodosLosCampos() {
        assertThat(entidad.getId()).isEqualTo(RESPUESTA_ID.valor());
        assertThat(entidad.getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(entidad.getObjetivoId()).isEqualTo(OBJETIVO_ID.valor());
        assertThat(entidad.getMiniObjetivoId()).isEqualTo(OTRO_MINI_ID.valor());
        assertThat(entidad.getHizo()).isEqualTo(HizoManual.EN_PARTE);
        assertThat(entidad.getQuePaso()).isEqualTo(QuePaso.NO_TUVE_TIEMPO);
        assertThat(entidad.getComentario()).isEqualTo(COMENTARIO);
        assertThat(entidad.getSemanaInicio()).isEqualTo(SEMANA_RESPUESTA);
        assertThat(entidad.getRespondidaEn()).isEqualTo(AHORA);
    }

    private void entoncesElDominioTraeTodosLosCampos() {
        assertThat(dominio.id()).isEqualTo(RESPUESTA_ID);
        assertThat(dominio.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(dominio.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(dominio.miniObjetivoId()).isEqualTo(OTRO_MINI_ID);
        assertThat(dominio.hizo()).isEqualTo(HizoManual.EN_PARTE);
        assertThat(dominio.quePaso()).isEqualTo(QuePaso.NO_TUVE_TIEMPO);
        assertThat(dominio.comentario()).isEqualTo(COMENTARIO);
        assertThat(dominio.semanaInicio()).isEqualTo(SEMANA_RESPUESTA);
        assertThat(dominio.respondidaEn()).isEqualTo(AHORA);
    }

    private void entoncesElDominioNoTieneIds() {
        assertThat(dominio.id()).isNull();
        assertThat(dominio.miniObjetivoId()).isNull();
    }
}
