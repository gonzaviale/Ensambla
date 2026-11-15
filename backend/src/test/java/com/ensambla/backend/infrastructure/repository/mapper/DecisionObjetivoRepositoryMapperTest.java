package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.objetivo.DecisionObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoDecision;
import com.ensambla.backend.infrastructure.repository.entity.DecisionObjetivoEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.DecisionTestData.AVISO_ID;
import static com.ensambla.backend.support.DecisionTestData.DECISION_ID;
import static com.ensambla.backend.support.DecisionTestData.SEMANA;
import static com.ensambla.backend.support.DecisionTestData.TITULO_AVISO;
import static com.ensambla.backend.support.DecisionTestData.decision;
import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class DecisionObjetivoRepositoryMapperTest {

    private final DecisionObjetivoRepositoryMapper mapper = new DecisionObjetivoRepositoryMapper();

    @Test
    @DisplayName("Mapea la entidad al dominio con todos sus campos")
    void toDomainShouldMapEveryField() {
        DecisionObjetivoEntity entity = DecisionObjetivoEntity.builder()
                .id(DECISION_ID.valor())
                .negocioId(NEGOCIO_ID.valor())
                .objetivoId(OBJETIVO_ID.valor())
                .miniObjetivoId(MINI_ID.valor())
                .tipo(TipoDecision.AVISO_APLICADO)
                .avisoId(AVISO_ID.valor())
                .registroId(PRODUCTO_ID.valor())
                .descripcion(TITULO_AVISO)
                .tomadaEn(AHORA)
                .semanaInicio(SEMANA)
                .build();

        DecisionObjetivo result = mapper.toDomain(entity);

        assertThat(result.id()).isEqualTo(DECISION_ID);
        assertThat(result.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(result.miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(result.tipo()).isEqualTo(TipoDecision.AVISO_APLICADO);
        assertThat(result.avisoId()).isEqualTo(AVISO_ID);
        assertThat(result.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(result.descripcion()).isEqualTo(TITULO_AVISO);
        assertThat(result.tomadaEn()).isEqualTo(AHORA);
        assertThat(result.semanaInicio()).isEqualTo(SEMANA);
    }

    @Test
    @DisplayName("Una entidad sin mini-objetivo, aviso ni producto se mapea con esos ids nulos")
    void toDomainShouldKeepOptionalIdsNull() {
        DecisionObjetivoEntity entity = DecisionObjetivoEntity.builder()
                .id(DECISION_ID.valor())
                .negocioId(NEGOCIO_ID.valor())
                .objetivoId(OBJETIVO_ID.valor())
                .tipo(TipoDecision.PASO_IGNORADO)
                .descripcion("Paso")
                .tomadaEn(AHORA)
                .semanaInicio(SEMANA)
                .build();

        DecisionObjetivo result = mapper.toDomain(entity);

        assertThat(result.miniObjetivoId()).isNull();
        assertThat(result.avisoId()).isNull();
        assertThat(result.registroId()).isNull();
    }

    @Test
    @DisplayName("Mapea el dominio a la entidad con todos sus campos")
    void toEntityShouldMapEveryField() {
        DecisionObjetivoEntity result = mapper.toEntity(decision(TipoDecision.AVISO_DESCARTADO, AVISO_ID));

        assertThat(result.getId()).isEqualTo(DECISION_ID.valor());
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(result.getObjetivoId()).isEqualTo(OBJETIVO_ID.valor());
        assertThat(result.getMiniObjetivoId()).isEqualTo(MINI_ID.valor());
        assertThat(result.getTipo()).isEqualTo(TipoDecision.AVISO_DESCARTADO);
        assertThat(result.getAvisoId()).isEqualTo(AVISO_ID.valor());
        assertThat(result.getRegistroId()).isEqualTo(PRODUCTO_ID.valor());
        assertThat(result.getDescripcion()).isEqualTo(TITULO_AVISO);
        assertThat(result.getTomadaEn()).isEqualTo(AHORA);
        assertThat(result.getSemanaInicio()).isEqualTo(SEMANA);
    }

    @Test
    @DisplayName("Un dominio sin mini-objetivo ni aviso se mapea con esos UUID nulos")
    void toEntityShouldKeepOptionalIdsNull() {
        DecisionObjetivoEntity result = mapper.toEntity(decision(TipoDecision.PASO_ACEPTADO, null));

        assertThat(result.getAvisoId()).isNull();
    }
}
