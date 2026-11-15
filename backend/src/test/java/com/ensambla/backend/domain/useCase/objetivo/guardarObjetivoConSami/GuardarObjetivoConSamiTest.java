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
        givenUnNegocioQueGuardaSusObjetivosYMinis();
    }

    @Test
    @DisplayName("Guarda un objetivo híbrido sin meta, armado con Sami, con el texto recortado")
    void executeShouldSaveAHybridObjetivo() {
        whenSeGuarda(false, medido(TipoMiniObjetivo.VENTAS_SEMANALES, null, null), accion());

        thenSeGuardaUnObjetivoHibridoSinMetaArmadoConSami();
    }

    @Test
    @DisplayName("Devuelve el objetivo guardado")
    void executeShouldReturnTheSavedObjetivo() {
        whenSeGuarda(false, medido(TipoMiniObjetivo.VENTAS_SEMANALES, null, null), accion());

        thenSeDevuelveElObjetivoHibridoGuardado();
    }

    @Test
    @DisplayName("Los mini-objetivos entran en posiciones 1, 2… en la semana de hoy, después del objetivo")
    void executeShouldSaveTheMinisInOrderAfterTheObjetivo() {
        whenSeGuarda(false, medido(TipoMiniObjetivo.VENTAS_SEMANALES, null, null), accion());

        thenLosMinisEntranEnOrdenDespuesDelObjetivo();
    }

    @Test
    @DisplayName("Los números de un mini-objetivo medido los pone el servidor con el catálogo de hoy")
    void executeShouldTakeTheNumbersOfAMeasuredMiniFromTheCatalog() {
        whenSeGuarda(false, medido(TipoMiniObjetivo.SUBIR_PRECIO, PRODUCTO_ID, PRODUCTO));

        thenLosNumerosDelMiniSalenDelCatalogo();
    }

    @Test
    @DisplayName("Con sólo mini-objetivos medidos el objetivo es con datos")
    void executeShouldSaveAnObjetivoWithData() {
        whenSeGuarda(false, medido(TipoMiniObjetivo.VENTAS_SEMANALES, null, null),
                medido(TipoMiniObjetivo.MEJORAR_MARGEN, null, null));

        thenElObjetivoEs(TipoObjetivo.CON_DATOS);
    }

    @Test
    @DisplayName("Con sólo acciones el objetivo es sin datos y está armado con Sami")
    void executeShouldSaveAnObjetivoWithoutData() {
        whenSeGuarda(false, accion());

        thenElObjetivoEsSinDatosArmadoConSami();
    }

    @Test
    @DisplayName("Con sólo acciones no consulta el catálogo y el texto de la acción es de la IA")
    void executeShouldNotLoadTheCatalogWithOnlyActions() {
        whenSeGuarda(false, accion());

        thenNoSeConsultaElCatalogoYElTextoEsDeLaIa();
    }

    @Test
    @DisplayName("El seguimiento manual no cuenta como armado con Sami")
    void executeShouldNotCountTheManualFollowUpAsArmadoConSami() {
        whenSeGuarda(true, medido(TipoMiniObjetivo.VENTAS_SEMANALES, null, null));

        thenElObjetivoEsSinDatosYNoArmadoConSami();
    }

    @Test
    @DisplayName("El seguimiento manual ignora los mini-objetivos recibidos y guarda un manual genérico")
    void executeShouldIgnoreTheReceivedMinisOnTheManualFollowUp() {
        whenSeGuarda(true, medido(TipoMiniObjetivo.VENTAS_SEMANALES, null, null));

        thenSeGuardaSoloElManualGenerico();
    }

    @Test
    @DisplayName("Sin mini-objetivos no se guarda nada")
    void executeShouldRejectWithoutMinis() {
        whenSeGuarda(false);

        thenSeRechazaPorLaCantidadDeMinisSinGuardarNada();
    }

    @Test
    @DisplayName("Con más de 3 mini-objetivos no se guarda nada")
    void executeShouldRejectMoreThanThreeMinis() {
        whenSeGuarda(false, accion(), accion(), accion(), accion());

        thenSeRechazaPorLaCantidadDeMinisSinGuardarNada();
    }

    @Test
    @DisplayName("Con una lista nula de mini-objetivos no se guarda nada")
    void executeShouldRejectANullListOfMinis() {
        whenSeGuardaConListaNula();

        thenSeRechazaPorLaCantidadDeMinisSinGuardarNada();
    }

    @Test
    @DisplayName("Un mini-objetivo medido que ya no está en el catálogo se rechaza y no se guarda nada")
    void executeShouldRejectAMeasuredMiniThatIsNoLongerAvailable() {
        whenSeGuarda(false, medido(TipoMiniObjetivo.RECORTAR_GASTO, null, "Luz"));

        thenSeRechazaElMiniQueYaNoEstaDisponible();
    }

    @Test
    @DisplayName("Dos mini-objetivos medidos que miden lo mismo se rechazan")
    void executeShouldRejectTwoMeasuredMinisThatClash() {
        whenSeGuarda(false,
                medido(TipoMiniObjetivo.SUBIR_PRECIO, PRODUCTO_ID, PRODUCTO),
                medido(TipoMiniObjetivo.PONER_A_LA_VISTA, PRODUCTO_ID, PRODUCTO));

        thenSeRechazaPorqueDosMinisMidenLoMismo();
    }

    @Test
    @DisplayName("Una acción con importes, o sin texto, se rechaza")
    void executeShouldRejectAnInvalidAction() {
        whenSeGuarda(false, accionConImporte());

        thenSeRechazaLaAccionInvalida();
    }

    @Test
    @DisplayName("Un usuario sin negocio responde como inexistente")
    void executeShouldFailWithoutBusiness() {
        givenUnUsuarioSinNegocio();

        whenSeGuarda(false, accion());

        thenRespondeComoInexistenteSinGuardarNada();
    }

    @Test
    @DisplayName("Una acción elegida antes no impide guardar después un mini-objetivo medido")
    void executeShouldSaveAMeasuredMiniAfterAManualAction() {
        whenSeGuarda(false, accion(), medido(TipoMiniObjetivo.VENTAS_SEMANALES, null, null));

        thenSeGuardaUnObjetivoHibridoConDosMinis();
    }

    @Test
    @DisplayName("Un objetivo armado con Sami gasta una unidad de la cuota: se valida antes de guardar y se anota después")
    void executeShouldSpendOneUnitOfTheMonthlyAllowance() {
        whenSeGuarda(false, accion());

        thenLaCuotaSeValidaAntesDeGuardarYSeAnotaDespues();
    }

    @Test
    @DisplayName("Sin cuota del mes se rechaza con el límite del plan")
    void executeShouldRejectWhenTheAllowanceIsUsed() {
        givenLaCuotaDelMesAgotada();

        whenSeGuarda(false, accion());

        thenSeRechazaPorElLimiteDelPlan();
    }

    @Test
    @DisplayName("Sin cuota del mes no guarda el objetivo ni ningún mini-objetivo ni gasta nada")
    void executeShouldSaveNothingWhenTheAllowanceIsUsed() {
        givenLaCuotaDelMesAgotada();

        whenSeGuarda(false, accion());

        thenNoSeGuardaNadaNiSeGastaCuota();
    }

    @Test
    @DisplayName("El seguimiento manual no gasta ni consulta la cuota")
    void executeShouldNotSpendTheAllowanceOnTheManualFollowUp() {
        whenSeGuarda(true);

        thenNoSeGastaNiSeConsultaLaCuota();
    }

    // --- arrange ---
    private void givenUnNegocioQueGuardaSusObjetivosYMinis() {
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

    private void givenUnUsuarioSinNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID))
                .thenThrow(new ResourceNotFoundException("Negocio no encontrado"));
    }

    private void givenLaCuotaDelMesAgotada() {
        doThrow(new RestriccionPlanException("LIMITE_OBJETIVOS_IA", "Ya armaste los 5 objetivos"))
                .when(controlPlanService).validarObjetivoIa(NEGOCIO_ID, AHORA);
    }

    // --- act ---
    private void whenSeGuarda(boolean seguimientoManual, MiniElegido... minis) {
        error = catchThrowable(() -> objetivoGuardadoItem = guardarObjetivoConSami.execute(
                new GuardarObjetivoConSamiRequest(
                        USUARIO_ID, "  " + OBJETIVO + " ", seguimientoManual, List.of(minis))));
    }

    private void whenSeGuardaConListaNula() {
        error = catchThrowable(() -> objetivoGuardadoItem = guardarObjetivoConSami.execute(
                new GuardarObjetivoConSamiRequest(USUARIO_ID, OBJETIVO, false, null)));
    }

    // --- assert ---
    private void thenSeGuardaUnObjetivoHibridoSinMetaArmadoConSami() {
        Objetivo guardado = objetivoGuardado();
        assertThat(guardado.getTipo()).isEqualTo(TipoObjetivo.HIBRIDO);
        assertThat(guardado.getMeta()).isNull();
        assertThat(guardado.isArmadoConSami()).isTrue();
        assertThat(guardado.getTexto()).isEqualTo(OBJETIVO);
        assertThat(guardado.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardado.getCreadoEn()).isEqualTo(AHORA);
    }

    private void thenSeDevuelveElObjetivoHibridoGuardado() {
        assertThat(objetivoGuardadoItem.id()).isEqualTo(OBJETIVO_ID);
        assertThat(objetivoGuardadoItem.tipo()).isEqualTo(TipoObjetivo.HIBRIDO);
        assertThat(objetivoGuardadoItem.meta()).isNull();
        assertThat(objetivoGuardadoItem.armadoConSami()).isTrue();
    }

    private void thenLosMinisEntranEnOrdenDespuesDelObjetivo() {
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

    private void thenLosNumerosDelMiniSalenDelCatalogo() {
        MiniObjetivo mini = unicoMiniGuardado();
        assertThat(mini.getMeta()).isEqualTo(catalogo().getFirst().meta());
        assertThat(mini.getValorInicial()).isEqualTo(catalogo().getFirst().valorInicial());
        assertThat(mini.getAporteEstimado()).isEqualTo(catalogo().getFirst().aporteEstimado());
        assertThat(mini.getTitulo()).isEqualTo(catalogo().getFirst().titulo());
        assertThat(mini.isTextoConIa()).isFalse();
    }

    private void thenElObjetivoEs(TipoObjetivo tipo) {
        assertThat(objetivoGuardado().getTipo()).isEqualTo(tipo);
    }

    private void thenElObjetivoEsSinDatosArmadoConSami() {
        assertThat(objetivoGuardado().getTipo()).isEqualTo(TipoObjetivo.SIN_DATOS);
        assertThat(objetivoGuardado().isArmadoConSami()).isTrue();
    }

    private void thenNoSeConsultaElCatalogoYElTextoEsDeLaIa() {
        verifyNoInteractions(propuestaMiniObjetivosService);
        assertThat(unicoMiniGuardado().isTextoConIa()).isTrue();
    }

    private void thenElObjetivoEsSinDatosYNoArmadoConSami() {
        assertThat(objetivoGuardado().getTipo()).isEqualTo(TipoObjetivo.SIN_DATOS);
        assertThat(objetivoGuardado().isArmadoConSami()).isFalse();
        assertThat(objetivoGuardadoItem.armadoConSami()).isFalse();
    }

    private void thenSeGuardaSoloElManualGenerico() {
        MiniObjetivo mini = unicoMiniGuardado();
        assertThat(mini.esManual()).isTrue();
        assertThat(mini.getTitulo()).isEqualTo("Anotá cómo te fue con tu objetivo");
        assertThat(mini.getJustificacion()).contains(OBJETIVO);
        assertThat(mini.isTextoConIa()).isFalse();
        verifyNoInteractions(propuestaMiniObjetivosService);
    }

    private void thenSeRechazaPorLaCantidadDeMinisSinGuardarNada() {
        assertThat(error)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Elegí entre 1 y 3 mini-objetivos para guardar");
        thenNoSeGuardaNadaDelObjetivo();
    }

    private void thenSeRechazaElMiniQueYaNoEstaDisponible() {
        assertThat(error)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Ese mini-objetivo ya no está disponible: pedile otra propuesta a Sami");
        verify(objetivoRepository, never()).save(any());
    }

    private void thenSeRechazaPorqueDosMinisMidenLoMismo() {
        assertThat(error)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Dos mini-objetivos no pueden medir lo mismo");
        verify(objetivoRepository, never()).save(any());
    }

    private void thenSeRechazaLaAccionInvalida() {
        assertThat(error)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageStartingWith("El texto de la acción no es válido");
        verify(objetivoRepository, never()).save(any());
    }

    private void thenRespondeComoInexistenteSinGuardarNada() {
        assertThat(error).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(objetivoRepository, miniObjetivoRepository);
    }

    private void thenSeGuardaUnObjetivoHibridoConDosMinis() {
        assertThat(objetivoGuardado().getTipo()).isEqualTo(TipoObjetivo.HIBRIDO);
        verify(miniObjetivoRepository, times(2)).save(any(MiniObjetivo.class));
    }

    private void thenLaCuotaSeValidaAntesDeGuardarYSeAnotaDespues() {
        InOrder orden = inOrder(controlPlanService, objetivoRepository, usoPlanRepository);
        orden.verify(controlPlanService).validarObjetivoIa(NEGOCIO_ID, AHORA);
        orden.verify(objetivoRepository).save(any(Objetivo.class));
        orden.verify(usoPlanRepository).registrarObjetivoIa(NEGOCIO_ID, OBJETIVO_ID, AHORA);
    }

    private void thenSeRechazaPorElLimiteDelPlan() {
        assertThat(error)
                .isInstanceOf(RestriccionPlanException.class)
                .hasMessageContaining("Ya armaste los 5 objetivos");
    }

    private void thenNoSeGuardaNadaNiSeGastaCuota() {
        thenNoSeGuardaNadaDelObjetivo();
        verifyNoInteractions(usoPlanRepository);
    }

    private void thenNoSeGastaNiSeConsultaLaCuota() {
        verifyNoInteractions(controlPlanService, usoPlanRepository);
    }

    // --- helpers ---
    private void thenNoSeGuardaNadaDelObjetivo() {
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
