package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.MiniObjetivoTestData.APORTE;
import static com.ensambla.backend.support.MiniObjetivoTestData.JUSTIFICACION;
import static com.ensambla.backend.support.MiniObjetivoTestData.JUSTIFICACION_MANUAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.META_MINI;
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.SEMANA;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO_MANUAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.VALOR_INICIAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidatoDelNegocio;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniManual;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class MiniObjetivoTest {

    private static final OffsetDateTime DESPUES = AHORA.plusDays(2);
    private static final LocalDate HOY = LocalDate.of(2026, 11, 12);

    private MiniObjetivo mini;
    private MiniObjetivo creado;
    private List<MiniObjetivo> propuestos;
    private CandidatoMiniObjetivo candidatoConMeta;
    private CandidatoMiniObjetivo candidatoParaElMini;
    private boolean respuesta;
    private BigDecimal avance;
    private Optional<HitoObjetivo> hito;
    private Optional<BigDecimal> metaSiguiente;
    private Throwable error;

    @Test
    @DisplayName("Uno nuevo arranca en curso con el valor inicial del candidato y un id propio")
    void nuevoShouldStartInProgressWithTheCandidateData() {
        givenUnCandidato(candidato());

        whenSeCreaUnMiniNuevo();

        thenArrancaEnCursoConLosDatosDelCandidato();
    }

    @Test
    @DisplayName("Uno nuevo conserva que el texto lo redactó la IA")
    void nuevoShouldKeepThatTheTextCameFromTheAi() {
        givenUnCandidatoConTextoDeIa();

        whenSeCreaUnMiniNuevo();

        thenConservaQueElTextoLoRedactoLaIa();
    }

    @ParameterizedTest
    @CsvSource({"EN_CURSO, true", "LOGRADO, false"})
    @DisplayName("Está en curso mientras no se logre, se reemplace o se quite")
    void estaEnCursoShouldDependOnTheState(EstadoMiniObjetivo estado, boolean esperado) {
        givenUnMini(conEstado(estado));

        whenSePreguntaSiEstaEnCurso();

        thenLaRespuestaEs(esperado);
    }

    @ParameterizedTest
    @CsvSource({"14182.99, false", "14183.00, true", "20000.00, true"})
    @DisplayName("Un mini-objetivo que sube se cumple cuando el valor llega a la meta o la pasa")
    void cumplidoShouldBeTrueWhenAnUpwardValueReachesTheGoal(String valorActual, boolean esperado) {
        givenUnMini(conValorActual(TipoMiniObjetivo.SUBIR_PRECIO, valorActual));

        whenSePreguntaSiEstaCumplido();

        thenLaRespuestaEs(esperado);
    }

    @ParameterizedTest
    @CsvSource({"14183.01, false", "14183.00, true", "100.00, true"})
    @DisplayName("Un mini-objetivo que baja se cumple cuando el valor baja hasta la meta o por debajo")
    void cumplidoShouldBeTrueWhenADownwardValueReachesTheGoal(String valorActual, boolean esperado) {
        givenUnMini(conValorActual(TipoMiniObjetivo.RECORTAR_GASTO, valorActual));

        whenSePreguntaSiEstaCumplido();

        thenLaRespuestaEs(esperado);
    }

    @Test
    @DisplayName("El avance de uno que sube es la parte del camino de lo inicial a la meta que ya se recorrió")
    void avanceShouldBeTheShareOfTheWayFromTheInitialValueToTheGoal() {
        givenUnMini(conValores(TipoMiniObjetivo.SUBIR_PRECIO, "100.00", "200.00", "150.00"));

        whenSeCalculaElAvance();

        thenElAvanceEs("0.5");
    }

    @Test
    @DisplayName("El avance de uno que baja cuenta lo que bajó del valor inicial")
    void avanceShouldCountWhatAValueThatShouldGoDownAlreadyWentDown() {
        givenUnMini(conValores(TipoMiniObjetivo.RECORTAR_GASTO, "100.00", "60.00", "80.00"));

        whenSeCalculaElAvance();

        thenElAvanceEs("0.5");
    }

    @Test
    @DisplayName("El avance no baja de 0 si el valor se aleja de la meta")
    void avanceShouldNotGoBelowZero() {
        givenUnMini(conValores(TipoMiniObjetivo.SUBIR_PRECIO, "100.00", "200.00", "50.00"));

        whenSeCalculaElAvance();

        thenElAvanceEs("0");
    }

    @Test
    @DisplayName("El avance no pasa de 1 si el valor supera la meta")
    void avanceShouldNotGoAboveOne() {
        givenUnMini(conValores(TipoMiniObjetivo.SUBIR_PRECIO, "100.00", "200.00", "500.00"));

        whenSeCalculaElAvance();

        thenElAvanceEs("1");
    }

    @Test
    @DisplayName("Sin camino que recorrer, el avance es 1 si ya se cumple")
    void avanceShouldBeOneWhenThereIsNoWayToGoAndItIsMet() {
        givenUnMini(conValores(TipoMiniObjetivo.SUBIR_PRECIO, "200.00", "200.00", "200.00"));

        whenSeCalculaElAvance();

        thenElAvanceEs("1");
    }

    @Test
    @DisplayName("Sin camino que recorrer, el avance es 0 si no se cumple")
    void avanceShouldBeZeroWhenThereIsNoWayToGoAndItIsNotMet() {
        givenUnMini(conValores(TipoMiniObjetivo.SUBIR_PRECIO, "200.00", "200.00", "150.00"));

        whenSeCalculaElAvance();

        thenElAvanceEs("0");
    }

    @Test
    @DisplayName("Al reemplazarlo queda reemplazado, apunta al nuevo y se anota cuándo")
    void reemplazarPorShouldMarkItAsReplaced() {
        givenUnMini(miniObjetivo());

        whenSeReemplazaPorOtro();

        thenQuedaReemplazadoApuntandoAlNuevo();
    }

    @Test
    @DisplayName("Al quitarlo queda abandonado y se anota cuándo")
    void abandonarShouldMarkItAsAbandoned() {
        givenUnMini(miniObjetivo());

        whenSeAbandona();

        thenQuedaAbandonado();
    }

    @Test
    @DisplayName("No se puede reemplazar uno que ya no está en curso")
    void reemplazarPorShouldFailWhenItIsNotInProgress() {
        givenUnMini(conEstado(EstadoMiniObjetivo.LOGRADO));

        whenSeReemplazaPorOtro();

        thenSeRechazaPorqueYaNoEstaEnCurso();
    }

    @Test
    @DisplayName("No se puede quitar uno que ya no está en curso")
    void abandonarShouldFailWhenItIsNotInProgress() {
        givenUnMini(conEstado(EstadoMiniObjetivo.REEMPLAZADO));

        whenSeAbandona();

        thenSeRechazaPorqueYaNoEstaEnCurso();
    }

    @Test
    @DisplayName("Propone un mini-objetivo en curso por candidato, en las posiciones 1, 2, 3 y en ese orden")
    void propuestosShouldAssignPositionsInOrder() {
        whenSeProponenDosCandidatos();

        thenSeProponenEnCursoEnLasPosiciones1Y2EnOrden();
    }

    @Test
    @DisplayName("Sin candidatos no propone ningún mini-objetivo")
    void propuestosShouldBeEmptyWithoutCandidates() {
        whenSeProponenCandidatos(List.of());

        thenNoSeProponeNingunMini();
    }

    @Test
    @DisplayName("Al medir un valor que mejora, lo guarda y deja de estar sin avance")
    void medirShouldRecordTheValueAndClearTheStallWhenItImproves() {
        givenUnMini(conSinAvanceDesde(HOY.minusDays(5)));

        whenSeMide("13000");

        thenGuardaElValorYDejaDeEstarSinAvance();
    }

    @Test
    @DisplayName("Al medir un valor que no mejora, queda sin avance desde hoy")
    void medirShouldStartTheStallWhenTheValueDoesNotImprove() {
        givenUnMini(miniObjetivo());

        whenSeMide(VALOR_INICIAL.toPlainString());

        thenQuedaSinAvanceDesdeHoy();
    }

    @Test
    @DisplayName("Si ya estaba sin avance y el valor empeora, conserva el día en que empezó")
    void medirShouldKeepTheStallStartWhenTheValueGetsWorse() {
        givenUnMini(conSinAvanceDesde(HOY.minusDays(5)));

        whenSeMide("12000");

        thenConservaElDiaEnQueEmpezoSinAvance();
    }

    @Test
    @DisplayName("En un tipo que baja, un valor menor es una mejora")
    void medirShouldTreatALowerValueAsImprovementWhenTheTypeGoesDown() {
        givenUnMini(gastoQueBajaSinAvance());

        whenSeMide("38000");

        thenEsUnaMejoraYSigueEnCurso();
    }

    @Test
    @DisplayName("En un tipo que baja, un valor mayor no es una mejora")
    void medirShouldNotTreatAHigherValueAsImprovementWhenTheTypeGoesDown() {
        givenUnMini(conValores(TipoMiniObjetivo.RECORTAR_GASTO, "40000", "36000", "40000"));

        whenSeMide("41000");

        thenQuedaSinAvanceDesdeHoy();
    }

    @Test
    @DisplayName("Al medir un valor que llega a la meta queda logrado y sin avance")
    void medirShouldMarkAsAchievedWhenTheGoalIsReached() {
        givenUnMini(conSinAvanceDesde(HOY.minusDays(20)));

        whenSeMide(META_MINI.toPlainString());

        thenQuedaLogradoYSinAvance();
    }

    @Test
    @DisplayName("Al medir guarda el valor con centavos")
    void medirShouldRoundTheValueToCents() {
        givenUnMini(miniObjetivo());

        whenSeMide("12999.996");

        thenGuardaElValorConCentavos();
    }

    @Test
    @DisplayName("No se mide un mini-objetivo que ya no está en curso")
    void medirShouldFailWhenNotInProgress() {
        givenUnMini(conEstado(EstadoMiniObjetivo.LOGRADO));

        whenSeMide(META_MINI.toPlainString());

        thenSeRechazaPorqueYaNoEstaEnCurso();
    }

    @Test
    @DisplayName("Uno logrado tiene el hito de logrado")
    void hitoShouldBeAchievedWhenTheMiniObjetivoWasAchieved() {
        givenUnMini(conEstado(EstadoMiniObjetivo.LOGRADO));

        whenSePideElHito();

        thenElHitoEs(HitoObjetivo.MINI_LOGRADO);
    }

    @Test
    @DisplayName("Uno en curso que no mejora desde hace 14 días tiene el hito de sin avance")
    void hitoShouldBeStalledWhenItHasNotImprovedForFourteenDays() {
        givenUnMini(conSinAvanceDesde(HOY.minusDays(MiniObjetivo.DIAS_SIN_AVANCE)));

        whenSePideElHito();

        thenElHitoEs(HitoObjetivo.SIN_AVANCE);
    }

    @Test
    @DisplayName("Uno que no mejora desde hace menos de 14 días todavía no tiene hito")
    void hitoShouldBeEmptyWhenTheStallIsRecent() {
        givenUnMini(conSinAvanceDesde(HOY.minusDays(MiniObjetivo.DIAS_SIN_AVANCE - 1L)));

        whenSePideElHito();

        thenNoTieneHito();
    }

    @Test
    @DisplayName("Uno que viene mejorando no tiene hito")
    void hitoShouldBeEmptyWhenItIsImproving() {
        givenUnMini(miniObjetivo());

        whenSePideElHito();

        thenNoTieneHito();
    }

    @Test
    @DisplayName("Uno reemplazado no tiene hito aunque haya quedado sin avance")
    void hitoShouldBeEmptyWhenTheMiniObjetivoWasReplaced() {
        givenUnMini(reemplazadoQueQuedoSinAvance());

        whenSePideElHito();

        thenNoTieneHito();
    }

    @ParameterizedTest
    @CsvSource({"36000, true", "36001, false"})
    @DisplayName("Un valor cumple la meta de un tipo que baja cuando no la supera")
    void cumpleConShouldBeTrueWhenTheValueIsAtOrBelowTheGoalOfADecreasingType(String valor, boolean esperado) {
        givenUnMini(conValores(TipoMiniObjetivo.RECORTAR_GASTO, "40000", "36000", "40000"));

        whenSePreguntaSiElValorCumple(valor);

        thenLaRespuestaEs(esperado);
    }

    @ParameterizedTest
    @CsvSource({"LOGRADO", "ABANDONADO"})
    @DisplayName("Uno logrado o quitado que nadie siguió espera el reajuste del cierre")
    void esperaReajusteShouldBeTrueWhenAchievedOrRemovedWithoutAFollower(EstadoMiniObjetivo estado) {
        givenUnMini(conEstado(estado));

        whenSePreguntaSiEsperaReajuste();

        thenLaRespuestaEs(true);
    }

    @Test
    @DisplayName("Uno en curso no espera reajuste")
    void esperaReajusteShouldBeFalseWhenInProgress() {
        givenUnMini(miniObjetivo());

        whenSePreguntaSiEsperaReajuste();

        thenLaRespuestaEs(false);
    }

    @Test
    @DisplayName("Uno reemplazado no espera reajuste")
    void esperaReajusteShouldBeFalseWhenReplaced() {
        givenUnMini(conEstado(EstadoMiniObjetivo.REEMPLAZADO));

        whenSePreguntaSiEsperaReajuste();

        thenLaRespuestaEs(false);
    }

    @Test
    @DisplayName("Uno logrado que ya tiene quien lo siga no espera reajuste")
    void esperaReajusteShouldBeFalseWhenAlreadyFollowed() {
        givenUnMini(logradoYaSeguidoPorOtro());

        whenSePreguntaSiEsperaReajuste();

        thenLaRespuestaEs(false);
    }

    @Test
    @DisplayName("La meta siguiente suma al valor al cierre el mismo recorrido que se pidió al principio")
    void metaSiguienteShouldAddTheInitialStepToTheClosingValue() {
        givenUnMini(conValores(TipoMiniObjetivo.SUBIR_PRECIO, "12600", "14183", "14500"));

        whenSePideLaMetaSiguienteConValorAlCierre("14500");

        thenLaMetaSiguienteEs("16083.00");
    }

    @Test
    @DisplayName("Si el valor al cierre quedó debajo de la meta, la meta siguiente parte de la meta")
    void metaSiguienteShouldStartFromTheGoalWhenTheValueIsBelowIt() {
        givenUnMini(conValores(TipoMiniObjetivo.SUBIR_PRECIO, "12600", "14183", "14500"));

        whenSePideLaMetaSiguienteConValorAlCierre("14000");

        thenLaMetaSiguienteEs("15766.00");
    }

    @Test
    @DisplayName("En un tipo que baja, la meta siguiente resta el recorrido inicial")
    void metaSiguienteShouldSubtractTheStepWhenTheTypeDecreases() {
        givenUnMini(conValores(TipoMiniObjetivo.RECORTAR_GASTO, "40000", "36000", "35000"));

        whenSePideLaMetaSiguienteConValorAlCierre("35000");

        thenLaMetaSiguienteEs("31000.00");
    }

    @Test
    @DisplayName("Un gasto no puede bajar de cero: si ya llegó, no hay meta siguiente")
    void metaSiguienteShouldBeEmptyWhenAnExpenseCannotGoBelowZero() {
        givenUnMini(conValores(TipoMiniObjetivo.RECORTAR_GASTO, "100", "0", "0"));

        whenSePideLaMetaSiguienteConValorAlCierre("0");

        thenNoHayMetaSiguiente();
    }

    @Test
    @DisplayName("El margen siguiente no pasa del tope")
    void metaSiguienteShouldCapTheMarginAtTheTop() {
        givenUnMini(conValores(TipoMiniObjetivo.MEJORAR_MARGEN, "55", "57", "59"));

        whenSePideLaMetaSiguienteConValorAlCierre("59");

        thenLaMetaSiguienteEs("60.00");
    }

    @Test
    @DisplayName("Un margen que ya está en el tope no tiene meta siguiente")
    void metaSiguienteShouldBeEmptyWhenTheMarginIsAlreadyAtTheTop() {
        givenUnMini(conValores(TipoMiniObjetivo.MEJORAR_MARGEN, "58", "60", "60"));

        whenSePideLaMetaSiguienteConValorAlCierre("60");

        thenNoHayMetaSiguiente();
    }

    @Test
    @DisplayName("Si la meta era igual al valor inicial, la siguiente pide un 10 % más")
    void metaSiguienteShouldAskTenPercentMoreWhenThereWasNoInitialStep() {
        givenUnMini(conValores(TipoMiniObjetivo.VENTAS_SEMANALES, "100", "100", "100"));

        whenSePideLaMetaSiguienteConValorAlCierre("100");

        thenLaMetaSiguienteEs("110.00");
    }

    @Test
    @DisplayName("Con otra meta conserva el tipo, el producto, el aporte y el título, y arranca del valor al cierre")
    void conMetaShouldKeepTheMiniObjetivoAndStartFromTheClosingValue() {
        givenUnMini(miniObjetivo());

        whenSePideUnCandidatoConOtraMeta();

        thenElCandidatoConservaElMiniYArrancaDelValorAlCierre();
    }

    @Test
    @DisplayName("Uno en curso que se logra al cierre queda logrado y apunta al que lo sigue")
    void lograrYSeguirConShouldMarkAsAchievedAndPointToTheFollower() {
        givenUnMini(conSinAvanceDesde(HOY));

        whenSeLograYSeguirConOtro();

        thenQuedaLogradoSinAvanceYApuntaAlQueLoSigue();
    }

    @Test
    @DisplayName("Uno que ya estaba logrado conserva su estado y apunta al que lo sigue")
    void lograrYSeguirConShouldKeepAnAchievedMiniObjetivoAchieved() {
        givenUnMini(conEstado(EstadoMiniObjetivo.LOGRADO));

        whenSeLograYSeguirConOtro();

        thenSigueLogradoYApuntaAlQueLoSigue();
    }

    @Test
    @DisplayName("No se puede seguir uno logrado que ya tiene quien lo siga")
    void lograrYSeguirConShouldRejectAnAchievedMiniObjetivoAlreadyFollowed() {
        givenUnMini(logradoYaSeguidoPorOtro());

        whenSeLograYSeguirConOtro();

        thenSeRechazaPorqueYaFueReajustado();
    }

    @Test
    @DisplayName("No se puede subir la meta de uno quitado")
    void lograrYSeguirConShouldRejectARemovedMiniObjetivo() {
        givenUnMini(conEstado(EstadoMiniObjetivo.ABANDONADO));

        whenSeLograYSeguirConOtro();

        thenSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Uno en curso que cede su lugar queda reemplazado")
    void cederLugarAShouldReplaceAMiniObjetivoInProgress() {
        givenUnMini(miniObjetivo());

        whenCedeSuLugarAOtro();

        thenQuedaReemplazadoYApuntaAlQueOcupaElLugar();
    }

    @Test
    @DisplayName("Uno quitado que cede su lugar sigue quitado y apunta al que ocupó el lugar")
    void cederLugarAShouldKeepARemovedMiniObjetivoRemoved() {
        givenUnMini(conEstado(EstadoMiniObjetivo.ABANDONADO));

        whenCedeSuLugarAOtro();

        thenSigueQuitadoYApuntaAlQueOcupoElLugar();
    }

    @Test
    @DisplayName("No cede su lugar uno que ya fue reemplazado")
    void cederLugarAShouldRejectAReplacedMiniObjetivo() {
        givenUnMini(conEstado(EstadoMiniObjetivo.REEMPLAZADO));

        whenCedeSuLugarAOtro();

        thenSeRechazaPorqueYaFueReajustado();
    }

    @Test
    @DisplayName("Un mini-objetivo es medido por defecto y no manual")
    void esMedidoShouldBeTheDefaultMode() {
        givenUnMini(miniObjetivo());

        thenEsMedidoYNoManual();
    }

    @Test
    @DisplayName("Un mini-objetivo manual no es medido")
    void esManualShouldBeTrueForAManualMini() {
        givenUnMini(miniManual());

        thenEsManualYNoMedido();
    }

    @Test
    @DisplayName("Un manual en curso se puede responder")
    void exigirManualEnCursoShouldAcceptAManualInProgress() {
        givenUnMini(miniManual());

        whenSeExigeQueSeaUnManualEnCurso();

        thenNoSeRechaza();
    }

    @Test
    @DisplayName("Un mini-objetivo medido no se responde")
    void exigirManualEnCursoShouldRejectAMeasuredMini() {
        givenUnMini(miniObjetivo());

        whenSeExigeQueSeaUnManualEnCurso();

        thenSeRechazaPorqueSoloSeRespondenLosManuales();
    }

    @Test
    @DisplayName("Un manual que ya no está en curso no se responde")
    void exigirManualEnCursoShouldRejectAManualNoLongerInProgress() {
        givenUnMini(manualAbandonado());

        whenSeExigeQueSeaUnManualEnCurso();

        thenSeRechazaPorqueYaNoEstaEnCurso();
    }

    @Test
    @DisplayName("Una acción manual nace en curso, sin métrica ni números, en la posición y la semana pedidas")
    void manualShouldBuildAManualMiniInProgress() {
        whenSeCreaUnMiniManual();

        thenNaceManualEnCursoSinMetricaNiNumeros();
    }

    // --- arrange ---
    private void givenUnMini(MiniObjetivo miniDePrueba) {
        mini = miniDePrueba;
    }

    private void givenUnCandidato(CandidatoMiniObjetivo candidatoDePrueba) {
        candidatoParaElMini = candidatoDePrueba;
    }

    // --- act ---
    private void whenSeCreaUnMiniNuevo() {
        creado = MiniObjetivo.nuevo(objetivo(), 2, candidatoParaElMini, SEMANA, AHORA);
    }

    private void whenSeCreaUnMiniManual() {
        creado = MiniObjetivo.manual(
                objetivo(), 2, new AccionManual(TITULO_MANUAL, JUSTIFICACION_MANUAL), true, SEMANA, AHORA);
    }

    private void whenSeProponenDosCandidatos() {
        CandidatoMiniObjetivo margen = candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "14000");
        whenSeProponenCandidatos(List.of(candidato(), margen));
    }

    private void whenSeProponenCandidatos(List<CandidatoMiniObjetivo> candidatos) {
        propuestos = MiniObjetivo.propuestos(objetivo(), candidatos, SEMANA, AHORA);
    }

    private void whenSePreguntaSiEstaEnCurso() {
        respuesta = mini.estaEnCurso();
    }

    private void whenSePreguntaSiEstaCumplido() {
        respuesta = mini.cumplido();
    }

    private void whenSePreguntaSiElValorCumple(String valor) {
        respuesta = mini.cumpleCon(new BigDecimal(valor));
    }

    private void whenSePreguntaSiEsperaReajuste() {
        respuesta = mini.esperaReajuste();
    }

    private void whenSeCalculaElAvance() {
        avance = mini.avance();
    }

    private void whenSeReemplazaPorOtro() {
        error = catchThrowable(() -> mini.reemplazarPor(OTRO_MINI_ID, DESPUES));
    }

    private void whenSeAbandona() {
        error = catchThrowable(() -> mini.abandonar(DESPUES));
    }

    private void whenSeMide(String valor) {
        error = catchThrowable(() -> mini.medir(new BigDecimal(valor), HOY, DESPUES));
    }

    private void whenSePideElHito() {
        hito = mini.hito(HOY);
    }

    private void whenSePideLaMetaSiguienteConValorAlCierre(String valorAlCierre) {
        metaSiguiente = mini.metaSiguiente(new BigDecimal(valorAlCierre));
    }

    private void whenSePideUnCandidatoConOtraMeta() {
        candidatoConMeta = mini.conMeta(new BigDecimal("14500"), new BigDecimal("16083.00"), "Lo lograste");
    }

    private void whenSeLograYSeguirConOtro() {
        error = catchThrowable(() -> mini.lograrYSeguirCon(OTRO_MINI_ID, DESPUES));
    }

    private void whenCedeSuLugarAOtro() {
        error = catchThrowable(() -> mini.cederLugarA(OTRO_MINI_ID, DESPUES));
    }

    private void whenSeExigeQueSeaUnManualEnCurso() {
        error = catchThrowable(() -> mini.exigirManualEnCurso());
    }

    // --- assert ---
    private void thenArrancaEnCursoConLosDatosDelCandidato() {
        assertThat(creado.getId()).isNotNull();
        assertThat(creado.getObjetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(creado.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(creado.getPosicion()).isEqualTo(2);
        assertThat(creado.getTipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(creado.getRegistroId()).isEqualTo(PRODUCTO_ID);
        assertThat(creado.getReferencia()).isEqualTo(PRODUCTO);
        assertThat(creado.getMeta()).isEqualTo(META_MINI);
        assertThat(creado.getValorInicial()).isEqualTo(VALOR_INICIAL);
        assertThat(creado.getValorActual()).isEqualTo(VALOR_INICIAL);
        assertThat(creado.getAporteEstimado()).isEqualTo(APORTE);
        assertThat(creado.getEstado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
        assertThat(creado.getSemanaInicio()).isEqualTo(SEMANA);
        assertThat(creado.getTitulo()).isEqualTo(TITULO);
        assertThat(creado.getJustificacion()).isEqualTo(JUSTIFICACION);
        assertThat(creado.isTextoConIa()).isFalse();
        assertThat(creado.getReemplazadoPor()).isNull();
        assertThat(creado.getCreadoEn()).isEqualTo(AHORA);
        assertThat(creado.getActualizadoEn()).isEqualTo(AHORA);
    }

    private void thenConservaQueElTextoLoRedactoLaIa() {
        assertThat(creado.isTextoConIa()).isTrue();
        assertThat(creado.getTitulo()).isEqualTo("Título");
    }

    private void thenLaRespuestaEs(boolean esperada) {
        assertThat(respuesta).isEqualTo(esperada);
    }

    private void thenElAvanceEs(String esperado) {
        assertThat(avance).isEqualByComparingTo(esperado);
    }

    private void thenQuedaReemplazadoApuntandoAlNuevo() {
        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.REEMPLAZADO);
        assertThat(mini.getReemplazadoPor()).isEqualTo(OTRO_MINI_ID);
        assertThat(mini.getActualizadoEn()).isEqualTo(DESPUES);
    }

    private void thenQuedaAbandonado() {
        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.ABANDONADO);
        assertThat(mini.getActualizadoEn()).isEqualTo(DESPUES);
    }

    private void thenSeRechazaPorqueYaNoEstaEnCurso() {
        assertThat(error)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El mini-objetivo ya no está en curso");
    }

    private void thenSeProponenEnCursoEnLasPosiciones1Y2EnOrden() {
        assertThat(propuestos).extracting(MiniObjetivo::getPosicion).containsExactly(1, 2);
        assertThat(propuestos).extracting(MiniObjetivo::getTipo)
                .containsExactly(TipoMiniObjetivo.SUBIR_PRECIO, TipoMiniObjetivo.MEJORAR_MARGEN);
        assertThat(propuestos).allSatisfy(propuesto -> {
            assertThat(propuesto.getEstado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
            assertThat(propuesto.getObjetivoId()).isEqualTo(OBJETIVO_ID);
            assertThat(propuesto.getSemanaInicio()).isEqualTo(SEMANA);
        });
    }

    private void thenNoSeProponeNingunMini() {
        assertThat(propuestos).isEmpty();
    }

    private void thenGuardaElValorYDejaDeEstarSinAvance() {
        assertThat(mini.getValorActual()).isEqualByComparingTo("13000");
        assertThat(mini.getSinAvanceDesde()).isNull();
        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
        assertThat(mini.getActualizadoEn()).isEqualTo(DESPUES);
    }

    private void thenQuedaSinAvanceDesdeHoy() {
        assertThat(mini.getSinAvanceDesde()).isEqualTo(HOY);
    }

    private void thenConservaElDiaEnQueEmpezoSinAvance() {
        assertThat(mini.getSinAvanceDesde()).isEqualTo(HOY.minusDays(5));
        assertThat(mini.getValorActual()).isEqualByComparingTo("12000");
    }

    private void thenEsUnaMejoraYSigueEnCurso() {
        assertThat(mini.getSinAvanceDesde()).isNull();
        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
    }

    private void thenQuedaLogradoYSinAvance() {
        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.LOGRADO);
        assertThat(mini.getSinAvanceDesde()).isNull();
        assertThat(mini.getValorActual()).isEqualByComparingTo(META_MINI);
    }

    private void thenGuardaElValorConCentavos() {
        assertThat(mini.getValorActual()).isEqualTo(new BigDecimal("13000.00"));
    }

    private void thenElHitoEs(HitoObjetivo esperado) {
        assertThat(hito).contains(esperado);
    }

    private void thenNoTieneHito() {
        assertThat(hito).isEmpty();
    }

    private void thenLaMetaSiguienteEs(String esperada) {
        assertThat(metaSiguiente).hasValueSatisfying(meta -> assertThat(meta).isEqualByComparingTo(esperada));
    }

    private void thenNoHayMetaSiguiente() {
        assertThat(metaSiguiente).isEmpty();
    }

    private void thenElCandidatoConservaElMiniYArrancaDelValorAlCierre() {
        assertThat(candidatoConMeta.tipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(candidatoConMeta.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(candidatoConMeta.referencia()).isEqualTo(PRODUCTO);
        assertThat(candidatoConMeta.valorInicial()).isEqualByComparingTo("14500.00");
        assertThat(candidatoConMeta.valorInicial().scale()).isEqualTo(2);
        assertThat(candidatoConMeta.meta()).isEqualByComparingTo("16083.00");
        assertThat(candidatoConMeta.aporteEstimado()).isEqualTo(APORTE);
        assertThat(candidatoConMeta.titulo()).isEqualTo(TITULO);
        assertThat(candidatoConMeta.justificacion()).isEqualTo("Lo lograste");
        assertThat(candidatoConMeta.textoConIa()).isFalse();
    }

    private void thenQuedaLogradoSinAvanceYApuntaAlQueLoSigue() {
        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.LOGRADO);
        assertThat(mini.getSinAvanceDesde()).isNull();
        assertThat(mini.getReemplazadoPor()).isEqualTo(OTRO_MINI_ID);
        assertThat(mini.getActualizadoEn()).isEqualTo(DESPUES);
    }

    private void thenSigueLogradoYApuntaAlQueLoSigue() {
        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.LOGRADO);
        assertThat(mini.getReemplazadoPor()).isEqualTo(OTRO_MINI_ID);
    }

    private void thenSeRechazaPorqueYaFueReajustado() {
        assertThat(error)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El mini-objetivo ya fue reajustado");
    }

    private void thenSeRechazaPorReglaDeNegocio() {
        assertThat(error).isInstanceOf(BusinessRuleException.class);
    }

    private void thenQuedaReemplazadoYApuntaAlQueOcupaElLugar() {
        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.REEMPLAZADO);
        assertThat(mini.getReemplazadoPor()).isEqualTo(OTRO_MINI_ID);
    }

    private void thenSigueQuitadoYApuntaAlQueOcupoElLugar() {
        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.ABANDONADO);
        assertThat(mini.getReemplazadoPor()).isEqualTo(OTRO_MINI_ID);
        assertThat(mini.getActualizadoEn()).isEqualTo(DESPUES);
    }

    private void thenEsMedidoYNoManual() {
        assertThat(mini.getModo()).isEqualTo(ModoMiniObjetivo.MEDIDO);
        assertThat(mini.esMedido()).isTrue();
        assertThat(mini.esManual()).isFalse();
    }

    private void thenEsManualYNoMedido() {
        assertThat(mini.esManual()).isTrue();
        assertThat(mini.esMedido()).isFalse();
    }

    private void thenNoSeRechaza() {
        assertThat(error).isNull();
    }

    private void thenSeRechazaPorqueSoloSeRespondenLosManuales() {
        assertThat(error)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Sólo se responden los mini-objetivos manuales");
    }

    private void thenNaceManualEnCursoSinMetricaNiNumeros() {
        assertThat(creado.getId()).isNotNull();
        assertThat(creado.getObjetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(creado.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(creado.getPosicion()).isEqualTo(2);
        assertThat(creado.esManual()).isTrue();
        assertThat(creado.getEstado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
        assertThat(creado.getSemanaInicio()).isEqualTo(SEMANA);
        assertThat(creado.getTitulo()).isEqualTo(TITULO_MANUAL);
        assertThat(creado.getJustificacion()).isEqualTo(JUSTIFICACION_MANUAL);
        assertThat(creado.isTextoConIa()).isTrue();
        assertThat(creado.getTipo()).isNull();
        assertThat(creado.getMeta()).isNull();
        assertThat(creado.getValorInicial()).isNull();
        assertThat(creado.getValorActual()).isNull();
        assertThat(creado.getAporteEstimado()).isNull();
        assertThat(creado.getCreadoEn()).isEqualTo(AHORA);
        assertThat(creado.getActualizadoEn()).isEqualTo(AHORA);
    }

    // --- helpers ---
    private static MiniObjetivo conSinAvanceDesde(LocalDate dia) {
        MiniObjetivo miniDePrueba = miniObjetivo();
        miniDePrueba.setSinAvanceDesde(dia);
        return miniDePrueba;
    }

    private static MiniObjetivo conEstado(EstadoMiniObjetivo estado) {
        MiniObjetivo miniDePrueba = miniObjetivo();
        miniDePrueba.setEstado(estado);
        return miniDePrueba;
    }

    private static MiniObjetivo conValorActual(TipoMiniObjetivo tipo, String valorActual) {
        MiniObjetivo miniDePrueba = miniObjetivo();
        miniDePrueba.setTipo(tipo);
        miniDePrueba.setValorActual(new BigDecimal(valorActual));
        return miniDePrueba;
    }

    private static MiniObjetivo conValores(TipoMiniObjetivo tipo, String inicial, String meta, String actual) {
        MiniObjetivo miniDePrueba = miniObjetivo();
        miniDePrueba.setTipo(tipo);
        miniDePrueba.setValorInicial(new BigDecimal(inicial));
        miniDePrueba.setMeta(new BigDecimal(meta));
        miniDePrueba.setValorActual(new BigDecimal(actual));
        return miniDePrueba;
    }

    private static MiniObjetivo gastoQueBajaSinAvance() {
        MiniObjetivo gasto = conValores(TipoMiniObjetivo.RECORTAR_GASTO, "40000", "36000", "40000");
        gasto.setSinAvanceDesde(HOY.minusDays(3));
        return gasto;
    }

    private static MiniObjetivo reemplazadoQueQuedoSinAvance() {
        MiniObjetivo reemplazado = conSinAvanceDesde(HOY.minusDays(30));
        reemplazado.setEstado(EstadoMiniObjetivo.REEMPLAZADO);
        return reemplazado;
    }

    private static MiniObjetivo logradoYaSeguidoPorOtro() {
        MiniObjetivo seguido = conEstado(EstadoMiniObjetivo.LOGRADO);
        seguido.setReemplazadoPor(MiniObjetivoId.nuevo());
        return seguido;
    }

    private static MiniObjetivo manualAbandonado() {
        MiniObjetivo manual = miniManual();
        manual.abandonar(AHORA);
        return manual;
    }

    private void givenUnCandidatoConTextoDeIa() {
        givenUnCandidato(candidato().conTextoDeIa(new TextoMiniObjetivo("Título", "Razón")));
    }
}
