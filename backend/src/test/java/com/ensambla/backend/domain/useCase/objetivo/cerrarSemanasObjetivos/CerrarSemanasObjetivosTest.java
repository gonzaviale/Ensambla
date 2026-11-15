package com.ensambla.backend.domain.useCase.objetivo.cerrarSemanasObjetivos;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.ImpactoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.CandidatoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.CierreMini;
import com.ensambla.backend.domain.model.objetivo.ConteoDecisiones;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivo;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivoId;
import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MedidorMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ReajusteMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.ResumenSemanal;
import com.ensambla.backend.domain.model.objetivo.ResumenSemanalId;
import com.ensambla.backend.domain.model.objetivo.TipoDecision;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoReajuste;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.objetivo.DecisionObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ResumenSemanalRepository;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
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
import static com.ensambla.backend.support.MiniObjetivoTestData.candidatoDelNegocio;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    private static final NegocioId OTRO_NEGOCIO_ID = NegocioId.de("00000000-0000-4000-8000-0000000000aa");
    private static final RegistroId OTRO_PRODUCTO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");
    /** Por debajo de la meta del mini-objetivo de prueba ($14.183). */
    private static final BigDecimal SIN_LLEGAR = new BigDecimal("13000");
    private static final CandidatoMiniObjetivo VENTAS = candidatoDelNegocio(TipoMiniObjetivo.VENTAS_SEMANALES, "9000");

    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class);
    private final MiniObjetivoRepository miniObjetivoRepository = mock(MiniObjetivoRepository.class);
    private final DecisionObjetivoRepository decisionObjetivoRepository = mock(DecisionObjetivoRepository.class);
    private final AvisoRepository avisoRepository = mock(AvisoRepository.class);
    private final ResumenSemanalRepository resumenSemanalRepository = mock(ResumenSemanalRepository.class);
    private final PropuestaMiniObjetivosService propuestaMiniObjetivosService =
            mock(PropuestaMiniObjetivosService.class);
    private final MedidorMiniObjetivo medidor = mock(MedidorMiniObjetivo.class);
    private CerrarSemanasObjetivos cerrarSemanasObjetivos;

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
        MiniObjetivo reemplazado = conEstado(EstadoMiniObjetivo.REEMPLAZADO);
        reemplazado.setId(OTRO_MINI_ID);
        givenMinis(miniObjetivo(), reemplazado);
        givenValor(SIN_LLEGAR);
        givenDecisiones(decision(TipoDecision.PASO_ACEPTADO, null), decisionDeOtraSemana());

        whenCerrar();

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

    @Test
    @DisplayName("Un mini-objetivo que llegó a su meta sigue con una meta más alta desde la semana siguiente")
    void executeShouldRaiseTheGoalOfAnAchievedMiniObjetivo() {
        MiniObjetivo mini = miniObjetivo();
        givenMinis(mini);
        givenValor(new BigDecimal("15000"));

        whenCerrar();

        ReajusteMiniObjetivo reajuste = unicoReajuste();
        MiniObjetivo nuevo = nuevoGuardado();
        assertThat(reajuste.tipo()).isEqualTo(TipoReajuste.SUBIR_META);
        assertThat(reajuste.motivo()).isEqualTo("Lo lograste: llegaste a $15.000 por semana. Ahora el objetivo es "
                + "$16.583 por semana.");
        assertThat(nuevo.getMeta()).isEqualByComparingTo("16583.00");
        assertThat(nuevo.getValorInicial()).isEqualByComparingTo("15000");
        assertThat(nuevo.getSemanaInicio()).isEqualTo(SEMANA.plusWeeks(1));
        assertThat(nuevo.getPosicion()).isEqualTo(1);
        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.LOGRADO);
        assertThat(mini.getReemplazadoPor()).isEqualTo(nuevo.getId());
        thenSavedTheOldBeforeTheNew(mini);
    }

    @Test
    @DisplayName("Uno que no avanzó respecto de dos cierres atrás se cambia por el mejor candidato")
    void executeShouldReplaceAMiniObjetivoWithoutProgressInTwoCloses() {
        MiniObjetivo mini = deSemanasAtras();
        givenMinis(mini);
        givenValor(SIN_LLEGAR);
        givenResumenes(resumen(SEMANA.minusWeeks(1), "175000", SIN_LLEGAR),
                resumen(SEMANA.minusWeeks(2), "170000", SIN_LLEGAR));
        givenCandidatos(VENTAS);

        whenCerrar();

        ReajusteMiniObjetivo reajuste = unicoReajuste();
        assertThat(reajuste.tipo()).isEqualTo(TipoReajuste.REEMPLAZAR);
        assertThat(reajuste.motivo()).startsWith("Hace 2 semanas que");
        assertThat(nuevoGuardado().getTipo()).isEqualTo(TipoMiniObjetivo.VENTAS_SEMANALES);
        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.REEMPLAZADO);
        assertThat(resumenGuardado().queFunciono()).startsWith("Esta semana tu resultado subió $5.000");
        thenSavedTheOldBeforeTheNew(mini);
    }

    @Test
    @DisplayName("Sin un candidato para reemplazarlo, el mini-objetivo se mantiene")
    void executeShouldKeepTheMiniObjetivoWhenThereIsNoCandidate() {
        givenMinis(deSemanasAtras());
        givenValor(SIN_LLEGAR);
        givenResumenes(resumen(SEMANA.minusWeeks(1), "175000", SIN_LLEGAR),
                resumen(SEMANA.minusWeeks(2), "170000", SIN_LLEGAR));
        givenCandidatos();

        whenCerrar();

        assertThat(resumenGuardado().reajustes()).isEmpty();
        verify(miniObjetivoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Si un aviso aplicado no movió la venta, prueba con un candidato de otro tipo, sin crear avisos")
    void executeShouldTryAnotherPathWhenAnAppliedAvisoHadNoEffect() {
        givenMinis(deSemanasAtras());
        givenValor(SIN_LLEGAR);
        givenDecisiones(decision(TipoDecision.AVISO_APLICADO, AVISO_ID));
        givenAvisoSinEfecto();
        givenCandidatos(new CandidatoMiniObjetivo(TipoMiniObjetivo.SUBIR_PRECIO, OTRO_PRODUCTO_ID, "Aceite",
                BigDecimal.ONE, BigDecimal.TEN, new BigDecimal("9500"), "Subile al aceite", "x", false), VENTAS);

        whenCerrar();

        assertThat(unicoReajuste().tipo()).isEqualTo(TipoReajuste.PROBAR_OTRO_CAMINO);
        assertThat(nuevoGuardado().getTipo()).isEqualTo(TipoMiniObjetivo.VENTAS_SEMANALES);
        verify(avisoRepository).findAllByIdsAndNegocioId(List.of(AVISO_ID), NEGOCIO_ID);
        verify(avisoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Uno cuyo producto ya no está en el cuaderno se cambia y no entra en el cierre")
    void executeShouldReplaceAMiniObjetivoWhoseProductIsGone() {
        givenMinis(miniObjetivo());
        givenCandidatos(VENTAS);

        whenCerrar();

        assertThat(unicoReajuste().motivo()).startsWith("«Yerba 1 kg» ya no está en tu cuaderno");
        assertThat(resumenGuardado().minis()).isEmpty();
    }

    @Test
    @DisplayName("El lugar de uno quitado se completa y el quitado conserva su estado")
    void executeShouldFillThePlaceOfARemovedMiniObjetivo() {
        MiniObjetivo quitado = conEstado(EstadoMiniObjetivo.ABANDONADO);
        givenMinis(quitado);
        givenCandidatos(VENTAS);

        whenCerrar();

        assertThat(unicoReajuste().motivo()).startsWith("Quitaste");
        assertThat(quitado.getEstado()).isEqualTo(EstadoMiniObjetivo.ABANDONADO);
        assertThat(quitado.getReemplazadoPor()).isEqualTo(nuevoGuardado().getId());
    }

    @Test
    @DisplayName("El lugar de uno quitado se completa aunque haya otro en curso en otro lugar")
    void executeShouldFillThePlaceOfARemovedMiniObjetivoWhenAnotherPlaceIsTaken() {
        MiniObjetivo quitado = conEstado(EstadoMiniObjetivo.ABANDONADO);
        quitado.setId(OTRO_MINI_ID);
        quitado.setPosicion(2);
        givenMinis(quitado, miniObjetivo());
        givenValor(SIN_LLEGAR);
        givenCandidatos(VENTAS);

        whenCerrar();

        assertThat(unicoReajuste().motivo()).startsWith("Quitaste");
        assertThat(nuevoGuardado().getPosicion()).isEqualTo(2);
    }

    @Test
    @DisplayName("Uno logrado cuyo lugar ya ocupa otro en curso no se toca")
    void executeShouldNotTouchAnAchievedMiniObjetivoWhosePlaceIsTaken() {
        MiniObjetivo logrado = conEstado(EstadoMiniObjetivo.LOGRADO);
        logrado.setId(OTRO_MINI_ID);
        givenMinis(logrado, miniObjetivo());
        givenValor(SIN_LLEGAR);

        whenCerrar();

        assertThat(resumenGuardado().reajustes()).isEmpty();
        assertThat(logrado.getReemplazadoPor()).isNull();
    }

    @Test
    @DisplayName("Uno logrado que ya no puede pedir más deja su lugar al mejor candidato")
    void executeShouldReplaceAnAchievedMiniObjetivoThatCannotAskForMore() {
        MiniObjetivo margen = conEstado(EstadoMiniObjetivo.LOGRADO);
        margen.setTipo(TipoMiniObjetivo.MEJORAR_MARGEN);
        margen.setRegistroId(null);
        margen.setReferencia(null);
        margen.setValorInicial(new BigDecimal("58"));
        margen.setMeta(new BigDecimal("60"));
        margen.setValorActual(new BigDecimal("60"));
        givenMinis(margen);
        givenCandidatos(VENTAS);

        whenCerrar();

        ReajusteMiniObjetivo reajuste = unicoReajuste();
        assertThat(reajuste.tipo()).isEqualTo(TipoReajuste.REEMPLAZAR);
        assertThat(reajuste.motivo()).startsWith("Lo lograste: llegaste a 60 %. Como ya no se puede pedir más");
        assertThat(margen.getEstado()).isEqualTo(EstadoMiniObjetivo.LOGRADO);
        assertThat(margen.getReemplazadoPor()).isEqualTo(nuevoGuardado().getId());
    }

    @Test
    @DisplayName("Con el objetivo alcanzado no se reajusta nada")
    void executeShouldNotAdjustWhenTheObjetivoIsReached() {
        givenMinis(miniObjetivo());
        givenValor(new BigDecimal("15000"));
        when(medidor.resultado()).thenReturn(new BigDecimal("400000"));

        whenCerrar();

        assertThat(resumenGuardado().reajustes()).isEmpty();
        assertThat(resumenGuardado().queFunciono()).contains("¡Llegaste a tu meta!");
        verify(propuestaMiniObjetivosService, never()).candidatos(any(), any(MedidorMiniObjetivo.class));
    }

    @Test
    @DisplayName("Un objetivo que ya estaba alcanzado no se reajusta aunque el resultado haya bajado")
    void executeShouldNotAdjustWhenTheObjetivoWasAlreadyReached() {
        Objetivo alcanzado = objetivo();
        alcanzado.marcarAlcanzado(AHORA);
        givenObjetivo(alcanzado);
        givenMinis(miniObjetivo());
        givenValor(new BigDecimal("15000"));

        whenCerrar();

        assertThat(resumenGuardado().reajustes()).isEmpty();
        assertThat(resumenGuardado().queFunciono()).contains("¡Llegaste a tu meta!");
    }

    @Test
    @DisplayName("En una semana sin ventas no se reajusta nada")
    void executeShouldNotAdjustWhenThereWereNoSales() {
        givenMinis(miniObjetivo());
        givenValor(new BigDecimal("15000"));
        when(medidor.ventasSemana()).thenReturn(BigDecimal.ZERO);

        whenCerrar();

        assertThat(resumenGuardado().reajustes()).isEmpty();
        verify(miniObjetivoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Recupera como mucho las semanas pendientes más nuevas, cada una con lo medido a su domingo")
    void executeShouldRecoverAtMostTheNewestPendingWeeks() {
        cerrarSemanasObjetivos = cierre(3, 50);
        givenObjetivo(creadoHaceSemanas(20));

        whenCerrar();

        verify(resumenSemanalRepository, times(3)).save(any());
        verify(propuestaMiniObjetivosService).medidor(NEGOCIO_ID, DOMINGO.minusWeeks(2));
        verify(propuestaMiniObjetivosService).medidor(NEGOCIO_ID, DOMINGO.minusWeeks(1));
        verify(propuestaMiniObjetivosService).medidor(NEGOCIO_ID, DOMINGO);
    }

    @Test
    @DisplayName("Sigue desde la semana siguiente a la última cerrada")
    void executeShouldContinueFromTheWeekAfterTheLastClosed() {
        givenObjetivo(creadoHaceSemanas(20));
        givenResumenes(resumen(SEMANA.minusWeeks(1), "175000", SIN_LLEGAR));

        whenCerrar();

        assertThat(resumenGuardado().semanaInicio()).isEqualTo(SEMANA);
    }

    @Test
    @DisplayName("Un cierre anterior a la creación del objetivo no adelanta el comienzo")
    void executeShouldStartFromTheCreationWeekWhenTheLastCloseIsOlder() {
        givenResumenes(resumen(SEMANA.minusWeeks(5), "175000", SIN_LLEGAR));

        whenCerrar();

        assertThat(resumenGuardado().semanaInicio()).isEqualTo(SEMANA);
    }

    @Test
    @DisplayName("Si la última semana ya está cerrada no hace nada")
    void executeShouldDoNothingWhenTheLastWeekIsAlreadyClosed() {
        givenResumenes(resumen(SEMANA, "175000", SIN_LLEGAR));

        whenCerrar();

        verify(resumenSemanalRepository, never()).save(any());
        verify(propuestaMiniObjetivosService, never()).medidor(any(), any());
    }

    @Test
    @DisplayName("Si otra corrida ya cerró una semana, sigue con la siguiente")
    void executeShouldContinueWithTheNextWeekWhenAnotherRunAlreadyClosedOne() {
        givenObjetivo(creadoHaceSemanas(1));
        when(resumenSemanalRepository.save(any(ResumenSemanal.class)))
                .thenThrow(new DuplicateResourceException("Esa semana del objetivo ya está cerrada"))
                .thenAnswer(call -> call.getArgument(0));

        whenCerrar();

        verify(resumenSemanalRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("Un negocio con error no frena el cierre de los demás")
    void executeShouldContinueWithOtherBusinessesWhenOneFails() {
        when(objetivoRepository.findNegocioIdsConObjetivos(0, 50)).thenReturn(List.of(OTRO_NEGOCIO_ID, NEGOCIO_ID));
        when(objetivoRepository.findAllByNegocioId(OTRO_NEGOCIO_ID)).thenThrow(new IllegalStateException("caído"));

        whenCerrar();

        verify(resumenSemanalRepository).save(any());
    }

    @Test
    @DisplayName("Pide los negocios de a tandas hasta que una viene incompleta")
    void executeShouldRequestBusinessesInBatches() {
        cerrarSemanasObjetivos = cierre(8, 1);
        when(objetivoRepository.findNegocioIdsConObjetivos(0, 1)).thenReturn(List.of(NEGOCIO_ID));
        when(objetivoRepository.findNegocioIdsConObjetivos(1, 1)).thenReturn(List.of());

        whenCerrar();

        verify(objetivoRepository).findNegocioIdsConObjetivos(1, 1);
        verify(resumenSemanalRepository).save(any());
    }

    @Test
    @DisplayName("Los objetivos de un negocio comparten lo medido al domingo")
    void executeShouldShareTheMeasurementBetweenObjetivosOfTheSameBusiness() {
        Objetivo otro = objetivo();
        otro.setId(ObjetivoId.de("00000000-0000-4000-8000-0000000000bb"));
        when(objetivoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(objetivo(), otro));

        whenCerrar();

        verify(resumenSemanalRepository, times(2)).save(any());
        verify(propuestaMiniObjetivosService, times(1)).medidor(NEGOCIO_ID, DOMINGO);
    }

    // --- arrange ---
    private CerrarSemanasObjetivos cierre(int maxSemanasPendientes, int tamanoTanda) {
        return new CerrarSemanasObjetivos(objetivoRepository, miniObjetivoRepository, decisionObjetivoRepository,
                avisoRepository, resumenSemanalRepository, propuestaMiniObjetivosService, new TransaccionDirecta(),
                CLOCK, maxSemanasPendientes, tamanoTanda);
    }

    private void givenObjetivo(Objetivo objetivo) {
        when(objetivoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(objetivo));
    }

    private void givenMinis(MiniObjetivo... minis) {
        when(miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of(minis));
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

    private void givenCandidatos(CandidatoMiniObjetivo... candidatos) {
        when(propuestaMiniObjetivosService.candidatos(NEGOCIO_ID, medidor)).thenReturn(List.of(candidatos));
    }

    private void givenAvisoSinEfecto() {
        Aviso aplicado = aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO);
        aplicado.registrarImpacto(new ImpactoAviso(14, new BigDecimal("3.00"), new BigDecimal("2.00"),
                new BigDecimal("150.00"), new BigDecimal("100.00")), AHORA);
        when(avisoRepository.findAllByIdsAndNegocioId(List.of(AVISO_ID), NEGOCIO_ID)).thenReturn(List.of(aplicado));
    }

    // --- act ---
    private void whenCerrar() {
        cerrarSemanasObjetivos.execute();
    }

    // --- assert ---
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

    private void thenSavedTheOldBeforeTheNew(MiniObjetivo anterior) {
        InOrder orden = inOrder(miniObjetivoRepository);
        orden.verify(miniObjetivoRepository).save(anterior);
        orden.verify(miniObjetivoRepository).save(any(MiniObjetivo.class));
    }

    // --- helpers ---
    /** Uno en curso que empezó dos semanas antes de la que se cierra: ya se le pueden aplicar todas las reglas. */
    private static MiniObjetivo deSemanasAtras() {
        MiniObjetivo mini = miniObjetivo();
        mini.setSemanaInicio(SEMANA.minusWeeks(2));
        return mini;
    }

    private static MiniObjetivo conEstado(EstadoMiniObjetivo estado) {
        MiniObjetivo mini = miniObjetivo();
        mini.setEstado(estado);
        return mini;
    }

    private static Objetivo creadoHaceSemanas(int semanas) {
        Objetivo objetivo = objetivo();
        objetivo.setCreadoEn(AHORA.minusWeeks(semanas));
        return objetivo;
    }

    private static DecisionObjetivo decisionDeOtraSemana() {
        return new DecisionObjetivo(DecisionObjetivoId.nuevo(), NEGOCIO_ID, OBJETIVO_ID, MINI_ID,
                TipoDecision.AVISO_DESCARTADO, null, PRODUCTO_ID, "Otro aviso", AHORA.minusWeeks(1),
                SEMANA.minusWeeks(1));
    }

    private static ResumenSemanal resumen(LocalDate semana, String resultado, BigDecimal valorMini) {
        return new ResumenSemanal(ResumenSemanalId.nuevo(), OBJETIVO_ID, semana, new BigDecimal(resultado),
                BigDecimal.ZERO, new ConteoDecisiones(0, 0, 0, 0), new ArrayList<>(List.of(
                        new CierreMini(MINI_ID, valorMini))), "", false, AHORA, List.of());
    }
}
