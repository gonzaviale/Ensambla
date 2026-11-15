package com.ensambla.backend.domain.useCase.objetivo.guardarObjetivoConSami;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.objetivo.CandidatosDelNegocio;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @BeforeEach
    void setUp() {
        guardarObjetivoConSami = new GuardarObjetivoConSami(
                negocioRepository, objetivoRepository, miniObjetivoRepository, propuestaMiniObjetivosService,
                controlPlanService, usoPlanRepository, new TransaccionDirecta(), CLOCK);
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

    @Test
    @DisplayName("Guarda un objetivo híbrido sin meta con un mini-objetivo medido y uno manual")
    void executeShouldSaveAHybridObjetivo() {
        ObjetivoItem result = whenGuardar(false, medido(TipoMiniObjetivo.VENTAS_SEMANALES, null, null), accion());

        Objetivo guardado = objetivoGuardado();
        assertThat(guardado.getTipo()).isEqualTo(TipoObjetivo.HIBRIDO);
        assertThat(guardado.getMeta()).isNull();
        assertThat(guardado.isArmadoConSami()).isTrue();
        assertThat(guardado.getTexto()).isEqualTo(OBJETIVO);
        assertThat(guardado.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardado.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.id()).isEqualTo(OBJETIVO_ID);
        assertThat(result.tipo()).isEqualTo(TipoObjetivo.HIBRIDO);
        assertThat(result.meta()).isNull();
        assertThat(result.armadoConSami()).isTrue();
    }

    @Test
    @DisplayName("Los mini-objetivos entran en posiciones 1, 2… en la semana de hoy, después del objetivo")
    void executeShouldSaveTheMinisInOrderAfterTheObjetivo() {
        whenGuardar(false, medido(TipoMiniObjetivo.VENTAS_SEMANALES, null, null), accion());

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
            assertThat(mini.getEstado().name()).isEqualTo("EN_CURSO");
        });
    }

    @Test
    @DisplayName("Los números de un mini-objetivo medido los pone el servidor con el catálogo de hoy")
    void executeShouldTakeTheNumbersOfAMeasuredMiniFromTheCatalog() {
        whenGuardar(false, medido(TipoMiniObjetivo.SUBIR_PRECIO, PRODUCTO_ID, PRODUCTO));

        ArgumentCaptor<MiniObjetivo> captor = ArgumentCaptor.forClass(MiniObjetivo.class);
        verify(miniObjetivoRepository).save(captor.capture());
        assertThat(captor.getValue().getMeta()).isEqualTo(catalogo().getFirst().meta());
        assertThat(captor.getValue().getValorInicial()).isEqualTo(catalogo().getFirst().valorInicial());
        assertThat(captor.getValue().getAporteEstimado()).isEqualTo(catalogo().getFirst().aporteEstimado());
        assertThat(captor.getValue().getTitulo()).isEqualTo(catalogo().getFirst().titulo());
        assertThat(captor.getValue().isTextoConIa()).isFalse();
    }

    @Test
    @DisplayName("Con sólo mini-objetivos medidos el objetivo es con datos")
    void executeShouldSaveAnObjetivoWithData() {
        whenGuardar(false, medido(TipoMiniObjetivo.VENTAS_SEMANALES, null, null),
                medido(TipoMiniObjetivo.MEJORAR_MARGEN, null, null));

        assertThat(objetivoGuardado().getTipo()).isEqualTo(TipoObjetivo.CON_DATOS);
    }

    @Test
    @DisplayName("Con sólo acciones el objetivo es sin datos y no consulta el catálogo")
    void executeShouldSaveAnObjetivoWithoutDataAndNotLoadTheCatalog() {
        whenGuardar(false, accion());

        assertThat(objetivoGuardado().getTipo()).isEqualTo(TipoObjetivo.SIN_DATOS);
        assertThat(objetivoGuardado().isArmadoConSami()).isTrue();
        verifyNoInteractions(propuestaMiniObjetivosService);
        ArgumentCaptor<MiniObjetivo> captor = ArgumentCaptor.forClass(MiniObjetivo.class);
        verify(miniObjetivoRepository).save(captor.capture());
        assertThat(captor.getValue().isTextoConIa()).isTrue();
    }

    @Test
    @DisplayName("El seguimiento manual ignora los mini-objetivos recibidos y no cuenta como armado con Sami")
    void executeShouldSaveTheManualFollowUp() {
        ObjetivoItem result = whenGuardar(true, medido(TipoMiniObjetivo.VENTAS_SEMANALES, null, null));

        assertThat(objetivoGuardado().getTipo()).isEqualTo(TipoObjetivo.SIN_DATOS);
        assertThat(objetivoGuardado().isArmadoConSami()).isFalse();
        assertThat(result.armadoConSami()).isFalse();
        ArgumentCaptor<MiniObjetivo> captor = ArgumentCaptor.forClass(MiniObjetivo.class);
        verify(miniObjetivoRepository).save(captor.capture());
        assertThat(captor.getValue().esManual()).isTrue();
        assertThat(captor.getValue().getTitulo()).isEqualTo("Anotá cómo te fue con tu objetivo");
        assertThat(captor.getValue().getJustificacion()).contains(OBJETIVO);
        assertThat(captor.getValue().isTextoConIa()).isFalse();
        verifyNoInteractions(propuestaMiniObjetivosService);
    }

    @Test
    @DisplayName("Sin mini-objetivos, con una lista nula o con más de 3 no se guarda nada")
    void executeShouldRejectAnInvalidNumberOfMinis() {
        assertThatThrownBy(() -> whenGuardar(false))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Elegí entre 1 y 3 mini-objetivos para guardar");
        assertThatThrownBy(() -> whenGuardar(false, accion(), accion(), accion(), accion()))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> guardarObjetivoConSami.execute(
                new GuardarObjetivoConSamiRequest(USUARIO_ID, OBJETIVO, false, null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(objetivoRepository, never()).save(any());
        verify(miniObjetivoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un mini-objetivo medido que ya no está en el catálogo se rechaza y no se guarda nada")
    void executeShouldRejectAMeasuredMiniThatIsNoLongerAvailable() {
        assertThatThrownBy(() -> whenGuardar(false, medido(TipoMiniObjetivo.RECORTAR_GASTO, null, "Luz")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Ese mini-objetivo ya no está disponible: pedile otra propuesta a Sami");
        verify(objetivoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Dos mini-objetivos medidos que miden lo mismo se rechazan")
    void executeShouldRejectTwoMeasuredMinisThatClash() {
        assertThatThrownBy(() -> whenGuardar(false,
                medido(TipoMiniObjetivo.SUBIR_PRECIO, PRODUCTO_ID, PRODUCTO),
                medido(TipoMiniObjetivo.PONER_A_LA_VISTA, PRODUCTO_ID, PRODUCTO)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Dos mini-objetivos no pueden medir lo mismo");
        verify(objetivoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una acción con importes, o sin texto, se rechaza")
    void executeShouldRejectAnInvalidAction() {
        MiniElegido conImporte = new MiniElegido(ModoMiniObjetivo.MANUAL, null, null, null,
                "Cobrá $5.000 más", JUSTIFICACION_ACCION);

        assertThatThrownBy(() -> whenGuardar(false, conImporte))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageStartingWith("El texto de la acción no es válido");
        verify(objetivoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un usuario sin negocio responde como inexistente")
    void executeShouldFailWithoutBusiness() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID))
                .thenThrow(new ResourceNotFoundException("Negocio no encontrado"));

        assertThatThrownBy(() -> whenGuardar(false, accion()))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(objetivoRepository, miniObjetivoRepository);
    }

    // --- helpers ---
    private static MiniElegido medido(TipoMiniObjetivo tipo, RegistroId producto, String referencia) {
        return new MiniElegido(ModoMiniObjetivo.MEDIDO, tipo, producto, referencia, null, null);
    }

    private static MiniElegido accion() {
        return new MiniElegido(ModoMiniObjetivo.MANUAL, null, null, null, TITULO_ACCION, JUSTIFICACION_ACCION);
    }

    // --- act ---
    private ObjetivoItem whenGuardar(boolean seguimientoManual, MiniElegido... minis) {
        return guardarObjetivoConSami.execute(new GuardarObjetivoConSamiRequest(
                USUARIO_ID, "  " + OBJETIVO + " ", seguimientoManual, List.of(minis)));
    }

    // --- assert ---
    private Objetivo objetivoGuardado() {
        ArgumentCaptor<Objetivo> captor = ArgumentCaptor.forClass(Objetivo.class);
        verify(objetivoRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Una acción elegida antes no impide guardar después un mini-objetivo medido")
    void executeShouldSaveAMeasuredMiniAfterAManualAction() {
        whenGuardar(false, accion(), medido(TipoMiniObjetivo.VENTAS_SEMANALES, null, null));

        assertThat(objetivoGuardado().getTipo()).isEqualTo(TipoObjetivo.HIBRIDO);
        verify(miniObjetivoRepository, times(2)).save(any(MiniObjetivo.class));
    }

    @Test
    @DisplayName("Un objetivo armado con Sami gasta una unidad de la cuota: se valida antes de guardar y se anota después")
    void executeShouldSpendOneUnitOfTheMonthlyAllowance() {
        whenGuardar(false, accion());

        InOrder orden = inOrder(controlPlanService, objetivoRepository, usoPlanRepository);
        orden.verify(controlPlanService).validarObjetivoIa(NEGOCIO_ID, AHORA);
        orden.verify(objetivoRepository).save(any(Objetivo.class));
        orden.verify(usoPlanRepository).registrarObjetivoIa(NEGOCIO_ID, OBJETIVO_ID, AHORA);
    }

    @Test
    @DisplayName("Sin cuota del mes no guarda el objetivo ni ningún mini-objetivo ni gasta nada")
    void executeShouldSaveNothingWhenTheAllowanceIsUsed() {
        doThrow(new RestriccionPlanException("LIMITE_OBJETIVOS_IA", "Ya armaste los 5 objetivos"))
                .when(controlPlanService).validarObjetivoIa(NEGOCIO_ID, AHORA);

        assertThatThrownBy(() -> whenGuardar(false, accion()))
                .isInstanceOf(RestriccionPlanException.class)
                .hasMessageContaining("Ya armaste los 5 objetivos");
        verify(objetivoRepository, never()).save(any());
        verify(miniObjetivoRepository, never()).save(any());
        verifyNoInteractions(usoPlanRepository);
    }

    @Test
    @DisplayName("El seguimiento manual no gasta ni consulta la cuota")
    void executeShouldNotSpendTheAllowanceOnTheManualFollowUp() {
        whenGuardar(true);

        verifyNoInteractions(controlPlanService, usoPlanRepository);
    }
}
