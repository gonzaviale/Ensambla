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
    void unaDecisionGuardadaSePasaAlDominioConTodosSusDatos() {
        DecisionObjetivoEntity entidad = dadoUnaEntidadDeDecisionCompleta();

        DecisionObjetivo decision = cuandoSePasaAlDominio(entidad);

        entoncesLaDecisionTieneTodosLosDatosDeLaEntidadCompleta(decision);
    }

    @Test
    @DisplayName("Una entidad sin mini-objetivo, aviso ni producto se mapea con esos ids nulos")
    void unaDecisionGuardadaSinMiniObjetivoAvisoNiProductoSePasaConEsosIdsNulos() {
        DecisionObjetivoEntity entidad = dadoUnaEntidadDeDecisionSinReferencias();

        DecisionObjetivo decision = cuandoSePasaAlDominio(entidad);

        entoncesLaDecisionNoTieneMiniObjetivoAvisoNiProducto(decision);
    }

    @Test
    @DisplayName("Mapea el dominio a la entidad con todos sus campos")
    void unaDecisionDelDominioSePasaALaEntidadConTodosSusDatos() {
        DecisionObjetivo decision = dadoUnAvisoDescartado();

        DecisionObjetivoEntity entidad = cuandoSePasaALaEntidad(decision);

        entoncesLaEntidadTieneTodosLosDatosDelAvisoDescartado(entidad);
    }

    @Test
    @DisplayName("Un dominio sin mini-objetivo ni aviso se mapea con esos UUID nulos")
    void unaDecisionDelDominioSinAvisoSePasaALaEntidadConElAvisoNulo() {
        DecisionObjetivo decision = dadoUnPasoAceptadoSinAviso();

        DecisionObjetivoEntity entidad = cuandoSePasaALaEntidad(decision);

        entoncesLaEntidadNoTieneAviso(entidad);
    }

    // --- dado ---
    private DecisionObjetivoEntity dadoUnaEntidadDeDecisionCompleta() {
        return DecisionObjetivoEntity.builder()
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
    }

    private DecisionObjetivoEntity dadoUnaEntidadDeDecisionSinReferencias() {
        return DecisionObjetivoEntity.builder()
                .id(DECISION_ID.valor())
                .negocioId(NEGOCIO_ID.valor())
                .objetivoId(OBJETIVO_ID.valor())
                .tipo(TipoDecision.PASO_IGNORADO)
                .descripcion("Paso")
                .tomadaEn(AHORA)
                .semanaInicio(SEMANA)
                .build();
    }

    private DecisionObjetivo dadoUnAvisoDescartado() {
        return decision(TipoDecision.AVISO_DESCARTADO, AVISO_ID);
    }

    private DecisionObjetivo dadoUnPasoAceptadoSinAviso() {
        return decision(TipoDecision.PASO_ACEPTADO, null);
    }

    // --- cuando ---
    private DecisionObjetivo cuandoSePasaAlDominio(DecisionObjetivoEntity entidad) {
        return mapper.toDomain(entidad);
    }

    private DecisionObjetivoEntity cuandoSePasaALaEntidad(DecisionObjetivo decision) {
        return mapper.toEntity(decision);
    }

    // --- entonces ---
    private void entoncesLaDecisionTieneTodosLosDatosDeLaEntidadCompleta(DecisionObjetivo decision) {
        assertThat(decision.id()).isEqualTo(DECISION_ID);
        assertThat(decision.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(decision.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(decision.miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(decision.tipo()).isEqualTo(TipoDecision.AVISO_APLICADO);
        assertThat(decision.avisoId()).isEqualTo(AVISO_ID);
        assertThat(decision.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(decision.descripcion()).isEqualTo(TITULO_AVISO);
        assertThat(decision.tomadaEn()).isEqualTo(AHORA);
        assertThat(decision.semanaInicio()).isEqualTo(SEMANA);
    }

    private void entoncesLaDecisionNoTieneMiniObjetivoAvisoNiProducto(DecisionObjetivo decision) {
        assertThat(decision.miniObjetivoId()).isNull();
        assertThat(decision.avisoId()).isNull();
        assertThat(decision.registroId()).isNull();
    }

    private void entoncesLaEntidadTieneTodosLosDatosDelAvisoDescartado(DecisionObjetivoEntity entidad) {
        assertThat(entidad.getId()).isEqualTo(DECISION_ID.valor());
        assertThat(entidad.getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(entidad.getObjetivoId()).isEqualTo(OBJETIVO_ID.valor());
        assertThat(entidad.getMiniObjetivoId()).isEqualTo(MINI_ID.valor());
        assertThat(entidad.getTipo()).isEqualTo(TipoDecision.AVISO_DESCARTADO);
        assertThat(entidad.getAvisoId()).isEqualTo(AVISO_ID.valor());
        assertThat(entidad.getRegistroId()).isEqualTo(PRODUCTO_ID.valor());
        assertThat(entidad.getDescripcion()).isEqualTo(TITULO_AVISO);
        assertThat(entidad.getTomadaEn()).isEqualTo(AHORA);
        assertThat(entidad.getSemanaInicio()).isEqualTo(SEMANA);
    }

    private void entoncesLaEntidadNoTieneAviso(DecisionObjetivoEntity entidad) {
        assertThat(entidad.getAvisoId()).isNull();
    }
}
