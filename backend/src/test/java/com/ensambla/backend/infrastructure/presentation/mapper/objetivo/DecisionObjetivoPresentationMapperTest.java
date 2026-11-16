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
    private RegistrarDecisionPasoRequest pedidoDeRegistrar;
    private ListarDecisionesRequest pedidoDeListar;
    private DecisionObjetivoResponse respuestaDeLaDecision;
    private DecisionesObjetivoResponse respuestaDeLasSemanas;

    @Test
    @DisplayName("Arma el pedido de decidir un paso con el usuario, el objetivo, el mini-objetivo y la respuesta")
    void toRegistrarRequestShouldCarryEverything() {
        whenSeArmaElPedidoDeRegistrar();

        thenElPedidoDeRegistrarTraeTodo();
    }

    @Test
    @DisplayName("Arma el pedido de listar con el usuario de la sesión y el objetivo de la ruta")
    void toListarRequestShouldCarryTheUserAndTheObjetivo() {
        whenSeArmaElPedidoDeListar();

        thenElPedidoDeListarTraeUsuarioYObjetivo();
    }

    @Test
    @DisplayName("Copia todos los datos de la decisión")
    void toResponseShouldCopyEveryFieldOfTheDecision() {
        whenSeMapeaUnaDecisionMedida();

        thenSeCopianTodosLosDatosDeLaDecision();
    }

    @Test
    @DisplayName("Conserva las semanas y el orden de las decisiones")
    void toResponseShouldKeepTheWeeksAndTheirOrder() {
        whenSeMapeanLasSemanas(unaSemanaConDosDecisiones());

        thenSeConservaLaSemanaConSusDosDecisiones();
    }

    @Test
    @DisplayName("Sin decisiones devuelve una lista vacía")
    void toResponseShouldReturnEmptyWeeks() {
        whenSeMapeanLasSemanas(new ListarDecisionesResponse(List.of()));

        thenNoHaySemanas();
    }

    // --- act ---
    private void whenSeArmaElPedidoDeRegistrar() {
        pedidoDeRegistrar = mapper.toRegistrarRequest(
                USUARIO_ID, OBJETIVO_ID, MINI_ID, new DecidirPasoWebRequest(DecisionPaso.IGNORADO));
    }

    private void whenSeArmaElPedidoDeListar() {
        pedidoDeListar = mapper.toListarRequest(USUARIO_ID, OBJETIVO_ID);
    }

    private void whenSeMapeaUnaDecisionMedida() {
        respuestaDeLaDecision = mapper.toResponse(DecisionItem.de(decision(TipoDecision.AVISO_APLICADO, AVISO_ID),
                new EfectoDecision(EstadoEfecto.MEDIDO, new BigDecimal("350.00"))));
    }

    private void whenSeMapeanLasSemanas(ListarDecisionesResponse semanas) {
        respuestaDeLasSemanas = mapper.toResponse(semanas);
    }

    // --- assert ---
    private void thenElPedidoDeRegistrarTraeTodo() {
        assertThat(pedidoDeRegistrar.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(pedidoDeRegistrar.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(pedidoDeRegistrar.miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(pedidoDeRegistrar.decision()).isEqualTo(DecisionPaso.IGNORADO);
    }

    private void thenElPedidoDeListarTraeUsuarioYObjetivo() {
        assertThat(pedidoDeListar.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(pedidoDeListar.objetivoId()).isEqualTo(OBJETIVO_ID);
    }

    private void thenSeCopianTodosLosDatosDeLaDecision() {
        assertThat(respuestaDeLaDecision.id()).isEqualTo(DECISION_ID);
        assertThat(respuestaDeLaDecision.tipo()).isEqualTo(TipoDecision.AVISO_APLICADO);
        assertThat(respuestaDeLaDecision.miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(respuestaDeLaDecision.avisoId()).isEqualTo(AVISO_ID);
        assertThat(respuestaDeLaDecision.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(respuestaDeLaDecision.descripcion()).isEqualTo(TITULO_AVISO);
        assertThat(respuestaDeLaDecision.tomadaEn()).isEqualTo(AHORA);
        assertThat(respuestaDeLaDecision.semanaInicio()).isEqualTo(SEMANA);
        assertThat(respuestaDeLaDecision.efecto()).isEqualTo(EstadoEfecto.MEDIDO);
        assertThat(respuestaDeLaDecision.deltaIngresoSemanal()).isEqualByComparingTo("350.00");
    }

    private void thenSeConservaLaSemanaConSusDosDecisiones() {
        assertThat(respuestaDeLasSemanas.semanas()).singleElement().satisfies(semana -> {
            assertThat(semana.semanaInicio()).isEqualTo(SEMANA);
            assertThat(semana.decisiones()).hasSize(2);
        });
    }

    private void thenNoHaySemanas() {
        assertThat(respuestaDeLasSemanas.semanas()).isEmpty();
    }

    // --- helpers ---
    private static ListarDecisionesResponse unaSemanaConDosDecisiones() {
        DecisionItem item = DecisionItem.de(decision(TipoDecision.PASO_ACEPTADO, null), EfectoDecision.SIN_MEDICION);
        return new ListarDecisionesResponse(List.of(new SemanaDecisiones(SEMANA, List.of(item, item))));
    }
}
