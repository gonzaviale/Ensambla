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

    @Test
    @DisplayName("Mapea el dominio a la entidad con todos sus campos")
    void toEntityShouldMapEveryField() {
        RespuestaMiniObjetivoEntity result = mapper.toEntity(respuesta());

        assertThat(result.getId()).isEqualTo(RESPUESTA_ID.valor());
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(result.getObjetivoId()).isEqualTo(OBJETIVO_ID.valor());
        assertThat(result.getMiniObjetivoId()).isEqualTo(OTRO_MINI_ID.valor());
        assertThat(result.getHizo()).isEqualTo(HizoManual.EN_PARTE);
        assertThat(result.getQuePaso()).isEqualTo(QuePaso.NO_TUVE_TIEMPO);
        assertThat(result.getComentario()).isEqualTo(COMENTARIO);
        assertThat(result.getSemanaInicio()).isEqualTo(SEMANA_RESPUESTA);
        assertThat(result.getRespondidaEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Mapea la entidad al dominio con todos sus campos")
    void toDomainShouldMapEveryField() {
        RespuestaMiniObjetivo result = mapper.toDomain(mapper.toEntity(respuesta()));

        assertThat(result.id()).isEqualTo(RESPUESTA_ID);
        assertThat(result.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(result.miniObjetivoId()).isEqualTo(OTRO_MINI_ID);
        assertThat(result.hizo()).isEqualTo(HizoManual.EN_PARTE);
        assertThat(result.quePaso()).isEqualTo(QuePaso.NO_TUVE_TIEMPO);
        assertThat(result.comentario()).isEqualTo(COMENTARIO);
        assertThat(result.semanaInicio()).isEqualTo(SEMANA_RESPUESTA);
        assertThat(result.respondidaEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Una entidad sin ids se mapea con ids nulos")
    void toDomainShouldKeepMissingIdsNull() {
        RespuestaMiniObjetivo result = mapper.toDomain(RespuestaMiniObjetivoEntity.builder().build());

        assertThat(result.id()).isNull();
        assertThat(result.miniObjetivoId()).isNull();
    }
}
