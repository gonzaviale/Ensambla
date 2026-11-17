package com.ensambla.backend.domain.useCase.objetivo.cerrarSemanasObjetivos;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.ImpactoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.AccionRenovacion;
import com.ensambla.backend.domain.model.objetivo.CandidatoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.CierreMini;
import com.ensambla.backend.domain.model.objetivo.ConteoDecisiones;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivo;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivoId;
import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.HizoManual;
import com.ensambla.backend.domain.model.objetivo.ManualCierre;
import com.ensambla.backend.domain.model.objetivo.MedidorMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.objetivo.PedidoRenovacion;
import com.ensambla.backend.domain.model.objetivo.QuePaso;
import com.ensambla.backend.domain.model.objetivo.ReajusteMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.RedaccionObjetivosIa;
import com.ensambla.backend.domain.model.objetivo.RespuestaMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.RespuestaMiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ResumenSemanal;
import com.ensambla.backend.domain.model.objetivo.ResumenSemanalId;
import com.ensambla.backend.domain.model.objetivo.TextoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoDecision;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoReajuste;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.port.RenovacionManualesIaPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.objetivo.DecisionObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.RespuestaMiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ResumenSemanalRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.DecisionTestData.AVISO_ID;
import static com.ensambla.backend.support.DecisionTestData.aviso;
import static com.ensambla.backend.support.DecisionTestData.decision;
import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO_MANUAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidatoDelNegocio;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniManual;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static com.ensambla.backend.support.ObjetivoTestData.objetivoSinMeta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CerrarSemanasObjetivosTest {

    /** El lunes 16/11/2026 a la medianoche argentina: la última semana cerrable es la del 9/11. */
    private static final OffsetDateTime AHORA_CIERRE = OffsetDateTime.parse("2026-11-16T03:00:00Z");
    private static final Clock CLOCK = Clock.fixed(AHORA_CIERRE.toInstant(), ZoneOffset.UTC);
    private static final LocalDate SEMANA = LocalDate.of(2026, 11, 9);
    private static final LocalDate DOMINGO = LocalDate.of(2026, 11, 15);
    private static final LocalDate DIA_DEL_CIERRE = LocalDate.of(2026, 11, 16);
    private static final NegocioId OTRO_NEGOCIO_ID = NegocioId.de("00000000-0000-4000-8000-0000000000aa");
    private static final ObjetivoId OTRO_OBJETIVO_ID = ObjetivoId.de("00000000-0000-4000-8000-0000000000bb");
    private static final RegistroId OTRO_PRODUCTO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");
    /** Por debajo de la meta del mini-objetivo de prueba ($14.183). */
    private static final BigDecimal SIN_LLEGAR = new BigDecimal("13000");
    private static final BigDecimal META_ALCANZADA = new BigDecimal("15000");
    private static final int LIMITE_DIARIO_IA = 10;
    private static final int SEMANAS_DE_UN_OBJETIVO_VIEJO = 20;
    private static final ConsumoIaSami CONSUMO = new ConsumoIaSami("simulado", "simulado", 120, 80);
    private static final CandidatoMiniObjetivo VENTAS = candidatoDelNegocio(TipoMiniObjetivo.VENTAS_SEMANALES, "9000");

    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class);
    private final MiniObjetivoRepository miniObjetivoRepository = mock(MiniObjetivoRepository.class);
    private final DecisionObjetivoRepository decisionObjetivoRepository = mock(DecisionObjetivoRepository.class);
    private final AvisoRepository avisoRepository = mock(AvisoRepository.class);
    private final ResumenSemanalRepository resumenSemanalRepository = mock(ResumenSemanalRepository.class);
    private final PropuestaMiniObjetivosService propuestaMiniObjetivosService =
            mock(PropuestaMiniObjetivosService.class);
    private final RespuestaMiniObjetivoRepository respuestaMiniObjetivoRepository =
            mock(RespuestaMiniObjetivoRepository.class);
    private final RenovacionManualesIaPort renovacionManualesIaPort = mock(RenovacionManualesIaPort.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final ConsumoSamiRepository consumoSamiRepository = mock(ConsumoSamiRepository.class);
    private final MedidorMiniObjetivo medidor = mock(MedidorMiniObjetivo.class);
    private CerrarSemanasObjetivos cerrarSemanasObjetivos;
    private MiniObjetivo miniAnterior;

    @BeforeEach
    void setUp() {
        cerrarSemanasObjetivos = cierre(8, 50);
        when(objetivoRepository.findNegocioIdsConObjetivos(0, 50)).thenReturn(List.of(NEGOCIO_ID));
        when(objetivoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(objetivo()));
        when(propuestaMiniObjetivosService.medidor(any(), any())).thenReturn(medidor);
        when(medidor.resultado()).thenReturn(new BigDecimal("180000"));
        when(medidor.ventasSemana()).thenReturn(new BigDecimal("40000"));
        when(miniObjetivoRepository.save(any(MiniObjetivo.class))).thenAnswer(call -> call.getArgument(0));
        when(resumenSemanalRepository.save(any(ResumenSemanal.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    @DisplayName("Cierra la semana pendiente con el resultado al domingo y las decisiones de esa semana")
    void executeShouldCloseThePendingWeekWithTheSundayResultAndItsDecisions() {
        givenMinis(miniObjetivo(), miniConEstado(EstadoMiniObjetivo.REEMPLAZADO, OTRO_MINI_ID));
        givenValor(SIN_LLEGAR);
        givenDecisiones(decision(TipoDecision.PASO_ACEPTADO, null), decisionDeOtraSemana());

        whenCerrar();

        thenSeCierraLaSemanaConElResultadoDelDomingoYSusDecisiones();
    }

    @Test
    @DisplayName("Un mini-objetivo que llegó a su meta sigue con una meta más alta desde la semana siguiente")
    void executeShouldRaiseTheGoalOfAnAchievedMiniObjetivo() {
        givenUnMiniAnterior(miniObjetivo());
        givenValor(META_ALCANZADA);

        whenCerrar();

        thenElMiniLogradoSigueConUnaMetaMasAlta();
    }

    @Test
    @DisplayName("El mini-objetivo logrado se guarda antes que el nuevo")
    void executeShouldSaveTheAchievedMiniObjetivoBeforeTheNewOne() {
        givenUnMiniAnterior(miniObjetivo());
        givenValor(META_ALCANZADA);

        whenCerrar();

        thenSeGuardaElAnteriorAntesQueElNuevo();
    }

    @Test
    @DisplayName("Uno que no avanzó respecto de dos cierres atrás se cambia por el mejor candidato")
    void executeShouldReplaceAMiniObjetivoWithoutProgressInTwoCloses() {
        givenUnMiniAnterior(deSemanasAtras());
        givenValor(SIN_LLEGAR);
        givenDosCierresSinAvance();
        givenCandidatos(VENTAS);

        whenCerrar();

        thenElMiniSinAvanceSeReemplazaPorElMejorCandidato();
    }

    @Test
    @DisplayName("Sin un candidato para reemplazarlo, el mini-objetivo se mantiene")
    void executeShouldKeepTheMiniObjetivoWhenThereIsNoCandidate() {
        givenUnMiniAnterior(deSemanasAtras());
        givenValor(SIN_LLEGAR);
        givenDosCierresSinAvance();
        givenCandidatos();

        whenCerrar();

        thenElMiniSeMantieneSinReajustes();
    }

    @Test
    @DisplayName("Si un aviso aplicado no movió la venta, prueba con un candidato de otro tipo")
    void executeShouldTryAnotherPathWhenAnAppliedAvisoHadNoEffect() {
        givenUnMiniAnterior(deSemanasAtras());
        givenValor(SIN_LLEGAR);
        givenDecisiones(decision(TipoDecision.AVISO_APLICADO, AVISO_ID));
        givenAvisoSinEfecto();
        givenCandidatos(candidatoDeOtroProducto(), VENTAS);

        whenCerrar();

        thenSeProbaOtroCaminoConUnCandidatoDeOtroTipo();
    }

    @Test
    @DisplayName("Probar otro camino consulta el aviso aplicado pero no crea avisos")
    void executeShouldNotCreateAvisosWhenTryingAnotherPath() {
        givenUnMiniAnterior(deSemanasAtras());
        givenValor(SIN_LLEGAR);
        givenDecisiones(decision(TipoDecision.AVISO_APLICADO, AVISO_ID));
        givenAvisoSinEfecto();
        givenCandidatos(candidatoDeOtroProducto(), VENTAS);

        whenCerrar();

        thenSeConsultaElAvisoAplicadoSinCrearAvisos();
    }

    @Test
    @DisplayName("Uno cuyo producto ya no está en el cuaderno se cambia y no entra en el cierre")
    void executeShouldReplaceAMiniObjetivoWhoseProductIsGone() {
        givenMinis(miniObjetivo());
        givenCandidatos(VENTAS);

        whenCerrar();

        thenSeCambiaElMiniCuyoProductoYaNoEstaYNoEntraEnElCierre();
    }

    @Test
    @DisplayName("El lugar de uno quitado se completa y el quitado conserva su estado")
    void executeShouldFillThePlaceOfARemovedMiniObjetivo() {
        givenUnMiniAnterior(miniConEstado(EstadoMiniObjetivo.ABANDONADO, MINI_ID));
        givenCandidatos(VENTAS);

        whenCerrar();

        thenSeCompletaElLugarDelQuitadoYConservaSuEstado();
    }

    @Test
    @DisplayName("El lugar de uno quitado se completa aunque haya otro en curso en otro lugar")
    void executeShouldFillThePlaceOfARemovedMiniObjetivoWhenAnotherPlaceIsTaken() {
        givenMinis(quitadoEnElSegundoLugar(), miniObjetivo());
        givenValor(SIN_LLEGAR);
        givenCandidatos(VENTAS);

        whenCerrar();

        thenSeCompletaElSegundoLugar();
    }

    @Test
    @DisplayName("Uno logrado cuyo lugar ya ocupa otro en curso no se toca")
    void executeShouldNotTouchAnAchievedMiniObjetivoWhosePlaceIsTaken() {
        givenUnMiniAnterior(miniConEstado(EstadoMiniObjetivo.LOGRADO, OTRO_MINI_ID));
        givenMinis(miniAnterior, miniObjetivo());
        givenValor(SIN_LLEGAR);

        whenCerrar();

        thenElLogradoNoSeToca();
    }

    @Test
    @DisplayName("Uno logrado que ya no puede pedir más deja su lugar al mejor candidato")
    void executeShouldReplaceAnAchievedMiniObjetivoThatCannotAskForMore() {
        givenUnMiniAnterior(margenLogradoEnSuTope());
        givenCandidatos(VENTAS);

        whenCerrar();

        thenElLogradoSinTechoDejaSuLugarAlMejorCandidato();
    }

    @Test
    @DisplayName("Con el objetivo alcanzado no se reajusta nada")
    void executeShouldNotAdjustWhenTheObjetivoIsReached() {
        givenMinis(miniObjetivo());
        givenValor(META_ALCANZADA);
        givenUnResultadoQueSuperaLaMeta();

        whenCerrar();

        thenNoSeReajustaNadaNiSePidenCandidatos();
    }

    @Test
    @DisplayName("Un objetivo que ya estaba alcanzado no se reajusta aunque el resultado haya bajado")
    void executeShouldNotAdjustWhenTheObjetivoWasAlreadyReached() {
        givenObjetivo(objetivoYaAlcanzado());
        givenMinis(miniObjetivo());
        givenValor(META_ALCANZADA);

        whenCerrar();

        thenElResumenFelicitaSinReajustar();
    }

    @Test
    @DisplayName("En una semana sin ventas no se reajusta nada")
    void executeShouldNotAdjustWhenThereWereNoSales() {
        givenMinis(miniObjetivo());
        givenValor(META_ALCANZADA);
        givenUnaSemanaSinVentas();

        whenCerrar();

        thenNoSeReajustaNadaNiSeGuardaNingunMini();
    }

    @Test
    @DisplayName("Recupera como mucho las semanas pendientes más nuevas, cada una con lo medido a su domingo")
    void executeShouldRecoverAtMostTheNewestPendingWeeks() {
        givenUnCierreQueRecuperaComoMuchoTresSemanas();
        givenObjetivo(creadoHaceSemanas(SEMANAS_DE_UN_OBJETIVO_VIEJO));

        whenCerrar();

        thenSeRecuperanLasTresSemanasMasNuevasConLoMedidoASuDomingo();
    }

    @Test
    @DisplayName("Sigue desde la semana siguiente a la última cerrada")
    void executeShouldContinueFromTheWeekAfterTheLastClosed() {
        givenObjetivo(creadoHaceSemanas(SEMANAS_DE_UN_OBJETIVO_VIEJO));
        givenResumenes(resumen(SEMANA.minusWeeks(1), "175000", SIN_LLEGAR));

        whenCerrar();

        thenSeCierraLaSemana(SEMANA);
    }

    @Test
    @DisplayName("Un cierre anterior a la creación del objetivo no adelanta el comienzo")
    void executeShouldStartFromTheCreationWeekWhenTheLastCloseIsOlder() {
        givenResumenes(resumen(SEMANA.minusWeeks(5), "175000", SIN_LLEGAR));

        whenCerrar();

        thenSeCierraLaSemana(SEMANA);
    }

    @Test
    @DisplayName("Si la última semana ya está cerrada no hace nada")
    void executeShouldDoNothingWhenTheLastWeekIsAlreadyClosed() {
        givenResumenes(resumen(SEMANA, "175000", SIN_LLEGAR));

        whenCerrar();

        thenNoSeCierraNiSeMideNada();
    }

    @Test
    @DisplayName("Si otra corrida ya cerró una semana, sigue con la siguiente")
    void executeShouldContinueWithTheNextWeekWhenAnotherRunAlreadyClosedOne() {
        givenObjetivo(creadoHaceSemanas(1));
        givenQueOtraCorridaYaCerroLaPrimeraSemana();

        whenCerrar();

        thenSeIntentanCerrarDosSemanas();
    }

    @Test
    @DisplayName("Un negocio con error no frena el cierre de los demás")
    void executeShouldContinueWithOtherBusinessesWhenOneFails() {
        givenUnNegocioCaidoAntesDeOtroQueFunciona();

        whenCerrar();

        thenSeCierraLaSemanaDelNegocioQueFunciona();
    }

    @Test
    @DisplayName("Pide los negocios de a tandas hasta que una viene incompleta")
    void executeShouldRequestBusinessesInBatches() {
        givenNegociosEnTandasDeUno();

        whenCerrar();

        thenSePideLaSegundaTandaYSeCierraLaSemana();
    }

    @Test
    @DisplayName("Los objetivos de un negocio comparten lo medido al domingo")
    void executeShouldShareTheMeasurementBetweenObjetivosOfTheSameBusiness() {
        givenDosObjetivosEnElMismoNegocio();

        whenCerrar();

        thenSeCierranLosDosObjetivosConUnaSolaMedicion();
    }

    @Test
    @DisplayName("Un mini-objetivo manual no se reajusta ni suma su valor al cierre")
    void executeShouldLeaveAManualMiniObjetivoUntouched() {
        givenUnMiniAnterior(miniManualAbandonado());
        givenMinis(miniObjetivo(), miniAnterior);
        givenValor(SIN_LLEGAR);

        whenCerrar();

        thenElManualAbandonadoQuedaIntactoYNoSumaAlCierre();
    }

    @Test
    @DisplayName("Un objetivo sin meta en pesos cierra la semana sin avance y sin decir cuánto falta")
    void executeShouldCloseTheWeekOfAnObjetivoWithoutGoal() {
        givenObjetivo(objetivoSinMeta());
        givenMinis(miniObjetivo(), miniManual());
        givenValor(SIN_LLEGAR);

        whenCerrar();

        thenSeCierraSinAvanceNiCuantoFalta();
    }

    @Test
    @DisplayName("Un manual que la persona hizo y le funcionó queda logrado")
    void executeShouldMarkAsAchievedAManualThatWorked() {
        givenUnMiniAnterior(miniManual());
        givenRespuesta(HizoManual.SI, QuePaso.FUNCIONO, null);

        whenCerrar();

        thenElManualQuedaLogradoYLoSigueUnoNuevo();
    }

    @Test
    @DisplayName("Un manual que funcionó lo sigue uno con un paso más")
    void executeShouldRenewAManualThatWorkedWithOneStepMore() {
        givenUnMiniAnterior(miniManual());
        givenRespuesta(HizoManual.SI, QuePaso.FUNCIONO, null);

        whenCerrar();

        thenElNuevoManualEsUnPasoMas();
    }

    @Test
    @DisplayName("El manual logrado se guarda antes que el nuevo")
    void executeShouldSaveTheAchievedManualBeforeTheNewOne() {
        givenUnMiniAnterior(miniManual());
        givenRespuesta(HizoManual.SI, QuePaso.FUNCIONO, null);

        whenCerrar();

        thenSeGuardaElAnteriorAntesQueElNuevo();
    }

    @Test
    @DisplayName("Un manual que se hizo en parte queda reemplazado por lo que faltó")
    void executeShouldSplitAManualDoneInPart() {
        givenUnMiniAnterior(miniManual());
        givenRespuesta(HizoManual.EN_PARTE, QuePaso.NO_TUVE_TIEMPO, "Hablé con 1 sola vecina");

        whenCerrar();

        thenElManualQuedaReemplazadoPorLoQueFalto();
    }

    @Test
    @DisplayName("Un manual que no se hizo por falta de tiempo se simplifica")
    void executeShouldSimplifyAManualThatWasNotDone() {
        givenMinis(miniManual());
        givenRespuesta(HizoManual.NO, QuePaso.NO_TUVE_TIEMPO, null);

        whenCerrar();

        thenElManualSeSimplifica();
    }

    @Test
    @DisplayName("Un manual que no funcionó cambia por otra acción, aunque se haya hecho")
    void executeShouldChangeAManualThatDidNotWork() {
        givenUnMiniAnterior(miniManual());
        givenRespuesta(HizoManual.SI, QuePaso.NO_FUNCIONO, null);

        whenCerrar();

        thenElManualQuedaLogradoYCambiaPorOtraAccion();
    }

    @Test
    @DisplayName("El resumen cuenta lo respondido de los manuales")
    void executeShouldSummarizeTheManualAnswers() {
        givenMinis(miniManual());
        givenRespuesta(HizoManual.SI, QuePaso.FUNCIONO, null);

        whenCerrar();

        thenElResumenCuentaLoRespondidoDeLosManuales();
    }

    @Test
    @DisplayName("Un manual sin respuesta no se renueva: sigue igual")
    void executeShouldNotRenewAManualWithoutAnswer() {
        givenUnMiniAnterior(miniManual());

        whenCerrar();

        thenElManualSinRespuestaSigueIgual();
    }

    @Test
    @DisplayName("Un manual sin respuesta queda en el resumen, que dice que sigue igual hasta que responda")
    void executeShouldTellInTheSummaryThatAManualWithoutAnswerStaysTheSame() {
        givenMinis(miniManual());

        whenCerrar();

        thenElResumenDiceQueElManualSigueIgualHastaQueResponda();
    }

    @Test
    @DisplayName("Los medidos se reajustan como siempre y los manuales se renuevan en el mismo cierre")
    void executeShouldReadjustTheMeasuredAndRenewTheManualOnesTogether() {
        givenMinis(miniObjetivo(), miniManual());
        givenValor(META_ALCANZADA);
        givenRespuesta(HizoManual.NO, QuePaso.OTRA, null);

        whenCerrar();

        thenSeReajustanLosMedidosYSeRenuevanLosManuales();
    }

    @Test
    @DisplayName("Una semana sin ventas no se reajustan los medidos pero sí se renuevan los manuales")
    void executeShouldRenewTheManualOnesInAWeekWithoutSales() {
        givenUnaSemanaSinVentas();
        givenMinis(miniManual());
        givenRespuesta(HizoManual.SI, QuePaso.FUNCIONO, null);

        whenCerrar();

        thenSeRenuevaElManualConUnPasoMas();
    }

    @Test
    @DisplayName("Un manual que empieza después de la semana que se cierra no entra en su cierre")
    void executeShouldIgnoreAManualOfALaterWeek() {
        givenUnMiniAnterior(miniManualDeLaSemanaSiguiente());
        givenRespuesta(HizoManual.SI, QuePaso.FUNCIONO, null);

        whenCerrar();

        thenElManualDeLaSemanaSiguienteNoEntraNiSeRenueva();
    }

    @Test
    @DisplayName("Un objetivo ya alcanzado no renueva sus manuales pero el resumen cuenta lo que respondieron")
    void executeShouldNotRenewTheManualOnesOfAnAchievedObjetivo() {
        givenObjetivo(objetivoYaAlcanzado());
        givenUnMiniAnterior(miniManual());
        givenRespuesta(HizoManual.SI, QuePaso.FUNCIONO, null);

        whenCerrar();

        thenElResumenCuentaElManualPeroNoLoRenueva();
    }

    @Test
    @DisplayName("Un manual renovado no se vuelve a renovar en la semana siguiente del mismo cierre")
    void executeShouldNotRenewTheSameManualTwiceWhenClosingSeveralWeeks() {
        givenObjetivo(creadoHaceSemanas(1));
        givenUnMiniAnterior(miniManual());
        givenRespuesta(HizoManual.SI, QuePaso.FUNCIONO, null);

        whenCerrar();

        thenElManualSeRenuevaUnaSolaVezEnDosSemanas();
    }

    @Test
    @DisplayName("Con IA, la acción nueva la redacta la IA leyendo lo que contó la persona")
    void executeShouldLetTheIaWriteTheRenewedAction() {
        givenUnCierreConIa();
        givenMinis(miniManual());
        givenRespuesta(HizoManual.EN_PARTE, QuePaso.NO_TUVE_TIEMPO, "Le mandé un mensaje a ana@mail.com");
        givenIaDisponibleQueRedacta();

        whenCerrar();

        thenLaAccionNuevaLaRedactaLaIa();
    }

    @Test
    @DisplayName("A la IA le pide la renovación con lo que contó la persona, sin datos personales")
    void executeShouldAskTheIaWithTheSafeCommentOfThePerson() {
        givenUnCierreConIa();
        givenMinis(miniManual());
        givenRespuesta(HizoManual.EN_PARTE, QuePaso.NO_TUVE_TIEMPO, "Le mandé un mensaje a ana@mail.com");
        givenIaDisponibleQueRedacta();

        whenCerrar();

        thenLaIaRecibeElPedidoSinDatosPersonales();
    }

    @Test
    @DisplayName("Cuando la IA redacta se registra su consumo")
    void executeShouldRegisterTheIaConsumption() {
        givenUnCierreConIa();
        givenMinis(miniManual());
        givenRespuesta(HizoManual.EN_PARTE, QuePaso.NO_TUVE_TIEMPO, "Le mandé un mensaje a ana@mail.com");
        givenIaDisponibleQueRedacta();

        whenCerrar();

        thenSeRegistraElConsumoDeLaIa();
    }

    @Test
    @DisplayName("Si el texto de la IA no es una acción válida (trae un importe), se usa el texto de reserva")
    void executeShouldFallBackWhenTheIaTextIsNotAValidAction() {
        givenUnCierreConIa();
        givenMinis(miniManual());
        givenRespuesta(HizoManual.NO, QuePaso.OTRA, null);
        givenIaDisponibleQueRedactaUnTextoConImporte();

        whenCerrar();

        thenSeUsaElTextoDeReserva();
    }

    @Test
    @DisplayName("Si la IA falla se usa el texto de reserva")
    void executeShouldFallBackWhenTheIaFails() {
        givenUnCierreConIa();
        givenMinis(miniManual());
        givenRespuesta(HizoManual.NO, QuePaso.OTRA, null);
        givenIaDisponibleQueFalla();

        whenCerrar();

        thenSeUsaElTextoDeReserva();
    }

    @Test
    @DisplayName("Si la IA falla no se registra consumo")
    void executeShouldNotRegisterConsumptionWhenTheIaFails() {
        givenUnCierreConIa();
        givenMinis(miniManual());
        givenRespuesta(HizoManual.NO, QuePaso.OTRA, null);
        givenIaDisponibleQueFalla();

        whenCerrar();

        thenNoSeRegistraConsumo();
    }

    @Test
    @DisplayName("Si la IA devuelve menos textos que renovaciones se usa el texto de reserva")
    void executeShouldFallBackWhenTheIaReturnsFewerTexts() {
        givenUnCierreConIa();
        givenMinis(miniManual());
        givenRespuesta(HizoManual.NO, QuePaso.OTRA, null);
        givenIaDisponibleQueDevuelveMenosTextos();

        whenCerrar();

        thenSeUsaElTextoDeReserva();
    }

    @Test
    @DisplayName("Sin acceso a la IA no la consulta y usa el texto de reserva")
    void executeShouldNotConsultTheIaWithoutAccess() {
        givenUnCierreConIa();
        givenMinis(miniManual());
        givenRespuesta(HizoManual.NO, QuePaso.OTRA, null);
        givenUnNegocioSinAccesoALaIa();

        whenCerrar();

        thenSeUsaElTextoDeReservaSinConsultarLaIa();
    }

    @Test
    @DisplayName("Sin reserva diaria de IA no la consulta y usa el texto de reserva")
    void executeShouldNotConsultTheIaWithoutDailyAllowance() {
        givenUnCierreConIa();
        givenMinis(miniManual());
        givenRespuesta(HizoManual.NO, QuePaso.OTRA, null);
        givenElLimiteDiarioDeIaAgotado();

        whenCerrar();

        thenSeUsaElTextoDeReservaSinPedirleNadaALaIa();
    }

    @Test
    @DisplayName("Sin nada para renovar (un manual sin respuesta) no consulta a la IA")
    void executeShouldNotConsultTheIaWithNothingToRenew() {
        givenUnCierreConIa();
        givenMinis(miniManual());

        whenCerrar();

        thenNoSeConsultaLaIaNiSuCuota();
    }

    @Test
    @DisplayName("Con la IA apagada nunca la consulta")
    void executeShouldNeverConsultTheIaWhenItIsDisabled() {
        givenMinis(miniManual());
        givenRespuesta(HizoManual.NO, QuePaso.OTRA, null);

        whenCerrar();

        thenNoSeConsultaLaIaNiSuCuota();
    }

    @Test
    @DisplayName("Un objetivo alcanzado no consulta a la IA")
    void executeShouldNotConsultTheIaForAnAchievedObjetivo() {
        givenUnCierreConIa();
        givenObjetivo(objetivoYaAlcanzado());
        givenMinis(miniManual());
        givenRespuesta(HizoManual.NO, QuePaso.OTRA, null);

        whenCerrar();

        thenNoSePideNadaALaIa();
    }

    // --- arrange ---
    private CerrarSemanasObjetivos cierre(int maxSemanasPendientes, int tamanoTanda) {
        return cierre(maxSemanasPendientes, tamanoTanda, false);
    }

    private CerrarSemanasObjetivos cierre(int maxSemanasPendientes, int tamanoTanda, boolean iaHabilitada) {
        return new CerrarSemanasObjetivos(objetivoRepository, miniObjetivoRepository, decisionObjetivoRepository,
                avisoRepository, resumenSemanalRepository, propuestaMiniObjetivosService,
                respuestaMiniObjetivoRepository, renovacionManualesIaPort, controlPlanService, consumoSamiRepository,
                new TransaccionDirecta(), CLOCK, maxSemanasPendientes, tamanoTanda, iaHabilitada, LIMITE_DIARIO_IA);
    }

    private void givenUnCierreConIa() {
        cerrarSemanasObjetivos = cierre(8, 50, true);
    }

    private void givenUnCierreQueRecuperaComoMuchoTresSemanas() {
        cerrarSemanasObjetivos = cierre(3, 50);
    }

    private void givenObjetivo(Objetivo objetivo) {
        when(objetivoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(objetivo));
    }

    private void givenMinis(MiniObjetivo... minis) {
        when(miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of(minis));
    }

    private void givenUnMiniAnterior(MiniObjetivo mini) {
        miniAnterior = mini;
        givenMinis(mini);
    }

    private void givenValor(BigDecimal valor) {
        when(medidor.valorDe(TipoMiniObjetivo.SUBIR_PRECIO, PRODUCTO_ID, PRODUCTO)).thenReturn(Optional.of(valor));
    }

    private void givenDecisiones(DecisionObjetivo... decisiones) {
        when(decisionObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of(decisiones));
    }

    private void givenResumenes(ResumenSemanal... resumenes) {
        when(resumenSemanalRepository.findAllByObjetivoId(OBJETIVO_ID)).thenReturn(List.of(resumenes));
    }

    private void givenDosCierresSinAvance() {
        givenResumenes(resumen(SEMANA.minusWeeks(1), "175000", SIN_LLEGAR),
                resumen(SEMANA.minusWeeks(2), "170000", SIN_LLEGAR));
    }

    private void givenCandidatos(CandidatoMiniObjetivo... candidatos) {
        when(propuestaMiniObjetivosService.candidatos(NEGOCIO_ID, medidor)).thenReturn(List.of(candidatos));
    }

    private void givenAvisoSinEfecto() {
        Aviso aplicado = aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO);
        aplicado.registrarImpacto(new ImpactoAviso(14, new BigDecimal("3.00"), new BigDecimal("2.00"),
                new BigDecimal("150.00"), new BigDecimal("100.00")), AHORA);
        when(avisoRepository.findAllByIdsAndNegocioId(List.of(AVISO_ID), NEGOCIO_ID)).thenReturn(List.of(aplicado));
    }

    private void givenUnResultadoQueSuperaLaMeta() {
        when(medidor.resultado()).thenReturn(new BigDecimal("400000"));
    }

    private void givenUnaSemanaSinVentas() {
        when(medidor.ventasSemana()).thenReturn(BigDecimal.ZERO);
    }

    private void givenQueOtraCorridaYaCerroLaPrimeraSemana() {
        when(resumenSemanalRepository.save(any(ResumenSemanal.class)))
                .thenThrow(new DuplicateResourceException("Esa semana del objetivo ya está cerrada"))
                .thenAnswer(call -> call.getArgument(0));
    }

    private void givenUnNegocioCaidoAntesDeOtroQueFunciona() {
        when(objetivoRepository.findNegocioIdsConObjetivos(0, 50)).thenReturn(List.of(OTRO_NEGOCIO_ID, NEGOCIO_ID));
        when(objetivoRepository.findAllByNegocioId(OTRO_NEGOCIO_ID)).thenThrow(new IllegalStateException("caído"));
    }

    private void givenNegociosEnTandasDeUno() {
        cerrarSemanasObjetivos = cierre(8, 1);
        when(objetivoRepository.findNegocioIdsConObjetivos(0, 1)).thenReturn(List.of(NEGOCIO_ID));
        when(objetivoRepository.findNegocioIdsConObjetivos(1, 1)).thenReturn(List.of());
    }

    private void givenDosObjetivosEnElMismoNegocio() {
        Objetivo otro = objetivo();
        otro.setId(OTRO_OBJETIVO_ID);
        when(objetivoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(objetivo(), otro));
    }

    private void givenRespuesta(HizoManual hizo, QuePaso quePaso, String comentario) {
        when(respuestaMiniObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID)).thenReturn(
                List.of(new RespuestaMiniObjetivo(RespuestaMiniObjetivoId.nuevo(), NEGOCIO_ID, OBJETIVO_ID,
                        OTRO_MINI_ID, hizo, quePaso, comentario, SEMANA, AHORA)));
    }

    private void givenIaDisponible() {
        when(consumoSamiRepository.reservar(NEGOCIO_ID, DIA_DEL_CIERRE, LIMITE_DIARIO_IA, AHORA_CIERRE))
                .thenReturn(true);
    }

    private void givenIaDisponibleQueRedacta() {
        givenIaDisponible();
        when(renovacionManualesIaPort.renovar(any())).thenReturn(new RedaccionObjetivosIa(
                List.of(new TextoMiniObjetivo("Hablá con 2 vecinas más", "Con más charlas sabés qué piden.")),
                CONSUMO));
    }

    private void givenIaDisponibleQueRedactaUnTextoConImporte() {
        givenIaDisponible();
        when(renovacionManualesIaPort.renovar(any())).thenReturn(new RedaccionObjetivosIa(
                List.of(new TextoMiniObjetivo("Cobrá $5.000 más", "Porque sí")), CONSUMO));
    }

    private void givenIaDisponibleQueFalla() {
        givenIaDisponible();
        when(renovacionManualesIaPort.renovar(any())).thenThrow(new IllegalStateException("sin red"));
    }

    private void givenIaDisponibleQueDevuelveMenosTextos() {
        givenIaDisponible();
        when(renovacionManualesIaPort.renovar(any())).thenReturn(new RedaccionObjetivosIa(List.of(), CONSUMO));
    }

    private void givenUnNegocioSinAccesoALaIa() {
        doThrow(new RestriccionPlanException("IA_NO_HABILITADA", "No habilitada"))
                .when(controlPlanService).exigirIa(NEGOCIO_ID);
    }

    private void givenElLimiteDiarioDeIaAgotado() {
        when(consumoSamiRepository.reservar(NEGOCIO_ID, DIA_DEL_CIERRE, LIMITE_DIARIO_IA, AHORA_CIERRE))
                .thenReturn(false);
    }

    // --- act ---
    private void whenCerrar() {
        cerrarSemanasObjetivos.execute();
    }

    // --- assert ---
    private void thenSeCierraLaSemanaConElResultadoDelDomingoYSusDecisiones() {
        ResumenSemanal resumen = resumenGuardado();
        assertThat(resumen.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(resumen.semanaInicio()).isEqualTo(SEMANA);
        assertThat(resumen.resultado()).isEqualByComparingTo("180000");
        assertThat(resumen.avance()).isEqualByComparingTo("0.45");
        assertThat(resumen.decisiones()).isEqualTo(new ConteoDecisiones(0, 0, 1, 0));
        assertThat(resumen.minis()).containsExactly(new CierreMini(MINI_ID, SIN_LLEGAR));
        assertThat(resumen.queFunciono()).startsWith("Al cerrar la semana");
        assertThat(resumen.cerradoEn()).isEqualTo(AHORA_CIERRE);
        assertThat(resumen.reajustes()).isEmpty();
        verify(propuestaMiniObjetivosService).medidor(NEGOCIO_ID, DOMINGO);
        verifyNoInteractions(avisoRepository);
    }

    private void thenElMiniLogradoSigueConUnaMetaMasAlta() {
        ReajusteMiniObjetivo reajuste = unicoReajuste();
        MiniObjetivo nuevo = nuevoGuardado();
        assertThat(reajuste.tipo()).isEqualTo(TipoReajuste.SUBIR_META);
        assertThat(reajuste.motivo()).isEqualTo("Lo lograste: llegaste a $15.000 por semana. Ahora el objetivo es "
                + "$16.583 por semana.");
        assertThat(nuevo.getMeta()).isEqualByComparingTo("16583.00");
        assertThat(nuevo.getValorInicial()).isEqualByComparingTo("15000");
        assertThat(nuevo.getSemanaInicio()).isEqualTo(SEMANA.plusWeeks(1));
        assertThat(nuevo.getPosicion()).isEqualTo(1);
        assertThat(miniAnterior.getEstado()).isEqualTo(EstadoMiniObjetivo.LOGRADO);
        assertThat(miniAnterior.getReemplazadoPor()).isEqualTo(nuevo.getId());
    }

    private void thenSeGuardaElAnteriorAntesQueElNuevo() {
        InOrder orden = inOrder(miniObjetivoRepository);
        orden.verify(miniObjetivoRepository).save(miniAnterior);
        orden.verify(miniObjetivoRepository).save(any(MiniObjetivo.class));
    }

    private void thenElMiniSinAvanceSeReemplazaPorElMejorCandidato() {
        ReajusteMiniObjetivo reajuste = unicoReajuste();
        assertThat(reajuste.tipo()).isEqualTo(TipoReajuste.REEMPLAZAR);
        assertThat(reajuste.motivo()).startsWith("Hace 2 semanas que");
        assertThat(nuevoGuardado().getTipo()).isEqualTo(TipoMiniObjetivo.VENTAS_SEMANALES);
        assertThat(miniAnterior.getEstado()).isEqualTo(EstadoMiniObjetivo.REEMPLAZADO);
        assertThat(resumenGuardado().queFunciono()).startsWith("Esta semana tu resultado subió $5.000");
    }

    private void thenElMiniSeMantieneSinReajustes() {
        assertThat(resumenGuardado().reajustes()).isEmpty();
        verify(miniObjetivoRepository, never()).save(any());
    }

    private void thenSeProbaOtroCaminoConUnCandidatoDeOtroTipo() {
        assertThat(unicoReajuste().tipo()).isEqualTo(TipoReajuste.PROBAR_OTRO_CAMINO);
        assertThat(nuevoGuardado().getTipo()).isEqualTo(TipoMiniObjetivo.VENTAS_SEMANALES);
    }

    private void thenSeConsultaElAvisoAplicadoSinCrearAvisos() {
        verify(avisoRepository).findAllByIdsAndNegocioId(List.of(AVISO_ID), NEGOCIO_ID);
        verify(avisoRepository, never()).save(any());
    }

    private void thenSeCambiaElMiniCuyoProductoYaNoEstaYNoEntraEnElCierre() {
        assertThat(unicoReajuste().motivo()).startsWith("«Yerba 1 kg» ya no está en tu cuaderno");
        assertThat(resumenGuardado().minis()).isEmpty();
    }

    private void thenSeCompletaElLugarDelQuitadoYConservaSuEstado() {
        assertThat(unicoReajuste().motivo()).startsWith("Quitaste");
        assertThat(miniAnterior.getEstado()).isEqualTo(EstadoMiniObjetivo.ABANDONADO);
        assertThat(miniAnterior.getReemplazadoPor()).isEqualTo(nuevoGuardado().getId());
    }

    private void thenSeCompletaElSegundoLugar() {
        assertThat(unicoReajuste().motivo()).startsWith("Quitaste");
        assertThat(nuevoGuardado().getPosicion()).isEqualTo(2);
    }

    private void thenElLogradoNoSeToca() {
        assertThat(resumenGuardado().reajustes()).isEmpty();
        assertThat(miniAnterior.getReemplazadoPor()).isNull();
    }

    private void thenElLogradoSinTechoDejaSuLugarAlMejorCandidato() {
        ReajusteMiniObjetivo reajuste = unicoReajuste();
        assertThat(reajuste.tipo()).isEqualTo(TipoReajuste.REEMPLAZAR);
        assertThat(reajuste.motivo()).startsWith("Lo lograste: llegaste a 60 %. Como ya no se puede pedir más");
        assertThat(miniAnterior.getEstado()).isEqualTo(EstadoMiniObjetivo.LOGRADO);
        assertThat(miniAnterior.getReemplazadoPor()).isEqualTo(nuevoGuardado().getId());
    }

    private void thenNoSeReajustaNadaNiSePidenCandidatos() {
        assertThat(resumenGuardado().reajustes()).isEmpty();
        assertThat(resumenGuardado().queFunciono()).contains("¡Llegaste a tu meta!");
        verify(propuestaMiniObjetivosService, never()).candidatos(any(), any(MedidorMiniObjetivo.class));
    }

    private void thenElResumenFelicitaSinReajustar() {
        assertThat(resumenGuardado().reajustes()).isEmpty();
        assertThat(resumenGuardado().queFunciono()).contains("¡Llegaste a tu meta!");
    }

    private void thenNoSeReajustaNadaNiSeGuardaNingunMini() {
        assertThat(resumenGuardado().reajustes()).isEmpty();
        verify(miniObjetivoRepository, never()).save(any());
    }

    private void thenSeRecuperanLasTresSemanasMasNuevasConLoMedidoASuDomingo() {
        verify(resumenSemanalRepository, times(3)).save(any());
        verify(propuestaMiniObjetivosService).medidor(NEGOCIO_ID, DOMINGO.minusWeeks(2));
        verify(propuestaMiniObjetivosService).medidor(NEGOCIO_ID, DOMINGO.minusWeeks(1));
        verify(propuestaMiniObjetivosService).medidor(NEGOCIO_ID, DOMINGO);
    }

    private void thenSeCierraLaSemana(LocalDate semana) {
        assertThat(resumenGuardado().semanaInicio()).isEqualTo(semana);
    }

    private void thenNoSeCierraNiSeMideNada() {
        verify(resumenSemanalRepository, never()).save(any());
        verify(propuestaMiniObjetivosService, never()).medidor(any(), any());
    }

    private void thenSeIntentanCerrarDosSemanas() {
        verify(resumenSemanalRepository, times(2)).save(any());
    }

    private void thenSeCierraLaSemanaDelNegocioQueFunciona() {
        verify(resumenSemanalRepository).save(any());
    }

    private void thenSePideLaSegundaTandaYSeCierraLaSemana() {
        verify(objetivoRepository).findNegocioIdsConObjetivos(1, 1);
        verify(resumenSemanalRepository).save(any());
    }

    private void thenSeCierranLosDosObjetivosConUnaSolaMedicion() {
        verify(resumenSemanalRepository, times(2)).save(any());
        verify(propuestaMiniObjetivosService, times(1)).medidor(NEGOCIO_ID, DOMINGO);
    }

    private void thenElManualAbandonadoQuedaIntactoYNoSumaAlCierre() {
        assertThat(resumenGuardado().minis()).containsExactly(new CierreMini(MINI_ID, SIN_LLEGAR));
        assertThat(resumenGuardado().reajustes()).isEmpty();
        assertThat(miniAnterior.getReemplazadoPor()).isNull();
        verify(miniObjetivoRepository, never()).save(miniAnterior);
    }

    private void thenSeCierraSinAvanceNiCuantoFalta() {
        ResumenSemanal resumen = resumenGuardado();
        assertThat(resumen.avance()).isEqualByComparingTo("0");
        assertThat(resumen.queFunciono()).doesNotContain("Te faltan").contains("de la semana que viene.");
    }

    private void thenElManualQuedaLogradoYLoSigueUnoNuevo() {
        MiniObjetivo nuevo = nuevoGuardado();
        ReajusteMiniObjetivo reajuste = unicoReajuste();
        assertThat(miniAnterior.getEstado()).isEqualTo(EstadoMiniObjetivo.LOGRADO);
        assertThat(miniAnterior.getReemplazadoPor()).isEqualTo(nuevo.getId());
        assertThat(nuevo.esManual()).isTrue();
        assertThat(nuevo.getPosicion()).isEqualTo(miniAnterior.getPosicion());
        assertThat(nuevo.getSemanaInicio()).isEqualTo(SEMANA.plusWeeks(1));
        assertThat(nuevo.getMeta()).isNull();
        assertThat(reajuste.metaAnterior()).isNull();
        assertThat(reajuste.metaNueva()).isNull();
        assertThat(reajuste.tituloNuevo()).isEqualTo(nuevo.getTitulo());
    }

    private void thenElNuevoManualEsUnPasoMas() {
        MiniObjetivo nuevo = nuevoGuardado();
        ReajusteMiniObjetivo reajuste = unicoReajuste();
        assertThat(nuevo.getTitulo()).isEqualTo("Un paso más: " + TITULO_MANUAL);
        assertThat(nuevo.isTextoConIa()).isFalse();
        assertThat(reajuste.tipo()).isEqualTo(TipoReajuste.SUBIR_META);
        assertThat(reajuste.motivo()).startsWith("Hiciste «" + TITULO_MANUAL + "» y te funcionó");
    }

    private void thenElManualQuedaReemplazadoPorLoQueFalto() {
        assertThat(miniAnterior.getEstado()).isEqualTo(EstadoMiniObjetivo.REEMPLAZADO);
        assertThat(nuevoGuardado().getTitulo()).isEqualTo("Lo que faltó: " + TITULO_MANUAL);
        assertThat(unicoReajuste().tipo()).isEqualTo(TipoReajuste.REEMPLAZAR);
    }

    private void thenElManualSeSimplifica() {
        assertThat(nuevoGuardado().getTitulo()).isEqualTo("Versión corta: " + TITULO_MANUAL);
        assertThat(unicoReajuste().tipo()).isEqualTo(TipoReajuste.REEMPLAZAR);
    }

    private void thenElManualQuedaLogradoYCambiaPorOtraAccion() {
        assertThat(miniAnterior.getEstado()).isEqualTo(EstadoMiniObjetivo.LOGRADO);
        assertThat(nuevoGuardado().getTitulo()).isEqualTo("Probá otra acción para tu objetivo");
        assertThat(unicoReajuste().tipo()).isEqualTo(TipoReajuste.PROBAR_OTRO_CAMINO);
    }

    private void thenElResumenCuentaLoRespondidoDeLosManuales() {
        ResumenSemanal resumen = resumenGuardado();
        assertThat(resumen.manuales()).containsExactly(
                new ManualCierre(OTRO_MINI_ID, TITULO_MANUAL, HizoManual.SI, QuePaso.FUNCIONO));
        assertThat(resumen.queFunciono()).contains("De tus acciones respondiste: 1 hechas, 0 en parte y 0 sin hacer.");
    }

    private void thenElManualSinRespuestaSigueIgual() {
        assertThat(miniAnterior.estaEnCurso()).isTrue();
        assertThat(miniAnterior.getReemplazadoPor()).isNull();
        assertThat(resumenGuardado().reajustes()).isEmpty();
        verify(miniObjetivoRepository, never()).save(any());
    }

    private void thenElResumenDiceQueElManualSigueIgualHastaQueResponda() {
        ResumenSemanal resumen = resumenGuardado();
        assertThat(resumen.manuales()).containsExactly(
                new ManualCierre(OTRO_MINI_ID, TITULO_MANUAL, null, null));
        assertThat(resumen.queFunciono()).contains("No me contaste cómo te fue con «" + TITULO_MANUAL
                + "»: queda igual hasta que respondas.");
    }

    private void thenSeReajustanLosMedidosYSeRenuevanLosManuales() {
        ResumenSemanal resumen = resumenGuardado();
        assertThat(resumen.reajustes()).extracting(ReajusteMiniObjetivo::tipo)
                .containsExactly(TipoReajuste.SUBIR_META, TipoReajuste.REEMPLAZAR);
        assertThat(resumen.minis()).extracting(CierreMini::miniObjetivoId).containsExactly(MINI_ID);
        verify(miniObjetivoRepository, times(4)).save(any(MiniObjetivo.class));
    }

    private void thenSeRenuevaElManualConUnPasoMas() {
        assertThat(unicoReajuste().tipo()).isEqualTo(TipoReajuste.SUBIR_META);
    }

    private void thenElManualDeLaSemanaSiguienteNoEntraNiSeRenueva() {
        assertThat(resumenGuardado().manuales()).isEmpty();
        assertThat(miniAnterior.estaEnCurso()).isTrue();
        verify(miniObjetivoRepository, never()).save(any());
    }

    private void thenElResumenCuentaElManualPeroNoLoRenueva() {
        assertThat(resumenGuardado().manuales()).hasSize(1);
        assertThat(resumenGuardado().reajustes()).isEmpty();
        assertThat(miniAnterior.estaEnCurso()).isTrue();
    }

    private void thenElManualSeRenuevaUnaSolaVezEnDosSemanas() {
        verify(resumenSemanalRepository, times(2)).save(any(ResumenSemanal.class));
        verify(miniObjetivoRepository, times(2)).save(any(MiniObjetivo.class));
        assertThat(miniAnterior.getEstado()).isEqualTo(EstadoMiniObjetivo.LOGRADO);
    }

    private void thenLaAccionNuevaLaRedactaLaIa() {
        MiniObjetivo nuevo = nuevoGuardado();
        assertThat(nuevo.getTitulo()).isEqualTo("Hablá con 2 vecinas más");
        assertThat(nuevo.getJustificacion()).isEqualTo("Con más charlas sabés qué piden.");
        assertThat(nuevo.isTextoConIa()).isTrue();
    }

    private void thenLaIaRecibeElPedidoSinDatosPersonales() {
        ArgumentCaptor<List<PedidoRenovacion>> pedidos = ArgumentCaptor.captor();
        verify(renovacionManualesIaPort).renovar(pedidos.capture());
        assertThat(pedidos.getValue()).singleElement().satisfies(pedido -> {
            assertThat(pedido.accion()).isEqualTo(AccionRenovacion.SE_DIVIDE);
            assertThat(pedido.titulo()).isEqualTo(TITULO_MANUAL);
            assertThat(pedido.comentario()).isEqualTo("Le mandé un mensaje a [dato oculto]");
        });
    }

    private void thenSeRegistraElConsumoDeLaIa() {
        verify(consumoSamiRepository).registrar(eq(NEGOCIO_ID), any(), eq(CONSUMO), eq(AHORA_CIERRE));
    }

    private void thenSeUsaElTextoDeReserva() {
        MiniObjetivo nuevo = nuevoGuardado();
        assertThat(nuevo.getTitulo()).isEqualTo("Versión corta: " + TITULO_MANUAL);
        assertThat(nuevo.isTextoConIa()).isFalse();
    }

    private void thenNoSeRegistraConsumo() {
        verify(consumoSamiRepository, never()).registrar(any(), any(), any(), any());
    }

    private void thenSeUsaElTextoDeReservaSinConsultarLaIa() {
        assertThat(nuevoGuardado().getTitulo()).isEqualTo("Versión corta: " + TITULO_MANUAL);
        verifyNoInteractions(renovacionManualesIaPort, consumoSamiRepository);
    }

    private void thenSeUsaElTextoDeReservaSinPedirleNadaALaIa() {
        assertThat(nuevoGuardado().getTitulo()).isEqualTo("Versión corta: " + TITULO_MANUAL);
        verifyNoInteractions(renovacionManualesIaPort);
    }

    private void thenNoSeConsultaLaIaNiSuCuota() {
        verifyNoInteractions(renovacionManualesIaPort, controlPlanService, consumoSamiRepository);
    }

    private void thenNoSePideNadaALaIa() {
        verifyNoInteractions(renovacionManualesIaPort);
    }

    // --- helpers ---
    private ResumenSemanal resumenGuardado() {
        ArgumentCaptor<ResumenSemanal> guardado = ArgumentCaptor.forClass(ResumenSemanal.class);
        verify(resumenSemanalRepository).save(guardado.capture());
        return guardado.getValue();
    }

    private ReajusteMiniObjetivo unicoReajuste() {
        List<ReajusteMiniObjetivo> reajustes = resumenGuardado().reajustes();
        assertThat(reajustes).hasSize(1);
        return reajustes.get(0);
    }

    private MiniObjetivo nuevoGuardado() {
        ArgumentCaptor<MiniObjetivo> guardados = ArgumentCaptor.forClass(MiniObjetivo.class);
        verify(miniObjetivoRepository, times(2)).save(guardados.capture());
        MiniObjetivo nuevo = guardados.getAllValues().get(1);
        assertThat(nuevo.getEstado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
        return nuevo;
    }

    /** Uno en curso que empezó dos semanas antes de la que se cierra: ya se le pueden aplicar todas las reglas. */
    private static MiniObjetivo deSemanasAtras() {
        MiniObjetivo mini = miniObjetivo();
        mini.setSemanaInicio(SEMANA.minusWeeks(2));
        return mini;
    }

    private static MiniObjetivo miniConEstado(EstadoMiniObjetivo estado, MiniObjetivoId id) {
        MiniObjetivo mini = miniObjetivo();
        mini.setEstado(estado);
        mini.setId(id);
        return mini;
    }

    private static MiniObjetivo quitadoEnElSegundoLugar() {
        MiniObjetivo quitado = miniConEstado(EstadoMiniObjetivo.ABANDONADO, OTRO_MINI_ID);
        quitado.setPosicion(2);
        return quitado;
    }

    private static MiniObjetivo margenLogradoEnSuTope() {
        MiniObjetivo margen = miniConEstado(EstadoMiniObjetivo.LOGRADO, MINI_ID);
        margen.setTipo(TipoMiniObjetivo.MEJORAR_MARGEN);
        margen.setRegistroId(null);
        margen.setReferencia(null);
        margen.setValorInicial(new BigDecimal("58"));
        margen.setMeta(new BigDecimal("60"));
        margen.setValorActual(new BigDecimal("60"));
        return margen;
    }

    private static MiniObjetivo miniManualAbandonado() {
        MiniObjetivo manual = miniManual();
        manual.abandonar(AHORA);
        return manual;
    }

    private static MiniObjetivo miniManualDeLaSemanaSiguiente() {
        MiniObjetivo manual = miniManual();
        manual.setSemanaInicio(SEMANA.plusWeeks(1));
        return manual;
    }

    private static CandidatoMiniObjetivo candidatoDeOtroProducto() {
        return new CandidatoMiniObjetivo(TipoMiniObjetivo.SUBIR_PRECIO, OTRO_PRODUCTO_ID, "Aceite",
                BigDecimal.ONE, BigDecimal.TEN, new BigDecimal("9500"), "Subile al aceite", "x", false);
    }

    private static Objetivo creadoHaceSemanas(int semanas) {
        Objetivo objetivo = objetivo();
        objetivo.setCreadoEn(AHORA.minusWeeks(semanas));
        return objetivo;
    }

    private static Objetivo objetivoYaAlcanzado() {
        Objetivo alcanzado = objetivo();
        alcanzado.marcarAlcanzado(AHORA);
        return alcanzado;
    }

    private static DecisionObjetivo decisionDeOtraSemana() {
        return new DecisionObjetivo(DecisionObjetivoId.nuevo(), NEGOCIO_ID, OBJETIVO_ID, MINI_ID,
                TipoDecision.AVISO_DESCARTADO, null, PRODUCTO_ID, "Otro aviso", AHORA.minusWeeks(1),
                SEMANA.minusWeeks(1));
    }

    private static ResumenSemanal resumen(LocalDate semana, String resultado, BigDecimal valorMini) {
        return new ResumenSemanal(ResumenSemanalId.nuevo(), OBJETIVO_ID, semana, new BigDecimal(resultado),
                BigDecimal.ZERO, new ConteoDecisiones(0, 0, 0, 0), new ArrayList<>(List.of(
                        new CierreMini(MINI_ID, valorMini))), "", false, AHORA, List.of(), List.of());
    }
}
