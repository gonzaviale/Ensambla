package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.APORTE;
import static com.ensambla.backend.support.MiniObjetivoTestData.JUSTIFICACION;
import static com.ensambla.backend.support.MiniObjetivoTestData.META_MINI;
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.SEMANA;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO;
import static com.ensambla.backend.support.MiniObjetivoTestData.VALOR_INICIAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidatoDelNegocio;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MiniObjetivoTest {

    private static final OffsetDateTime DESPUES = AHORA.plusDays(2);
    private static final LocalDate HOY = LocalDate.of(2026, 11, 12);

    @Test
    @DisplayName("Uno nuevo arranca en curso con el valor inicial del candidato y un id propio")
    void nuevoShouldStartInProgressWithTheCandidateData() {
        MiniObjetivo result = MiniObjetivo.nuevo(objetivo(), 2, candidato(), SEMANA, AHORA);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getObjetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getPosicion()).isEqualTo(2);
        assertThat(result.getTipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(result.getRegistroId()).isEqualTo(PRODUCTO_ID);
        assertThat(result.getReferencia()).isEqualTo(PRODUCTO);
        assertThat(result.getMeta()).isEqualTo(META_MINI);
        assertThat(result.getValorInicial()).isEqualTo(VALOR_INICIAL);
        assertThat(result.getValorActual()).isEqualTo(VALOR_INICIAL);
        assertThat(result.getAporteEstimado()).isEqualTo(APORTE);
        assertThat(result.getEstado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
        assertThat(result.getSemanaInicio()).isEqualTo(SEMANA);
        assertThat(result.getTitulo()).isEqualTo(TITULO);
        assertThat(result.getJustificacion()).isEqualTo(JUSTIFICACION);
        assertThat(result.isTextoConIa()).isFalse();
        assertThat(result.getReemplazadoPor()).isNull();
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.getActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Uno nuevo conserva que el texto lo redactó la IA")
    void nuevoShouldKeepThatTheTextCameFromTheAi() {
        CandidatoMiniObjetivo conIa = candidato().conTextoDeIa(new TextoMiniObjetivo("Título", "Razón"));

        MiniObjetivo result = MiniObjetivo.nuevo(objetivo(), 1, conIa, SEMANA, AHORA);

        assertThat(result.isTextoConIa()).isTrue();
        assertThat(result.getTitulo()).isEqualTo("Título");
    }

    @Test
    @DisplayName("Está en curso mientras no se logre, se reemplace o se quite")
    void estaEnCursoShouldDependOnTheState() {
        assertThat(miniObjetivo().estaEnCurso()).isTrue();
        assertThat(conEstado(EstadoMiniObjetivo.LOGRADO).estaEnCurso()).isFalse();
    }

    @Test
    @DisplayName("Un mini-objetivo que sube se cumple cuando el valor llega a la meta o la pasa")
    void cumplidoShouldBeTrueWhenAnUpwardValueReachesTheGoal() {
        assertThat(conValorActual(TipoMiniObjetivo.SUBIR_PRECIO, "14182.99").cumplido()).isFalse();
        assertThat(conValorActual(TipoMiniObjetivo.SUBIR_PRECIO, "14183.00").cumplido()).isTrue();
        assertThat(conValorActual(TipoMiniObjetivo.SUBIR_PRECIO, "20000.00").cumplido()).isTrue();
    }

    @Test
    @DisplayName("Un mini-objetivo que baja se cumple cuando el valor baja hasta la meta o por debajo")
    void cumplidoShouldBeTrueWhenADownwardValueReachesTheGoal() {
        assertThat(conValorActual(TipoMiniObjetivo.RECORTAR_GASTO, "14183.01").cumplido()).isFalse();
        assertThat(conValorActual(TipoMiniObjetivo.RECORTAR_GASTO, "14183.00").cumplido()).isTrue();
        assertThat(conValorActual(TipoMiniObjetivo.RECORTAR_GASTO, "100.00").cumplido()).isTrue();
    }

    @Test
    @DisplayName("El avance de uno que sube es la parte del camino de lo inicial a la meta que ya se recorrió")
    void avanceShouldBeTheShareOfTheWayFromTheInitialValueToTheGoal() {
        MiniObjetivo mitad = conValores(TipoMiniObjetivo.SUBIR_PRECIO, "100.00", "200.00", "150.00");

        assertThat(mitad.avance()).isEqualByComparingTo("0.5");
    }

    @Test
    @DisplayName("El avance de uno que baja cuenta lo que bajó del valor inicial")
    void avanceShouldCountWhatAValueThatShouldGoDownAlreadyWentDown() {
        MiniObjetivo mitad = conValores(TipoMiniObjetivo.RECORTAR_GASTO, "100.00", "60.00", "80.00");

        assertThat(mitad.avance()).isEqualByComparingTo("0.5");
    }

    @Test
    @DisplayName("El avance no baja de 0 si el valor se aleja de la meta")
    void avanceShouldNotGoBelowZero() {
        MiniObjetivo peor = conValores(TipoMiniObjetivo.SUBIR_PRECIO, "100.00", "200.00", "50.00");

        assertThat(peor.avance()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("El avance no pasa de 1 si el valor supera la meta")
    void avanceShouldNotGoAboveOne() {
        MiniObjetivo superado = conValores(TipoMiniObjetivo.SUBIR_PRECIO, "100.00", "200.00", "500.00");

        assertThat(superado.avance()).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("Sin camino que recorrer, el avance es 1 si ya se cumple y 0 si no")
    void avanceShouldBeOneOrZeroWhenThereIsNoWayToGo() {
        MiniObjetivo cumplido = conValores(TipoMiniObjetivo.SUBIR_PRECIO, "200.00", "200.00", "200.00");
        MiniObjetivo sinCumplir = conValores(TipoMiniObjetivo.SUBIR_PRECIO, "200.00", "200.00", "150.00");

        assertThat(cumplido.avance()).isEqualByComparingTo("1");
        assertThat(sinCumplir.avance()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Al reemplazarlo queda reemplazado, apunta al nuevo y se anota cuándo")
    void reemplazarPorShouldMarkItAsReplaced() {
        MiniObjetivo mini = miniObjetivo();

        mini.reemplazarPor(OTRO_MINI_ID, DESPUES);

        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.REEMPLAZADO);
        assertThat(mini.getReemplazadoPor()).isEqualTo(OTRO_MINI_ID);
        assertThat(mini.getActualizadoEn()).isEqualTo(DESPUES);
    }

    @Test
    @DisplayName("Al quitarlo queda abandonado y se anota cuándo")
    void abandonarShouldMarkItAsAbandoned() {
        MiniObjetivo mini = miniObjetivo();

        mini.abandonar(DESPUES);

        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.ABANDONADO);
        assertThat(mini.getActualizadoEn()).isEqualTo(DESPUES);
    }

    @Test
    @DisplayName("No se puede reemplazar uno que ya no está en curso")
    void reemplazarPorShouldFailWhenItIsNotInProgress() {
        MiniObjetivo mini = conEstado(EstadoMiniObjetivo.LOGRADO);

        assertThatThrownBy(() -> mini.reemplazarPor(OTRO_MINI_ID, DESPUES))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El mini-objetivo ya no está en curso");
    }

    @Test
    @DisplayName("No se puede quitar uno que ya no está en curso")
    void abandonarShouldFailWhenItIsNotInProgress() {
        MiniObjetivo mini = conEstado(EstadoMiniObjetivo.REEMPLAZADO);

        assertThatThrownBy(() -> mini.abandonar(DESPUES))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El mini-objetivo ya no está en curso");
    }

    @Test
    @DisplayName("Propone un mini-objetivo en curso por candidato, en las posiciones 1, 2, 3 y en ese orden")
    void propuestosShouldAssignPositionsInOrder() {
        CandidatoMiniObjetivo margen = candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "14000");

        List<MiniObjetivo> result = MiniObjetivo.propuestos(objetivo(), List.of(candidato(), margen), SEMANA, AHORA);

        assertThat(result).extracting(MiniObjetivo::getPosicion).containsExactly(1, 2);
        assertThat(result).extracting(MiniObjetivo::getTipo)
                .containsExactly(TipoMiniObjetivo.SUBIR_PRECIO, TipoMiniObjetivo.MEJORAR_MARGEN);
        assertThat(result).allSatisfy(mini -> {
            assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
            assertThat(mini.getObjetivoId()).isEqualTo(OBJETIVO_ID);
            assertThat(mini.getSemanaInicio()).isEqualTo(SEMANA);
        });
    }

    @Test
    @DisplayName("Sin candidatos no propone ningún mini-objetivo")
    void propuestosShouldBeEmptyWithoutCandidates() {
        List<MiniObjetivo> result = MiniObjetivo.propuestos(objetivo(), List.of(), SEMANA, AHORA);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Al medir un valor que mejora, lo guarda y deja de estar sin avance")
    void medirShouldRecordTheValueAndClearTheStallWhenItImproves() {
        MiniObjetivo mini = conSinAvanceDesde(HOY.minusDays(5));

        mini.medir(new BigDecimal("13000"), HOY, DESPUES);

        assertThat(mini.getValorActual()).isEqualByComparingTo("13000");
        assertThat(mini.getSinAvanceDesde()).isNull();
        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
        assertThat(mini.getActualizadoEn()).isEqualTo(DESPUES);
    }

    @Test
    @DisplayName("Al medir un valor que no mejora, queda sin avance desde hoy")
    void medirShouldStartTheStallWhenTheValueDoesNotImprove() {
        MiniObjetivo mini = miniObjetivo();

        mini.medir(VALOR_INICIAL, HOY, DESPUES);

        assertThat(mini.getSinAvanceDesde()).isEqualTo(HOY);
        assertThat(mini.getValorActual()).isEqualByComparingTo(VALOR_INICIAL);
    }

    @Test
    @DisplayName("Si ya estaba sin avance y el valor empeora, conserva el día en que empezó")
    void medirShouldKeepTheStallStartWhenTheValueGetsWorse() {
        MiniObjetivo mini = conSinAvanceDesde(HOY.minusDays(5));

        mini.medir(new BigDecimal("12000"), HOY, DESPUES);

        assertThat(mini.getSinAvanceDesde()).isEqualTo(HOY.minusDays(5));
        assertThat(mini.getValorActual()).isEqualByComparingTo("12000");
    }

    @Test
    @DisplayName("En un tipo que baja, un valor menor es una mejora")
    void medirShouldTreatALowerValueAsImprovementWhenTheTypeGoesDown() {
        MiniObjetivo mini = conValores(TipoMiniObjetivo.RECORTAR_GASTO, "40000", "36000", "40000");
        mini.setSinAvanceDesde(HOY.minusDays(3));

        mini.medir(new BigDecimal("38000"), HOY, DESPUES);

        assertThat(mini.getSinAvanceDesde()).isNull();
        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
    }

    @Test
    @DisplayName("En un tipo que baja, un valor mayor no es una mejora")
    void medirShouldNotTreatAHigherValueAsImprovementWhenTheTypeGoesDown() {
        MiniObjetivo mini = conValores(TipoMiniObjetivo.RECORTAR_GASTO, "40000", "36000", "40000");

        mini.medir(new BigDecimal("41000"), HOY, DESPUES);

        assertThat(mini.getSinAvanceDesde()).isEqualTo(HOY);
    }

    @Test
    @DisplayName("Al medir un valor que llega a la meta queda logrado y sin avance")
    void medirShouldMarkAsAchievedWhenTheGoalIsReached() {
        MiniObjetivo mini = conSinAvanceDesde(HOY.minusDays(20));

        mini.medir(META_MINI, HOY, DESPUES);

        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.LOGRADO);
        assertThat(mini.getSinAvanceDesde()).isNull();
        assertThat(mini.getValorActual()).isEqualByComparingTo(META_MINI);
    }

    @Test
    @DisplayName("Al medir guarda el valor con centavos")
    void medirShouldRoundTheValueToCents() {
        MiniObjetivo mini = miniObjetivo();

        mini.medir(new BigDecimal("12999.996"), HOY, DESPUES);

        assertThat(mini.getValorActual()).isEqualTo(new BigDecimal("13000.00"));
    }

    @Test
    @DisplayName("No se mide un mini-objetivo que ya no está en curso")
    void medirShouldFailWhenNotInProgress() {
        MiniObjetivo mini = conEstado(EstadoMiniObjetivo.LOGRADO);

        assertThatThrownBy(() -> mini.medir(META_MINI, HOY, DESPUES))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El mini-objetivo ya no está en curso");
    }

    @Test
    @DisplayName("Uno logrado tiene el hito de logrado")
    void hitoShouldBeAchievedWhenTheMiniObjetivoWasAchieved() {
        MiniObjetivo mini = conEstado(EstadoMiniObjetivo.LOGRADO);

        assertThat(mini.hito(HOY)).contains(HitoObjetivo.MINI_LOGRADO);
    }

    @Test
    @DisplayName("Uno en curso que no mejora desde hace 14 días tiene el hito de sin avance")
    void hitoShouldBeStalledWhenItHasNotImprovedForFourteenDays() {
        MiniObjetivo mini = conSinAvanceDesde(HOY.minusDays(MiniObjetivo.DIAS_SIN_AVANCE));

        assertThat(mini.hito(HOY)).contains(HitoObjetivo.SIN_AVANCE);
    }

    @Test
    @DisplayName("Uno que no mejora desde hace menos de 14 días todavía no tiene hito")
    void hitoShouldBeEmptyWhenTheStallIsRecent() {
        MiniObjetivo mini = conSinAvanceDesde(HOY.minusDays(MiniObjetivo.DIAS_SIN_AVANCE - 1L));

        assertThat(mini.hito(HOY)).isEmpty();
    }

    @Test
    @DisplayName("Uno que viene mejorando no tiene hito")
    void hitoShouldBeEmptyWhenItIsImproving() {
        assertThat(miniObjetivo().hito(HOY)).isEmpty();
    }

    @Test
    @DisplayName("Uno reemplazado no tiene hito aunque haya quedado sin avance")
    void hitoShouldBeEmptyWhenTheMiniObjetivoWasReplaced() {
        MiniObjetivo mini = conSinAvanceDesde(HOY.minusDays(30));
        mini.setEstado(EstadoMiniObjetivo.REEMPLAZADO);

        assertThat(mini.hito(HOY)).isEmpty();
    }

    @Test
    @DisplayName("Un valor cumple la meta de un tipo que baja cuando no la supera")
    void cumpleConShouldBeTrueWhenTheValueIsAtOrBelowTheGoalOfADecreasingType() {
        MiniObjetivo mini = conValores(TipoMiniObjetivo.RECORTAR_GASTO, "40000", "36000", "40000");

        assertThat(mini.cumpleCon(new BigDecimal("36000"))).isTrue();
        assertThat(mini.cumpleCon(new BigDecimal("36001"))).isFalse();
    }

    @Test
    @DisplayName("Uno logrado o quitado que nadie siguió espera el reajuste del cierre")
    void esperaReajusteShouldBeTrueWhenAchievedOrRemovedWithoutAFollower() {
        assertThat(conEstado(EstadoMiniObjetivo.LOGRADO).esperaReajuste()).isTrue();
        assertThat(conEstado(EstadoMiniObjetivo.ABANDONADO).esperaReajuste()).isTrue();
    }

    @Test
    @DisplayName("Uno en curso, reemplazado o ya seguido por otro no espera reajuste")
    void esperaReajusteShouldBeFalseWhenInProgressReplacedOrAlreadyFollowed() {
        MiniObjetivo seguido = conEstado(EstadoMiniObjetivo.LOGRADO);
        seguido.setReemplazadoPor(OTRO_MINI_ID);

        assertThat(miniObjetivo().esperaReajuste()).isFalse();
        assertThat(conEstado(EstadoMiniObjetivo.REEMPLAZADO).esperaReajuste()).isFalse();
        assertThat(seguido.esperaReajuste()).isFalse();
    }

    @Test
    @DisplayName("La meta siguiente suma al valor al cierre el mismo recorrido que se pidió al principio")
    void metaSiguienteShouldAddTheInitialStepToTheClosingValue() {
        MiniObjetivo mini = conValores(TipoMiniObjetivo.SUBIR_PRECIO, "12600", "14183", "14500");

        assertThat(mini.metaSiguiente(new BigDecimal("14500"))).hasValueSatisfying(
                meta -> assertThat(meta).isEqualByComparingTo("16083.00"));
    }

    @Test
    @DisplayName("Si el valor al cierre quedó debajo de la meta, la meta siguiente parte de la meta")
    void metaSiguienteShouldStartFromTheGoalWhenTheValueIsBelowIt() {
        MiniObjetivo mini = conValores(TipoMiniObjetivo.SUBIR_PRECIO, "12600", "14183", "14500");

        assertThat(mini.metaSiguiente(new BigDecimal("14000"))).hasValueSatisfying(
                meta -> assertThat(meta).isEqualByComparingTo("15766.00"));
    }

    @Test
    @DisplayName("En un tipo que baja, la meta siguiente resta el recorrido inicial")
    void metaSiguienteShouldSubtractTheStepWhenTheTypeDecreases() {
        MiniObjetivo mini = conValores(TipoMiniObjetivo.RECORTAR_GASTO, "40000", "36000", "35000");

        assertThat(mini.metaSiguiente(new BigDecimal("35000"))).hasValueSatisfying(
                meta -> assertThat(meta).isEqualByComparingTo("31000.00"));
    }

    @Test
    @DisplayName("Un gasto no puede bajar de cero: si ya llegó, no hay meta siguiente")
    void metaSiguienteShouldBeEmptyWhenAnExpenseCannotGoBelowZero() {
        MiniObjetivo mini = conValores(TipoMiniObjetivo.RECORTAR_GASTO, "100", "0", "0");

        assertThat(mini.metaSiguiente(BigDecimal.ZERO)).isEmpty();
    }

    @Test
    @DisplayName("El margen siguiente no pasa del tope")
    void metaSiguienteShouldCapTheMarginAtTheTop() {
        MiniObjetivo mini = conValores(TipoMiniObjetivo.MEJORAR_MARGEN, "55", "57", "59");

        assertThat(mini.metaSiguiente(new BigDecimal("59"))).hasValueSatisfying(
                meta -> assertThat(meta).isEqualByComparingTo("60.00"));
    }

    @Test
    @DisplayName("Un margen que ya está en el tope no tiene meta siguiente")
    void metaSiguienteShouldBeEmptyWhenTheMarginIsAlreadyAtTheTop() {
        MiniObjetivo mini = conValores(TipoMiniObjetivo.MEJORAR_MARGEN, "58", "60", "60");

        assertThat(mini.metaSiguiente(new BigDecimal("60"))).isEmpty();
    }

    @Test
    @DisplayName("Si la meta era igual al valor inicial, la siguiente pide un 10 % más")
    void metaSiguienteShouldAskTenPercentMoreWhenThereWasNoInitialStep() {
        MiniObjetivo mini = conValores(TipoMiniObjetivo.VENTAS_SEMANALES, "100", "100", "100");

        assertThat(mini.metaSiguiente(new BigDecimal("100"))).hasValueSatisfying(
                meta -> assertThat(meta).isEqualByComparingTo("110.00"));
    }

    @Test
    @DisplayName("Con otra meta conserva el tipo, el producto, el aporte y el título, y arranca del valor al cierre")
    void conMetaShouldKeepTheMiniObjetivoAndStartFromTheClosingValue() {
        CandidatoMiniObjetivo result = miniObjetivo().conMeta(new BigDecimal("14500"), new BigDecimal("16083.00"),
                "Lo lograste");

        assertThat(result.tipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(result.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(result.referencia()).isEqualTo(PRODUCTO);
        assertThat(result.valorInicial()).isEqualByComparingTo("14500.00");
        assertThat(result.valorInicial().scale()).isEqualTo(2);
        assertThat(result.meta()).isEqualByComparingTo("16083.00");
        assertThat(result.aporteEstimado()).isEqualTo(APORTE);
        assertThat(result.titulo()).isEqualTo(TITULO);
        assertThat(result.justificacion()).isEqualTo("Lo lograste");
        assertThat(result.textoConIa()).isFalse();
    }

    @Test
    @DisplayName("Uno en curso que se logra al cierre queda logrado y apunta al que lo sigue")
    void lograrYSeguirConShouldMarkAsAchievedAndPointToTheFollower() {
        MiniObjetivo mini = conSinAvanceDesde(HOY);

        mini.lograrYSeguirCon(OTRO_MINI_ID, DESPUES);

        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.LOGRADO);
        assertThat(mini.getSinAvanceDesde()).isNull();
        assertThat(mini.getReemplazadoPor()).isEqualTo(OTRO_MINI_ID);
        assertThat(mini.getActualizadoEn()).isEqualTo(DESPUES);
    }

    @Test
    @DisplayName("Uno que ya estaba logrado conserva su estado y apunta al que lo sigue")
    void lograrYSeguirConShouldKeepAnAchievedMiniObjetivoAchieved() {
        MiniObjetivo mini = conEstado(EstadoMiniObjetivo.LOGRADO);

        mini.lograrYSeguirCon(OTRO_MINI_ID, DESPUES);

        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.LOGRADO);
        assertThat(mini.getReemplazadoPor()).isEqualTo(OTRO_MINI_ID);
    }

    @Test
    @DisplayName("No se puede seguir uno logrado que ya tiene quien lo siga")
    void lograrYSeguirConShouldRejectAnAchievedMiniObjetivoAlreadyFollowed() {
        MiniObjetivo mini = conEstado(EstadoMiniObjetivo.LOGRADO);
        mini.setReemplazadoPor(MiniObjetivoId.nuevo());

        assertThatThrownBy(() -> mini.lograrYSeguirCon(OTRO_MINI_ID, DESPUES))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El mini-objetivo ya fue reajustado");
    }

    @Test
    @DisplayName("No se puede subir la meta de uno quitado")
    void lograrYSeguirConShouldRejectARemovedMiniObjetivo() {
        MiniObjetivo mini = conEstado(EstadoMiniObjetivo.ABANDONADO);

        assertThatThrownBy(() -> mini.lograrYSeguirCon(OTRO_MINI_ID, DESPUES))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Uno en curso que cede su lugar queda reemplazado")
    void cederLugarAShouldReplaceAMiniObjetivoInProgress() {
        MiniObjetivo mini = miniObjetivo();

        mini.cederLugarA(OTRO_MINI_ID, DESPUES);

        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.REEMPLAZADO);
        assertThat(mini.getReemplazadoPor()).isEqualTo(OTRO_MINI_ID);
    }

    @Test
    @DisplayName("Uno quitado que cede su lugar sigue quitado y apunta al que ocupó el lugar")
    void cederLugarAShouldKeepARemovedMiniObjetivoRemoved() {
        MiniObjetivo mini = conEstado(EstadoMiniObjetivo.ABANDONADO);

        mini.cederLugarA(OTRO_MINI_ID, DESPUES);

        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.ABANDONADO);
        assertThat(mini.getReemplazadoPor()).isEqualTo(OTRO_MINI_ID);
        assertThat(mini.getActualizadoEn()).isEqualTo(DESPUES);
    }

    @Test
    @DisplayName("No cede su lugar uno que ya fue reemplazado")
    void cederLugarAShouldRejectAReplacedMiniObjetivo() {
        MiniObjetivo mini = conEstado(EstadoMiniObjetivo.REEMPLAZADO);

        assertThatThrownBy(() -> mini.cederLugarA(OTRO_MINI_ID, DESPUES))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El mini-objetivo ya fue reajustado");
    }

    // --- helpers ---
    private static MiniObjetivo conSinAvanceDesde(LocalDate dia) {
        MiniObjetivo mini = miniObjetivo();
        mini.setSinAvanceDesde(dia);
        return mini;
    }

    private static MiniObjetivo conEstado(EstadoMiniObjetivo estado) {
        MiniObjetivo mini = miniObjetivo();
        mini.setEstado(estado);
        return mini;
    }

    private static MiniObjetivo conValorActual(TipoMiniObjetivo tipo, String valorActual) {
        MiniObjetivo mini = miniObjetivo();
        mini.setTipo(tipo);
        mini.setValorActual(new BigDecimal(valorActual));
        return mini;
    }

    private static MiniObjetivo conValores(TipoMiniObjetivo tipo, String inicial, String meta, String actual) {
        MiniObjetivo mini = miniObjetivo();
        mini.setTipo(tipo);
        mini.setValorInicial(new BigDecimal(inicial));
        mini.setMeta(new BigDecimal(meta));
        mini.setValorActual(new BigDecimal(actual));
        return mini;
    }
}
