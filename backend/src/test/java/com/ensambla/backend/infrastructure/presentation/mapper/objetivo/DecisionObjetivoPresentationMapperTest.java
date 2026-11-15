package com.ensambla.backend.infrastructure.presentation.mapper.objetivo;

import com.ensambla.backend.domain.model.objetivo.DecisionPaso;
import com.ensambla.backend.domain.model.objetivo.EfectoDecision;
import com.ensambla.backend.domain.model.objetivo.EstadoEfecto;
import com.ensambla.backend.domain.model.objetivo.TipoDecision;
import com.ensambla.backend.domain.useCase.objetivo.DecisionItem;
import com.ensambla.backend.domain.useCase.objetivo.listarDecisiones.ListarDecisionesRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarDecisiones.ListarDecisionesResponse;
import com.ensambla.backend.domain.useCase.objetivo.listarDecisiones.ListarDecisionesResponse.SemanaDecisiones;
import com.ensambla.backend.domain.useCase.objetivo.registrarDecisionPaso.RegistrarDecisionPasoRequest;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.DecidirPasoWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.DecisionObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.DecisionesObjetivoResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.ensambla.backend.support.DecisionTestData.AVISO_ID;
import static com.ensambla.backend.support.DecisionTestData.DECISION_ID;
import static com.ensambla.backend.support.DecisionTestData.SEMANA;
import static com.ensambla.backend.support.DecisionTestData.TITULO_AVISO;
import static com.ensambla.backend.support.DecisionTestData.decision;
import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class DecisionObjetivoPresentationMapperTest {

    private final DecisionObjetivoPresentationMapper mapper = new DecisionObjetivoPresentationMapper();

    @Test
    @DisplayName("Arma el pedido de decidir un paso con el usuario, el objetivo, el mini-objetivo y la respuesta")
    void toRegistrarRequestShouldCarryEverything() {
        RegistrarDecisionPasoRequest result = mapper.toRegistrarRequest(
                USUARIO_ID, OBJETIVO_ID, MINI_ID, new DecidirPasoWebRequest(DecisionPaso.IGNORADO));

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(result.miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(result.decision()).isEqualTo(DecisionPaso.IGNORADO);
    }

    @Test
    @DisplayName("Arma el pedido de listar con el usuario de la sesión y el objetivo de la ruta")
    void toListarRequestShouldCarryTheUserAndTheObjetivo() {
        ListarDecisionesRequest result = mapper.toListarRequest(USUARIO_ID, OBJETIVO_ID);

        assertThat(result.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(result.objetivoId()).isEqualTo(OBJETIVO_ID);
    }

    @Test
    @DisplayName("Copia todos los datos de la decisión")
    void toResponseShouldCopyEveryFieldOfTheDecision() {
        DecisionItem item = DecisionItem.de(decision(TipoDecision.AVISO_APLICADO, AVISO_ID),
                new EfectoDecision(EstadoEfecto.MEDIDO, new BigDecimal("350.00")));

        DecisionObjetivoResponse result = mapper.toResponse(item);

        assertThat(result.id()).isEqualTo(DECISION_ID);
        assertThat(result.tipo()).isEqualTo(TipoDecision.AVISO_APLICADO);
        assertThat(result.miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(result.avisoId()).isEqualTo(AVISO_ID);
        assertThat(result.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(result.descripcion()).isEqualTo(TITULO_AVISO);
        assertThat(result.tomadaEn()).isEqualTo(AHORA);
        assertThat(result.semanaInicio()).isEqualTo(SEMANA);
        assertThat(result.efecto()).isEqualTo(EstadoEfecto.MEDIDO);
        assertThat(result.deltaIngresoSemanal()).isEqualByComparingTo("350.00");
    }

    @Test
    @DisplayName("Conserva las semanas y el orden de las decisiones")
    void toResponseShouldKeepTheWeeksAndTheirOrder() {
        DecisionItem item = DecisionItem.de(decision(TipoDecision.PASO_ACEPTADO, null), EfectoDecision.SIN_MEDICION);

        DecisionesObjetivoResponse result = mapper.toResponse(
                new ListarDecisionesResponse(List.of(new SemanaDecisiones(SEMANA, List.of(item, item)))));

        assertThat(result.semanas()).singleElement().satisfies(semana -> {
            assertThat(semana.semanaInicio()).isEqualTo(SEMANA);
            assertThat(semana.decisiones()).hasSize(2);
        });
    }

    @Test
    @DisplayName("Sin decisiones devuelve una lista vacía")
    void toResponseShouldReturnEmptyWeeks() {
        DecisionesObjetivoResponse result = mapper.toResponse(new ListarDecisionesResponse(List.of()));

        assertThat(result.semanas()).isEmpty();
    }
}
