package com.ensambla.backend.domain.useCase.objetivo.guardarObjetivoConSami;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.objetivo.CandidatosDelNegocio;
import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.ModoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoObjetivo;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.objetivo.ObjetivoItem;
import com.ensambla.backend.domain.useCase.objetivo.guardarObjetivoConSami.GuardarObjetivoConSamiRequest.MiniElegido;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static com.ensambla.backend.support.ConversacionObjetivoTestData.JUSTIFICACION_ACCION;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.OBJETIVO;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.TITULO_ACCION;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.catalogo;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.SEMANA;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GuardarObjetivoConSamiTest {

    private static final Clock CLOCK = Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC);
    private static final LocalDate HOY = LocalDate.of(2026, 11, 10);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class);
    private final MiniObjetivoRepository miniObjetivoRepository = mock(MiniObjetivoRepository.class);
    private final PropuestaMiniObjetivosService propuestaMiniObjetivosService =
            mock(PropuestaMiniObjetivosService.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final UsoPlanRepository usoPlanRepository = mock(UsoPlanRepository.class);
    private GuardarObjetivoConSami guardarObjetivoConSami;
    private ObjetivoItem objetivoGuardadoItem;
    private Throwable error;

    @BeforeEach
    void setUp() {
        guardarObjetivoConSami = new GuardarObjetivoConSami(
                negocioRepository, objetivoRepository, miniObjetivoRepository, propuestaMiniObjetivosService,
                controlPlanService, usoPlanRepository, new TransaccionDirecta(), CLOCK);
        dadoUnNegocioQueGuardaSusObjetivosYMinis();
    }

    @Test
    @DisplayName("Guarda un objetivo híbrido sin meta, armado con Sami, con el texto recortado")
    void guardaUnObjetivoHibridoSinMetaArmadoConSamiConElTextoRecortado() {
        cuandoSeGuarda(false, medido(TipoMiniObjetivo.VENTAS_SEMANALES, null, null), accion());

        entoncesSeGuardaUnObjetivoHibridoSinMetaArmadoConSami();
    }

    @Test
    @DisplayName("Devuelve el objetivo guardado")
    void devuelveElObjetivoGuardado() {
        cuandoSeGuarda(false, medido(TipoMiniObjetivo.VENTAS_SEMANALES, null, null), accion());

        entoncesSeDevuelveElObjetivoHibridoGuardado();
    }

    @Test
    @DisplayName("Los mini-objetivos entran en posiciones 1, 2… en la semana de hoy, después del objetivo")
    void losMiniObjetivosEntranEnPosiciones12EnLaSemanaDeHoyDespuesDelObjetivo() {
        cuandoSeGuarda(false, medido(TipoMiniObjetivo.VENTAS_SEMANALES, null, null), accion());

        entoncesLosMinisEntranEnOrdenDespuesDelObjetivo();
    }

    @Test
    @DisplayName("Los números de un mini-objetivo medido los pone el servidor con el catálogo de hoy")
    void losNumerosDeUnMiniObjetivoMedidoLosPoneElServidorConElCatalogoDeHoy() {
        cuandoSeGuarda(false, medido(TipoMiniObjetivo.SUBIR_PRECIO, PRODUCTO_ID, PRODUCTO));

        entoncesLosNumerosDelMiniSalenDelCatalogo();
    }

    @Test
    @DisplayName("Con sólo mini-objetivos medidos el objetivo es con datos")
    void conSoloMiniObjetivosMedidosElObjetivoEsConDatos() {
        cuandoSeGuarda(false, medido(TipoMiniObjetivo.VENTAS_SEMANALES, null, null),
                medido(TipoMiniObjetivo.MEJORAR_MARGEN, null, null));

        entoncesElObjetivoEs(TipoObjetivo.CON_DATOS);
    }

    @Test
    @DisplayName("Con sólo acciones el objetivo es sin datos y está armado con Sami")
    void conSoloAccionesElObjetivoEsSinDatosYEstaArmadoConSami() {
        cuandoSeGuarda(false, accion());

        entoncesElObjetivoEsSinDatosArmadoConSami();
    }

    @Test
    @DisplayName("Con sólo acciones no consulta el catálogo y el texto de la acción es de la IA")
    void conSoloAccionesNoConsultaElCatalogoYElTextoDeLaAccionEsDeLaIa() {
        cuandoSeGuarda(false, accion());

        entoncesNoSeConsultaElCatalogoYElTextoEsDeLaIa();
    }

    @Test
    @DisplayName("El seguimiento manual no cuenta como armado con Sami")
    void elSeguimientoManualNoCuentaComoArmadoConSami() {
        cuandoSeGuarda(true, medido(TipoMiniObjetivo.VENTAS_SEMANALES, null, null));

        entoncesElObjetivoEsSinDatosYNoArmadoConSami();
    }

    @Test
    @DisplayName("El seguimiento manual ignora los mini-objetivos recibidos y guarda un manual genérico")
    void elSeguimientoManualIgnoraLosMiniObjetivosRecibidosYGuardaUnManualGenerico() {
        cuandoSeGuarda(true, medido(TipoMiniObjetivo.VENTAS_SEMANALES, null, null));

        entoncesSeGuardaSoloElManualGenerico();
    }

    @Test
    @DisplayName("Sin mini-objetivos no se guarda nada")
    void sinMiniObjetivosNoSeGuardaNada() {
        cuandoSeGuarda(false);

        entoncesSeRechazaPorLaCantidadDeMinisSinGuardarNada();
    }

    @Test
    @DisplayName("Con más de 3 mini-objetivos no se guarda nada")
    void conMasDe3MiniObjetivosNoSeGuardaNada() {
        cuandoSeGuarda(false, accion(), accion(), accion(), accion());

        entoncesSeRechazaPorLaCantidadDeMinisSinGuardarNada();
    }

    @Test
    @DisplayName("Con una lista nula de mini-objetivos no se guarda nada")
    void conUnaListaNulaDeMiniObjetivosNoSeGuardaNada() {
        cuandoSeGuardaConListaNula();

        entoncesSeRechazaPorLaCantidadDeMinisSinGuardarNada();
    }

    @Test
    @DisplayName("Un mini-objetivo medido que ya no está en el catálogo se rechaza y no se guarda nada")
    void unMiniObjetivoMedidoQueYaNoEstaEnElCatalogoSeRechazaYNoSeGuardaNada() {
        cuandoSeGuarda(false, medido(TipoMiniObjetivo.RECORTAR_GASTO, null, "Luz"));

        entoncesSeRechazaElMiniQueYaNoEstaDisponible();
    }

    @Test
    @DisplayName("Dos mini-objetivos medidos que miden lo mismo se rechazan")
    void dosMiniObjetivosMedidosQueMidenLoMismoSeRechazan() {
        cuandoSeGuarda(false,
                medido(TipoMiniObjetivo.SUBIR_PRECIO, PRODUCTO_ID, PRODUCTO),
                medido(TipoMiniObjetivo.PONER_A_LA_VISTA, PRODUCTO_ID, PRODUCTO));

        entoncesSeRechazaPorqueDosMinisMidenLoMismo();
    }

    @Test
    @DisplayName("Una acción con importes, o sin texto, se rechaza")
    void unaAccionConImportesOSinTextoSeRechaza() {
        cuandoSeGuarda(false, accionConImporte());

        entoncesSeRechazaLaAccionInvalida();
    }

    @Test
    @DisplayName("Un usuario sin negocio responde como inexistente")
    void unUsuarioSinNegocioRespondeComoInexistente() {
        dadoUnUsuarioSinNegocio();

        cuandoSeGuarda(false, accion());

        entoncesRespondeComoInexistenteSinGuardarNada();
    }

    @Test
    @DisplayName("Una acción elegida antes no impide guardar después un mini-objetivo medido")
    void unaAccionElegidaAntesNoImpideGuardarDespuesUnMiniObjetivoMedido() {
        cuandoSeGuarda(false, accion(), medido(TipoMiniObjetivo.VENTAS_SEMANALES, null, null));

        entoncesSeGuardaUnObjetivoHibridoConDosMinis();
    }

    @Test
    @DisplayName("Un objetivo armado con Sami gasta una unidad de la cuota: se valida antes de guardar y se anota después")
    void unObjetivoArmadoConSamiGastaUnaUnidadDeLaCuotaSeValidaAntesDeGuardarYSe() {
        cuandoSeGuarda(false, accion());

        entoncesLaCuotaSeValidaAntesDeGuardarYSeAnotaDespues();
    }

    @Test
    @DisplayName("Sin cuota del mes se rechaza con el límite del plan")
    void sinCuotaDelMesSeRechazaConElLimiteDelPlan() {
        dadoLaCuotaDelMesAgotada();

        cuandoSeGuarda(false, accion());

        entoncesSeRechazaPorElLimiteDelPlan();
    }

    @Test
    @DisplayName("Sin cuota del mes no guarda el objetivo ni ningún mini-objetivo ni gasta nada")
    void sinCuotaDelMesNoGuardaElObjetivoNiNingunMiniObjetivoNiGastaNada() {
        dadoLaCuotaDelMesAgotada();

        cuandoSeGuarda(false, accion());

        entoncesNoSeGuardaNadaNiSeGastaCuota();
    }

    @Test
    @DisplayName("El seguimiento manual no gasta ni consulta la cuota")
    void elSeguimientoManualNoGastaNiConsultaLaCuota() {
        cuandoSeGuarda(true);

        entoncesNoSeGastaNiSeConsultaLaCuota();
    }

    // --- arrange ---
    private void dadoUnNegocioQueGuardaSusObjetivosYMinis() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(objetivoRepository.save(any(Objetivo.class))).thenAnswer(call -> {
            Objetivo objetivo = call.getArgument(0);
            objetivo.setId(OBJETIVO_ID);
            return objetivo;
        });
        when(miniObjetivoRepository.save(any(MiniObjetivo.class))).thenAnswer(call -> call.getArgument(0));
        when(propuestaMiniObjetivosService.candidatos(NEGOCIO_ID, HOY))
                .thenReturn(new CandidatosDelNegocio(new BigDecimal("180000"), catalogo()));
    }

    private void dadoUnUsuarioSinNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID))
                .thenThrow(new ResourceNotFoundException("Negocio no encontrado"));
    }

    private void dadoLaCuotaDelMesAgotada() {
        doThrow(new RestriccionPlanException("LIMITE_OBJETIVOS_IA", "Ya armaste los 5 objetivos"))
                .when(controlPlanService).validarObjetivoIa(NEGOCIO_ID, AHORA);
    }

    // --- act ---
    private void cuandoSeGuarda(boolean seguimientoManual, MiniElegido... minis) {
        error = catchThrowable(() -> objetivoGuardadoItem = guardarObjetivoConSami.execute(
                new GuardarObjetivoConSamiRequest(
                        USUARIO_ID, "  " + OBJETIVO + " ", seguimientoManual, List.of(minis))));
    }

    private void cuandoSeGuardaConListaNula() {
        error = catchThrowable(() -> objetivoGuardadoItem = guardarObjetivoConSami.execute(
                new GuardarObjetivoConSamiRequest(USUARIO_ID, OBJETIVO, false, null)));
    }

    // --- assert ---
    private void entoncesSeGuardaUnObjetivoHibridoSinMetaArmadoConSami() {
        Objetivo guardado = objetivoGuardado();
        assertThat(guardado.getTipo()).isEqualTo(TipoObjetivo.HIBRIDO);
        assertThat(guardado.getMeta()).isNull();
        assertThat(guardado.isArmadoConSami()).isTrue();
        assertThat(guardado.getTexto()).isEqualTo(OBJETIVO);
        assertThat(guardado.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardado.getCreadoEn()).isEqualTo(AHORA);
    }

    private void entoncesSeDevuelveElObjetivoHibridoGuardado() {
        assertThat(objetivoGuardadoItem.id()).isEqualTo(OBJETIVO_ID);
        assertThat(objetivoGuardadoItem.tipo()).isEqualTo(TipoObjetivo.HIBRIDO);
        assertThat(objetivoGuardadoItem.meta()).isNull();
        assertThat(objetivoGuardadoItem.armadoConSami()).isTrue();
    }

    private void entoncesLosMinisEntranEnOrdenDespuesDelObjetivo() {
        ArgumentCaptor<MiniObjetivo> captor = ArgumentCaptor.forClass(MiniObjetivo.class);
        InOrder orden = inOrder(objetivoRepository, miniObjetivoRepository);
        orden.verify(objetivoRepository).save(any(Objetivo.class));
        orden.verify(miniObjetivoRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(MiniObjetivo::getPosicion).containsExactly(1, 2);
        assertThat(captor.getAllValues()).extracting(MiniObjetivo::getModo)
                .containsExactly(ModoMiniObjetivo.MEDIDO, ModoMiniObjetivo.MANUAL);
        assertThat(captor.getAllValues()).allSatisfy(mini -> {
            assertThat(mini.getObjetivoId()).isEqualTo(OBJETIVO_ID);
            assertThat(mini.getSemanaInicio()).isEqualTo(SEMANA);
            assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
        });
    }

    private void entoncesLosNumerosDelMiniSalenDelCatalogo() {
        MiniObjetivo mini = unicoMiniGuardado();
        assertThat(mini.getMeta()).isEqualTo(catalogo().getFirst().meta());
        assertThat(mini.getValorInicial()).isEqualTo(catalogo().getFirst().valorInicial());
        assertThat(mini.getAporteEstimado()).isEqualTo(catalogo().getFirst().aporteEstimado());
        assertThat(mini.getTitulo()).isEqualTo(catalogo().getFirst().titulo());
        assertThat(mini.isTextoConIa()).isFalse();
    }

    private void entoncesElObjetivoEs(TipoObjetivo tipo) {
        assertThat(objetivoGuardado().getTipo()).isEqualTo(tipo);
    }

    private void entoncesElObjetivoEsSinDatosArmadoConSami() {
        assertThat(objetivoGuardado().getTipo()).isEqualTo(TipoObjetivo.SIN_DATOS);
        assertThat(objetivoGuardado().isArmadoConSami()).isTrue();
    }

    private void entoncesNoSeConsultaElCatalogoYElTextoEsDeLaIa() {
        verifyNoInteractions(propuestaMiniObjetivosService);
        assertThat(unicoMiniGuardado().isTextoConIa()).isTrue();
    }

    private void entoncesElObjetivoEsSinDatosYNoArmadoConSami() {
        assertThat(objetivoGuardado().getTipo()).isEqualTo(TipoObjetivo.SIN_DATOS);
        assertThat(objetivoGuardado().isArmadoConSami()).isFalse();
        assertThat(objetivoGuardadoItem.armadoConSami()).isFalse();
    }

    private void entoncesSeGuardaSoloElManualGenerico() {
        MiniObjetivo mini = unicoMiniGuardado();
        assertThat(mini.esManual()).isTrue();
        assertThat(mini.getTitulo()).isEqualTo("Anotá cómo te fue con tu objetivo");
        assertThat(mini.getJustificacion()).contains(OBJETIVO);
        assertThat(mini.isTextoConIa()).isFalse();
        verifyNoInteractions(propuestaMiniObjetivosService);
    }

    private void entoncesSeRechazaPorLaCantidadDeMinisSinGuardarNada() {
        assertThat(error)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Elegí entre 1 y 3 mini-objetivos para guardar");
        entoncesNoSeGuardaNadaDelObjetivo();
    }

    private void entoncesSeRechazaElMiniQueYaNoEstaDisponible() {
        assertThat(error)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Ese mini-objetivo ya no está disponible: pedile otra propuesta a Sami");
        verify(objetivoRepository, never()).save(any());
    }

    private void entoncesSeRechazaPorqueDosMinisMidenLoMismo() {
        assertThat(error)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Dos mini-objetivos no pueden medir lo mismo");
        verify(objetivoRepository, never()).save(any());
    }

    private void entoncesSeRechazaLaAccionInvalida() {
        assertThat(error)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageStartingWith("El texto de la acción no es válido");
        verify(objetivoRepository, never()).save(any());
    }

    private void entoncesRespondeComoInexistenteSinGuardarNada() {
        assertThat(error).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(objetivoRepository, miniObjetivoRepository);
    }

    private void entoncesSeGuardaUnObjetivoHibridoConDosMinis() {
        assertThat(objetivoGuardado().getTipo()).isEqualTo(TipoObjetivo.HIBRIDO);
        verify(miniObjetivoRepository, times(2)).save(any(MiniObjetivo.class));
    }

    private void entoncesLaCuotaSeValidaAntesDeGuardarYSeAnotaDespues() {
        InOrder orden = inOrder(controlPlanService, objetivoRepository, usoPlanRepository);
        orden.verify(controlPlanService).validarObjetivoIa(NEGOCIO_ID, AHORA);
        orden.verify(objetivoRepository).save(any(Objetivo.class));
        orden.verify(usoPlanRepository).registrarObjetivoIa(NEGOCIO_ID, OBJETIVO_ID, AHORA);
    }

    private void entoncesSeRechazaPorElLimiteDelPlan() {
        assertThat(error)
                .isInstanceOf(RestriccionPlanException.class)
                .hasMessageContaining("Ya armaste los 5 objetivos");
    }

    private void entoncesNoSeGuardaNadaNiSeGastaCuota() {
        entoncesNoSeGuardaNadaDelObjetivo();
        verifyNoInteractions(usoPlanRepository);
    }

    private void entoncesNoSeGastaNiSeConsultaLaCuota() {
        verifyNoInteractions(controlPlanService, usoPlanRepository);
    }

    // --- helpers ---
    private void entoncesNoSeGuardaNadaDelObjetivo() {
        verify(objetivoRepository, never()).save(any());
        verify(miniObjetivoRepository, never()).save(any());
    }

    private Objetivo objetivoGuardado() {
        ArgumentCaptor<Objetivo> captor = ArgumentCaptor.forClass(Objetivo.class);
        verify(objetivoRepository).save(captor.capture());
        return captor.getValue();
    }

    private MiniObjetivo unicoMiniGuardado() {
        ArgumentCaptor<MiniObjetivo> captor = ArgumentCaptor.forClass(MiniObjetivo.class);
        verify(miniObjetivoRepository).save(captor.capture());
        return captor.getValue();
    }

    private static MiniElegido medido(TipoMiniObjetivo tipo, RegistroId producto, String referencia) {
        return new MiniElegido(ModoMiniObjetivo.MEDIDO, tipo, producto, referencia, null, null);
    }

    private static MiniElegido accion() {
        return new MiniElegido(ModoMiniObjetivo.MANUAL, null, null, null, TITULO_ACCION, JUSTIFICACION_ACCION);
    }

    private static MiniElegido accionConImporte() {
        return new MiniElegido(ModoMiniObjetivo.MANUAL, null, null, null, "Cobrá $5.000 más", JUSTIFICACION_ACCION);
    }
}
