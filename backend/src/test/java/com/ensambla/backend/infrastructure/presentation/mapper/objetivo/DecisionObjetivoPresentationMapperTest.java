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
    private DecisionesObjetivoResponse decisiones;

    @Test
    @DisplayName("Arma el pedido de decidir un paso con el usuario, el objetivo, el mini-objetivo y la respuesta")
    void armaElPedidoDeDecidirUnPaso() {
        entoncesElPedidoEs(mapper.toRegistrarRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID,
                        new DecidirPasoWebRequest(DecisionPaso.IGNORADO)),
                new RegistrarDecisionPasoRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID, DecisionPaso.IGNORADO));
    }

    @Test
    @DisplayName("Arma el pedido de listar con el usuario de la sesión y el objetivo de la ruta")
    void armaElPedidoDeListar() {
        entoncesElPedidoEs(mapper.toListarRequest(USUARIO_ID, OBJETIVO_ID), new ListarDecisionesRequest(USUARIO_ID, OBJETIVO_ID));
    }

    @Test
    @DisplayName("Copia todos los datos de la decisión")
    void copiaTodosLosDatosDeLaDecision() {
        entoncesLaDecisionSeRespondeComo(unAvisoAplicadoYMedido(), new DecisionObjetivoResponse(DECISION_ID,
                TipoDecision.AVISO_APLICADO, MINI_ID, AVISO_ID, PRODUCTO_ID, TITULO_AVISO, AHORA, SEMANA,
                EstadoEfecto.MEDIDO, new BigDecimal("350.00")));
    }

    @Test
    @DisplayName("Conserva las semanas y el orden de las decisiones")
    void conservaLasSemanasYElOrden() {
        cuandoSeRespondenLasSemanas(new SemanaDecisiones(SEMANA, List.of(unPasoAceptado(), unPasoAceptado())));

        entoncesLaUnicaSemanaTieneDecisiones(SEMANA, 2);
    }

    @Test
    @DisplayName("Sin decisiones devuelve una lista vacía")
    void sinDecisionesDevuelveUnaListaVacia() {
        cuandoSeRespondenLasSemanas();

        entoncesNoHaySemanas();
    }

    private static DecisionItem unAvisoAplicadoYMedido() {
        return DecisionItem.de(decision(TipoDecision.AVISO_APLICADO, AVISO_ID),
                new EfectoDecision(EstadoEfecto.MEDIDO, new BigDecimal("350.00")));
    }

    private static DecisionItem unPasoAceptado() {
        return DecisionItem.de(decision(TipoDecision.PASO_ACEPTADO, null), EfectoDecision.SIN_MEDICION);
    }

    private void cuandoSeRespondenLasSemanas(SemanaDecisiones... semanas) {
        decisiones = mapper.toResponse(new ListarDecisionesResponse(List.of(semanas)));
    }

    private static void entoncesElPedidoEs(Object armado, Object esperado) {
        assertThat(armado).isEqualTo(esperado);
    }

    private void entoncesLaDecisionSeRespondeComo(DecisionItem decision, DecisionObjetivoResponse esperada) {
        assertThat(mapper.toResponse(decision)).usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class).isEqualTo(esperada);
    }

    private void entoncesLaUnicaSemanaTieneDecisiones(Object semana, int cantidad) {
        assertThat(decisiones.semanas()).singleElement().satisfies(unaSemana -> {
            assertThat(unaSemana.semanaInicio()).isEqualTo(semana);
            assertThat(unaSemana.decisiones()).hasSize(cantidad);
        });
    }

    private void entoncesNoHaySemanas() {
        assertThat(decisiones.semanas()).isEmpty();
    }
}