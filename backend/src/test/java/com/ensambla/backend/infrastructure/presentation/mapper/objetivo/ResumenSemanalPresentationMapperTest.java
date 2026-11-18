package com.ensambla.backend.infrastructure.presentation.mapper.objetivo;

import com.ensambla.backend.domain.model.objetivo.TipoReajuste;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasRequest;
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

import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class ResumenSemanalPresentationMapperTest {

    private static final LocalDate SEMANA = LocalDate.of(2026, 11, 9);

    private final ResumenSemanalPresentationMapper mapper = new ResumenSemanalPresentationMapper();
    private SemanasObjetivoResponse semanas;

    @Test
    @DisplayName("Arma el pedido con el usuario de la sesión y el objetivo")
    void armaElPedidoConElUsuarioYElObjetivo() {
        entoncesElPedidoEs(new ListarSemanasRequest(USUARIO_ID, OBJETIVO_ID));
    }

    @Test
    @DisplayName("Copia cada semana con su resultado, sus decisiones y lo que funcionó")
    void copiaCadaSemanaConSusDecisiones() {
        cuandoSeRespondeConUnaSemanaReajustada();

        entoncesElUltimoCierreEs(SEMANA);
        entoncesLaSemanaEs(SEMANA, BigDecimal.TEN, BigDecimal.ONE, "Bien");
        entoncesLasDecisionesSon(1, 2, 3, 4);
    }

    @Test
    @DisplayName("Copia los reajustes de la semana; un reajuste sin nuevo queda sin «a»")
    void copiaLosReajustesDeLaSemana() {
        cuandoSeRespondeConUnaSemanaReajustada();

        entoncesElUnicoReajusteReemplazaA("Viejo", BigDecimal.ONE, "Sin avance");
    }

    @Test
    @DisplayName("Sin semanas devuelve una lista vacía")
    void sinSemanasDevuelveUnaListaVacia() {
        cuandoSeRespondeSinSemanas();

        entoncesNoHaySemanas();
    }

    private void cuandoSeRespondeConUnaSemanaReajustada() {
        semanas = mapper.toResponse(new ListarSemanasResponse(SEMANA, List.of(new SemanaItem(
                SEMANA, BigDecimal.TEN, BigDecimal.ONE, new DecisionesItem(1, 2, 3, 4), "Bien", true, List.of(
                        new ReajusteItem(TipoReajuste.REEMPLAZAR, "Sin avance", new MiniItem("Viejo", BigDecimal.ONE),
                                null))))));
    }

    private void cuandoSeRespondeSinSemanas() {
        semanas = mapper.toResponse(new ListarSemanasResponse(null, List.of()));
    }

    private SemanaResponse laSemana() {
        return semanas.semanas().getFirst();
    }

    private void entoncesElPedidoEs(ListarSemanasRequest esperado) {
        assertThat(mapper.toRequest(USUARIO_ID, OBJETIVO_ID)).isEqualTo(esperado);
    }

    private void entoncesElUltimoCierreEs(LocalDate cierre) {
        assertThat(semanas.ultimoCierre()).isEqualTo(cierre);
    }

    private void entoncesLaSemanaEs(LocalDate semana, BigDecimal resultado, BigDecimal avance, String queFunciono) {
        assertThat(laSemana().semana()).isEqualTo(semana);
        assertThat(laSemana().resultado()).isEqualTo(resultado);
        assertThat(laSemana().avance()).isEqualTo(avance);
        assertThat(laSemana().queFunciono()).isEqualTo(queFunciono);
        assertThat(laSemana().textoConIa()).isTrue();
    }

    private void entoncesLasDecisionesSon(int aplicadas, int descartadas, int aceptadas, int ignoradas) {
        assertThat(laSemana().decisiones().aplicadas()).isEqualTo(aplicadas);
        assertThat(laSemana().decisiones().descartadas()).isEqualTo(descartadas);
        assertThat(laSemana().decisiones().aceptadas()).isEqualTo(aceptadas);
        assertThat(laSemana().decisiones().ignoradas()).isEqualTo(ignoradas);
    }

    private void entoncesElUnicoReajusteReemplazaA(String titulo, BigDecimal meta, String motivo) {
        assertThat(laSemana().reajustes()).singleElement().satisfies(reajuste -> {
            assertThat(reajuste.tipo()).isEqualTo(TipoReajuste.REEMPLAZAR);
            assertThat(reajuste.motivo()).isEqualTo(motivo);
            assertThat(reajuste.de().titulo()).isEqualTo(titulo);
            assertThat(reajuste.de().meta()).isEqualTo(meta);
            assertThat(reajuste.a()).isNull();
        });
    }

    private void entoncesNoHaySemanas() {
        assertThat(semanas.ultimoCierre()).isNull();
        assertThat(semanas.semanas()).isEmpty();
    }
}