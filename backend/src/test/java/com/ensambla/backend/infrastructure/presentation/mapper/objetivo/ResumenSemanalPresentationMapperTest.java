package com.ensambla.backend.infrastructure.presentation.mapper.objetivo;

import com.ensambla.backend.domain.model.objetivo.HizoManual;
import com.ensambla.backend.domain.model.objetivo.QuePaso;
import com.ensambla.backend.domain.model.objetivo.TipoReajuste;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse.ManualItem;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse.DecisionesItem;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse.MiniItem;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse.ReajusteItem;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse.SemanaItem;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.SemanasObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.SemanasObjetivoResponse.SemanaResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class ResumenSemanalPresentationMapperTest {

    private static final List<ManualItem> MANUALES = List.of(
            new ManualItem(MINI_ID, "Pedile a 3 clientes", HizoManual.SI, QuePaso.FUNCIONO),
            new ManualItem(OTRO_MINI_ID, "Un cartel", null, null));

    private static final LocalDate SEMANA = LocalDate.of(2026, 11, 9);

    private final ResumenSemanalPresentationMapper mapper = new ResumenSemanalPresentationMapper();
    private ListarSemanasRequest pedido;
    private SemanasObjetivoResponse respuesta;

    @Test
    @DisplayName("Arma el pedido con el usuario de la sesión y el objetivo")
    void armaElPedidoConElUsuarioDeLaSesionYElObjetivo() {
        cuandoSeArmaElPedido();

        entoncesElPedidoTraeUsuarioYObjetivo();
    }

    @Test
    @DisplayName("Copia cada semana con sus decisiones y reajustes; un reajuste sin nuevo queda sin «a»")
    void copiaCadaSemanaConSusDecisionesYReajustesUnReajusteSinNuevoQuedaSinA() {
        cuandoSeMapeanLasSemanas(unaSemanaCerrada());

        entoncesSeCopiaLaSemanaConSusDecisionesYManuales();
        entoncesSeCopiaElReajusteSinDestino();
    }

    @Test
    @DisplayName("Sin semanas devuelve una lista vacía")
    void sinSemanasDevuelveUnaListaVacia() {
        cuandoSeMapeanLasSemanas(sinSemanas());

        entoncesNoHayUltimoCierreNiSemanas();
    }

    // --- act ---
    private void cuandoSeArmaElPedido() {
        pedido = mapper.toRequest(USUARIO_ID, OBJETIVO_ID);
    }

    private void cuandoSeMapeanLasSemanas(ListarSemanasResponse semanas) {
        respuesta = mapper.toResponse(semanas);
    }

    // --- assert ---
    private void entoncesElPedidoTraeUsuarioYObjetivo() {
        assertThat(pedido.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(pedido.objetivoId()).isEqualTo(OBJETIVO_ID);
    }

    private void entoncesSeCopiaLaSemanaConSusDecisionesYManuales() {
        SemanaResponse semana = respuesta.semanas().get(0);
        assertThat(respuesta.ultimoCierre()).isEqualTo(SEMANA);
        assertThat(semana.semana()).isEqualTo(SEMANA);
        assertThat(semana.resultado()).isEqualTo(BigDecimal.TEN);
        assertThat(semana.avance()).isEqualTo(BigDecimal.ONE);
        assertThat(semana.decisiones().aplicadas()).isEqualTo(1);
        assertThat(semana.decisiones().descartadas()).isEqualTo(2);
        assertThat(semana.decisiones().aceptadas()).isEqualTo(3);
        assertThat(semana.decisiones().ignoradas()).isEqualTo(4);
        assertThat(semana.queFunciono()).isEqualTo("Bien");
        assertThat(semana.textoConIa()).isTrue();
        assertThat(semana.manuales()).hasSize(2);
        assertThat(semana.manuales().get(0).miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(semana.manuales().get(0).titulo()).isEqualTo("Pedile a 3 clientes");
        assertThat(semana.manuales().get(0).hizo()).isEqualTo(HizoManual.SI);
        assertThat(semana.manuales().get(0).quePaso()).isEqualTo(QuePaso.FUNCIONO);
        assertThat(semana.manuales().get(1).hizo()).isNull();
        assertThat(semana.manuales().get(1).quePaso()).isNull();
    }

    private void entoncesSeCopiaElReajusteSinDestino() {
        assertThat(respuesta.semanas().get(0).reajustes()).singleElement().satisfies(reajuste -> {
            assertThat(reajuste.tipo()).isEqualTo(TipoReajuste.REEMPLAZAR);
            assertThat(reajuste.motivo()).isEqualTo("Sin avance");
            assertThat(reajuste.de().titulo()).isEqualTo("Viejo");
            assertThat(reajuste.de().meta()).isEqualTo(BigDecimal.ONE);
            assertThat(reajuste.a()).isNull();
        });
    }

    private void entoncesNoHayUltimoCierreNiSemanas() {
        assertThat(respuesta.ultimoCierre()).isNull();
        assertThat(respuesta.semanas()).isEmpty();
    }

    // --- helpers ---
    private static SemanaItem unaSemanaConUnReajusteSinNuevo() {
        ReajusteItem reajuste = new ReajusteItem(
                TipoReajuste.REEMPLAZAR, "Sin avance", new MiniItem("Viejo", BigDecimal.ONE), null);
        return new SemanaItem(SEMANA, BigDecimal.TEN, BigDecimal.ONE, new DecisionesItem(1, 2, 3, 4), "Bien", true,
                List.of(reajuste), MANUALES);
    }

    private static ListarSemanasResponse unaSemanaCerrada() {
        return new ListarSemanasResponse(SEMANA, List.of(unaSemanaConUnReajusteSinNuevo()));
    }

    private static ListarSemanasResponse sinSemanas() {
        return new ListarSemanasResponse(null, List.of());
    }
}
