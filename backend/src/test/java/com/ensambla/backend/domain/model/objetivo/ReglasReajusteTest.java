package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.ImpactoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.registro.RegistroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ensambla.backend.support.DecisionTestData.AVISO_ID;
import static com.ensambla.backend.support.DecisionTestData.aviso;
import static com.ensambla.backend.support.DecisionTestData.decision;
import static com.ensambla.backend.support.DecisionTestData.impacto;
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidatoDelNegocio;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static org.assertj.core.api.Assertions.assertThat;

class ReglasReajusteTest {

    /** La semana que se cierra: la siguiente a la que empezó el mini-objetivo de prueba. */
    private static final LocalDate SEMANA = LocalDate.of(2026, 11, 16);
    private static final RegistroId OTRO_PRODUCTO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");
    /** Por debajo de la meta de prueba ($14.183). */
    private static final Optional<BigDecimal> SIN_LLEGAR = Optional.of(new BigDecimal("13000"));
    private static final Optional<BigDecimal> NADA = Optional.empty();

    @Test
    @DisplayName("Uno logrado que nadie siguió sube su meta")
    void decidirShouldRaiseTheGoalOfAnAchievedMiniObjetivo() {
        MiniObjetivo mini = conEstado(EstadoMiniObjetivo.LOGRADO);

        assertThat(decidir(mini, NADA, NADA, false)).contains(MotivoReajuste.LOGRADO);
    }

    @Test
    @DisplayName("Uno quitado que nadie reemplazó se cambia por otro")
    void decidirShouldReplaceARemovedMiniObjetivo() {
        MiniObjetivo mini = conEstado(EstadoMiniObjetivo.ABANDONADO);

        assertThat(decidir(mini, NADA, NADA, false)).contains(MotivoReajuste.QUITADO);
    }

    @Test
    @DisplayName("Uno logrado o quitado que ya tiene quien lo siga no se toca")
    void decidirShouldKeepAnAchievedMiniObjetivoAlreadyFollowed() {
        MiniObjetivo mini = conEstado(EstadoMiniObjetivo.LOGRADO);
        mini.setReemplazadoPor(OTRO_MINI_ID);

        assertThat(decidir(mini, NADA, NADA, false)).isEmpty();
    }

    @Test
    @DisplayName("Uno reemplazado no se toca")
    void decidirShouldKeepAReplacedMiniObjetivo() {
        assertThat(decidir(conEstado(EstadoMiniObjetivo.REEMPLAZADO), NADA, NADA, false)).isEmpty();
    }

    @Test
    @DisplayName("Uno de producto que ya no se puede medir se cambia")
    void decidirShouldReplaceAProductMiniObjetivoThatCannotBeMeasured() {
        assertThat(decidir(enCurso(), NADA, NADA, false)).contains(MotivoReajuste.PRODUCTO_FUERA);
    }

    @Test
    @DisplayName("Uno del negocio que no se puede medir (sin costos para el margen) se mantiene")
    void decidirShouldKeepABusinessMiniObjetivoThatCannotBeMeasured() {
        MiniObjetivo mini = enCurso();
        mini.setTipo(TipoMiniObjetivo.MEJORAR_MARGEN);

        assertThat(decidir(mini, NADA, NADA, false)).isEmpty();
    }

    @Test
    @DisplayName("Uno en curso que al domingo llega a su meta sube la meta")
    void decidirShouldRaiseTheGoalWhenTheClosingValueMeetsIt() {
        assertThat(decidir(enCurso(), Optional.of(new BigDecimal("15000")), NADA, true))
                .contains(MotivoReajuste.LOGRADO);
    }

    @Test
    @DisplayName("Uno que empezó la misma semana que se cierra sólo puede subir su meta")
    void decidirShouldKeepAMiniObjetivoThatStartedThisWeek() {
        MiniObjetivo mini = enCurso();
        mini.setSemanaInicio(SEMANA);

        assertThat(decidir(mini, SIN_LLEGAR, SIN_LLEGAR, true)).isEmpty();
    }

    @Test
    @DisplayName("Si una decisión vinculada no movió la venta, prueba otro camino")
    void decidirShouldTryAnotherPathWhenADecisionHadNoEffect() {
        assertThat(decidir(enCurso(), SIN_LLEGAR, NADA, true)).contains(MotivoReajuste.DECISION_SIN_EFECTO);
    }

    @Test
    @DisplayName("Si no mejoró respecto de dos cierres atrás, se cambia por falta de avance")
    void decidirShouldReplaceWhenItDidNotImproveInTwoCloses() {
        assertThat(decidir(enCurso(), SIN_LLEGAR, SIN_LLEGAR, false)).contains(MotivoReajuste.SIN_AVANCE);
    }

    @Test
    @DisplayName("Si mejoró respecto de dos cierres atrás, se mantiene")
    void decidirShouldKeepWhenItImprovedInTwoCloses() {
        assertThat(decidir(enCurso(), SIN_LLEGAR, Optional.of(new BigDecimal("12000")), false)).isEmpty();
    }

    @Test
    @DisplayName("En un tipo que baja, mejorar es bajar: si bajó, se mantiene")
    void decidirShouldKeepADecreasingMiniObjetivoThatWentDown() {
        MiniObjetivo mini = enCurso();
        mini.setTipo(TipoMiniObjetivo.RECORTAR_GASTO);
        mini.setMeta(new BigDecimal("36000"));

        assertThat(decidir(mini, Optional.of(new BigDecimal("38000")), Optional.of(new BigDecimal("40000")), false))
                .isEmpty();
        assertThat(decidir(mini, Optional.of(new BigDecimal("40000")), Optional.of(new BigDecimal("40000")), false))
                .contains(MotivoReajuste.SIN_AVANCE);
    }

    @Test
    @DisplayName("Sin cierre de dos semanas atrás no hay con qué medir el avance: se mantiene")
    void decidirShouldKeepWhenThereIsNoCloseTwoWeeksBefore() {
        assertThat(decidir(enCurso(), SIN_LLEGAR, NADA, false)).isEmpty();
    }

    @Test
    @DisplayName("Una decisión de aplicar un aviso medida y sin subir las ventas no tuvo efecto")
    void decisionSinEfectoShouldBeTrueWhenTheAppliedAvisoDidNotRaiseSales() {
        Aviso aplicado = aplicadoCon(new ImpactoAviso(14, new BigDecimal("3.00"), new BigDecimal("2.00"),
                new BigDecimal("150.00"), new BigDecimal("100.00")));

        assertThat(ReglasReajuste.decisionSinEfecto(miniObjetivo(),
                List.of(decision(TipoDecision.AVISO_APLICADO, AVISO_ID)), Map.of(AVISO_ID, aplicado))).isTrue();
    }

    @Test
    @DisplayName("Una decisión que subió las ventas, que se sigue midiendo o de otro mini-objetivo no cuenta")
    void decisionSinEfectoShouldBeFalseWhenSalesRoseOrItIsStillMeasuringOrUnlinked() {
        Aviso subio = aplicadoCon(impacto());
        Aviso midiendo = aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO);
        MiniObjetivo otro = miniObjetivo();
        otro.setId(OTRO_MINI_ID);

        assertThat(ReglasReajuste.decisionSinEfecto(miniObjetivo(),
                List.of(decision(TipoDecision.AVISO_APLICADO, AVISO_ID)), Map.of(AVISO_ID, subio))).isFalse();
        assertThat(ReglasReajuste.decisionSinEfecto(miniObjetivo(),
                List.of(decision(TipoDecision.AVISO_APLICADO, AVISO_ID)), Map.of(AVISO_ID, midiendo))).isFalse();
        assertThat(ReglasReajuste.decisionSinEfecto(otro,
                List.of(decision(TipoDecision.AVISO_APLICADO, AVISO_ID)), Map.of(AVISO_ID, subio))).isFalse();
    }

    @Test
    @DisplayName("Una decisión sobre un paso, sin aviso, no se mide")
    void decisionSinEfectoShouldBeFalseForAStepWithoutAviso() {
        assertThat(ReglasReajuste.decisionSinEfecto(miniObjetivo(),
                List.of(decision(TipoDecision.PASO_ACEPTADO, null)), Map.of())).isFalse();
    }

    @Test
    @DisplayName("Los usados son los que siguen en curso y los que empezaron en las últimas cuatro semanas")
    void usadosShouldBeTheOnesInProgressOrStartedInTheLastFourWeeks() {
        MiniObjetivo viejoEnCurso = enCurso();
        viejoEnCurso.setSemanaInicio(SEMANA.minusWeeks(10));
        MiniObjetivo recienteLogrado = conEstado(EstadoMiniObjetivo.LOGRADO);
        recienteLogrado.setSemanaInicio(SEMANA.minusWeeks(3));
        MiniObjetivo viejoReemplazado = conEstado(EstadoMiniObjetivo.REEMPLAZADO);
        viejoReemplazado.setSemanaInicio(SEMANA.minusWeeks(4));

        assertThat(ReglasReajuste.usados(List.of(viejoEnCurso, recienteLogrado, viejoReemplazado), SEMANA))
                .containsExactly(viejoEnCurso, recienteLogrado);
    }

    @Test
    @DisplayName("El reemplazo es el de mayor aporte que no es el mismo ni uno usado ni choca con otro en curso")
    void reemplazoShouldBeTheBestCandidateNotUsedNorClashing() {
        MiniObjetivo mini = enCurso();
        CandidatoMiniObjetivo mismo = candidato();
        CandidatoMiniObjetivo usado = candidatoDelNegocio(TipoMiniObjetivo.VENTAS_SEMANALES, "9000");
        CandidatoMiniObjetivo chocaConOtro = candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "8000");
        CandidatoMiniObjetivo libre = candidatoDelNegocio(TipoMiniObjetivo.RECORTAR_GASTO, "1000");

        Optional<CandidatoMiniObjetivo> result = ReglasReajuste.reemplazo(
                List.of(mismo, usado, chocaConOtro, libre), mini, MotivoReajuste.SIN_AVANCE,
                List.of(mini, otroEnCurso(TipoMiniObjetivo.MEJORAR_MARGEN)),
                List.of(otroEnCurso(TipoMiniObjetivo.VENTAS_SEMANALES)));

        assertThat(result).contains(libre);
    }

    @Test
    @DisplayName("Para probar otro camino el reemplazo tiene que ser de otro tipo")
    void reemplazoShouldBeOfAnotherTypeWhenTryingAnotherPath() {
        MiniObjetivo mini = enCurso();
        CandidatoMiniObjetivo mismoTipo = new CandidatoMiniObjetivo(TipoMiniObjetivo.SUBIR_PRECIO, OTRO_PRODUCTO_ID,
                "Aceite", BigDecimal.ONE, BigDecimal.TEN, new BigDecimal("9000"), "Subile al aceite", "x", false);
        CandidatoMiniObjetivo otroTipo = candidatoDelNegocio(TipoMiniObjetivo.RECORTAR_GASTO, "1000");

        assertThat(ReglasReajuste.reemplazo(List.of(mismoTipo, otroTipo), mini, MotivoReajuste.DECISION_SIN_EFECTO,
                List.of(mini), List.of())).contains(otroTipo);
        assertThat(ReglasReajuste.reemplazo(List.of(mismoTipo, otroTipo), mini, MotivoReajuste.SIN_AVANCE,
                List.of(mini), List.of())).contains(mismoTipo);
    }

    @Test
    @DisplayName("Sin candidatos posibles no hay reemplazo")
    void reemplazoShouldBeEmptyWhenThereAreNoCandidates() {
        assertThat(ReglasReajuste.reemplazo(List.of(candidato()), enCurso(), MotivoReajuste.SIN_AVANCE,
                List.of(), List.of())).isEmpty();
    }

    // --- act ---
    private static Optional<MotivoReajuste> decidir(
            MiniObjetivo mini, Optional<BigDecimal> alCierre, Optional<BigDecimal> dosAtras, boolean sinEfecto) {
        return ReglasReajuste.decidir(mini, alCierre, dosAtras, sinEfecto, SEMANA);
    }

    // --- helpers ---
    private static MiniObjetivo enCurso() {
        return miniObjetivo();
    }

    private static MiniObjetivo conEstado(EstadoMiniObjetivo estado) {
        MiniObjetivo mini = miniObjetivo();
        mini.setEstado(estado);
        return mini;
    }

    private static MiniObjetivo otroEnCurso(TipoMiniObjetivo tipo) {
        MiniObjetivo otro = miniObjetivo();
        otro.setId(MiniObjetivoId.nuevo());
        otro.setTipo(tipo);
        otro.setRegistroId(null);
        otro.setReferencia(null);
        return otro;
    }

    private static Aviso aplicadoCon(ImpactoAviso impacto) {
        Aviso aplicado = aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO);
        aplicado.registrarImpacto(impacto, AHORA);
        return aplicado;
    }
}
