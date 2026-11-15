package com.ensambla.backend.domain.useCase.objetivo.proponerObjetivoConSami;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.objetivo.CandidatoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.ConsultaObjetivoIa;
import com.ensambla.backend.domain.model.objetivo.FuenteDato;
import com.ensambla.backend.domain.model.objetivo.MedidorMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MensajeObjetivo;
import com.ensambla.backend.domain.model.objetivo.ModoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.OrigenPropuesta;
import com.ensambla.backend.domain.model.objetivo.PropuestaObjetivoIa;
import com.ensambla.backend.domain.model.objetivo.RolMensaje;
import com.ensambla.backend.domain.model.objetivo.TipoObjetivo;
import com.ensambla.backend.domain.model.plan.CuotaObjetivosIa;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.port.ConversacionObjetivosIaPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.objetivo.proponerObjetivoConSami.ProponerObjetivoConSamiResponse.CuotaItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import static com.ensambla.backend.support.ConversacionObjetivoTestData.CONSUMO;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.ENTENDIDO;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.JUSTIFICACION_ACCION;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.OBJETIVO;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.RESPUESTA_IA;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.TITULO_ACCION;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.catalogo;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.manual;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.medido;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.mensajes;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.respuestaIa;
import static com.ensambla.backend.support.NegocioDePrueba.almacen;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProponerObjetivoConSamiTest {

    private static final Clock CLOCK = Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC);
    private static final LocalDate HOY = LocalDate.of(2026, 11, 10);
    private static final int LIMITE_DIARIO = 10;
    private static final int MAXIMO_MENSAJES = 6;
    private static final int MAXIMO_DE_METRICAS_PARA_LA_IA = 12;
    private static final int METRICAS_DE_SOBRA = 15;

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final PropuestaMiniObjetivosService propuestaMiniObjetivosService =
            mock(PropuestaMiniObjetivosService.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final ConsumoSamiRepository consumoSamiRepository = mock(ConsumoSamiRepository.class);
    private final ConversacionObjetivosIaPort conversacionObjetivosIaPort = mock(ConversacionObjetivosIaPort.class);
    private final MedidorMiniObjetivo medidor =
            MedidorMiniObjetivo.de(List.of(almacen()), new MetricasVentas(30, new BigDecimal("0.5")), HOY);
    private ProponerObjetivoConSamiResponse propuesta;
    private Throwable error;

    @BeforeEach
    void setUp() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(propuestaMiniObjetivosService.medidor(NEGOCIO_ID, HOY)).thenReturn(medidor);
        when(propuestaMiniObjetivosService.candidatos(NEGOCIO_ID, medidor)).thenReturn(catalogo());
        when(propuestaMiniObjetivosService.zona(NEGOCIO_ID)).thenReturn(AnalisisZona.sinZona());
        when(controlPlanService.cuotaObjetivosIa(NEGOCIO_ID, AHORA)).thenReturn(new CuotaObjetivosIa(2, 5));
    }

    @Test
    @DisplayName("Contesta con lo que la IA respondió y cómo entendió el objetivo")
    void executeShouldReturnWhatTheIaAnswered() {
        givenLaIaResponde(respuestaIa(medido("c3"), manual(TITULO_ACCION, JUSTIFICACION_ACCION)));

        whenSeProponeConIaHabilitada(mensajes());

        thenSeContestaLoQueRespondioLaIa();
    }

    @Test
    @DisplayName("Propone las métricas del catálogo y las acciones manuales que validó")
    void executeShouldReturnTheValidatedMinisOfTheIa() {
        givenLaIaResponde(respuestaIa(medido("c3"), manual(TITULO_ACCION, JUSTIFICACION_ACCION)));

        whenSeProponeConIaHabilitada(mensajes());

        thenSeProponenLaMetricaDelCatalogoYLaAccionManual();
    }

    @Test
    @DisplayName("Con la cuota del mes agotada no consulta a la IA: explica y ofrece el seguimiento manual")
    void executeShouldNotConsultTheIaWhenTheMonthlyAllowanceIsUsed() {
        givenLaCuotaDelMesAgotada();

        whenSeProponeConIaHabilitada(mensajes());

        thenSeExplicaQueSeAgotoLaCuotaYSeOfreceElSeguimientoManual();
    }

    @Test
    @DisplayName("Con la cuota del mes agotada no consulta nada de la IA ni de su consumo")
    void executeShouldNotTouchTheIaNorItsConsumptionWhenTheMonthlyAllowanceIsUsed() {
        givenLaCuotaDelMesAgotada();

        whenSeProponeConIaHabilitada(mensajes());

        thenNoSeConsultaLaIaNiSuConsumo();
    }

    @Test
    @DisplayName("En un plan sin límite la cuota viene sin límite ni restantes y no se agota")
    void executeShouldReportAnUnlimitedAllowance() {
        givenUnPlanSinLimiteDeObjetivosConIa();
        givenLaIaResponde(respuestaIa());

        whenSeProponeConIaHabilitada(mensajes());

        thenLaCuotaVieneSinLimiteNiRestantes();
    }

    @Test
    @DisplayName("Dice qué fuentes de datos tiene el negocio y cuáles no")
    void executeShouldTellWhichDataSourcesThereAre() {
        givenLaIaResponde(respuestaIa());

        whenSeProponeConIaHabilitada(mensajes());

        thenSeDiceQueFuentesDeDatosHay();
    }

    @Test
    @DisplayName("A la IA le manda la conversación sin datos personales")
    void executeShouldSendTheIaTheSafeConversation() {
        givenLaIaResponde(respuestaIa());

        whenSeProponeConIaHabilitada(mensajeConUnCorreo());

        thenLaIaRecibeLaConversacionSinDatosPersonales();
    }

    @Test
    @DisplayName("A la IA le manda el catálogo y los datos del negocio")
    void executeShouldSendTheIaTheCatalogAndTheContext() {
        givenLaIaResponde(respuestaIa());

        whenSeProponeConIaHabilitada(mensajes());

        thenLaIaRecibeElCatalogoYLosDatosDelNegocio();
    }

    @Test
    @DisplayName("Al catálogo de la IA van como mucho las 12 métricas que más suman")
    void executeShouldSendAtMostTwelveMetrics() {
        givenUnCatalogoDeMasDeDoceMetricas();
        givenLaIaResponde(respuestaIa());

        whenSeProponeConIaHabilitada(mensajes());

        thenLaIaRecibeComoMuchoDoceMetricas();
    }

    @Test
    @DisplayName("Reserva una consulta del día y registra lo que gastó la IA")
    void executeShouldReserveAndRegisterTheConsumption() {
        givenLaIaResponde(respuestaIa());

        whenSeProponeConIaHabilitada(mensajes());

        thenSeReservaUnaConsultaYSeRegistraElConsumo();
    }

    @Test
    @DisplayName("Una meta en pesos no arma mini-objetivos y no se puede guardar")
    void executeShouldReportAMonetaryGoal() {
        givenLaIaResponde(new PropuestaObjetivoIa(RESPUESTA_IA, ENTENDIDO, true, List.of(medido("c1")), CONSUMO));

        whenSeProponeConIaHabilitada(mensajes());

        thenLaMetaEnPesosNoArmaMinisNiSePuedeGuardar();
    }

    @Test
    @DisplayName("Con la IA apagada propone el seguimiento manual de respaldo")
    void executeShouldFallBackWhenTheIaIsDisabled() {
        whenSeProponeConIaApagada(mensajes());

        thenSeProponeElSeguimientoManualDeRespaldo();
    }

    @Test
    @DisplayName("Con la IA apagada no consulta nada")
    void executeShouldNotConsultAnythingWhenTheIaIsDisabled() {
        whenSeProponeConIaApagada(mensajes());

        thenNoSeConsultaLaIaNiSuConsumo();
    }

    @Test
    @DisplayName("Sin acceso a la IA propone el respaldo y no la consulta")
    void executeShouldFallBackWithoutAccessToTheIa() {
        givenUnNegocioSinAccesoALaIa();

        whenSeProponeConIaHabilitada(mensajes());

        thenSeProponeElRespaldoSinConsultarLaIa();
    }

    @Test
    @DisplayName("Con el límite diario agotado propone el respaldo y no consulta a la IA")
    void executeShouldFallBackWhenTheDailyLimitIsReached() {
        givenElLimiteDiarioAgotado();

        whenSeProponeConIaHabilitada(mensajes());

        thenSeProponeElRespaldoSinConsultarLaIaNiRegistrarConsumo();
    }

    @Test
    @DisplayName("Si la IA falla propone el respaldo y no registra consumo")
    void executeShouldFallBackWhenTheIaFails() {
        givenUnaIaQueFalla();

        whenSeProponeConIaHabilitada(mensajes());

        thenSeProponeElRespaldoSinRegistrarConsumo();
    }

    @Test
    @DisplayName("Si la IA no da una respuesta válida propone el respaldo, y su consumo queda registrado")
    void executeShouldFallBackWhenTheIaAnswerIsInvalid() {
        givenLaIaResponde(new PropuestaObjetivoIa(" ", ENTENDIDO, false, List.of(), CONSUMO));

        whenSeProponeConIaHabilitada(mensajes());

        thenSeProponeElRespaldoYSeRegistraElConsumo();
    }

    @Test
    @DisplayName("Una conversación sin mensajes se rechaza antes de consultar nada")
    void executeShouldRejectAnEmptyConversation() {
        whenSeProponeConIaHabilitada(List.of());

        thenSeRechazaLaConversacionVaciaSinConsultarNada();
    }

    @Test
    @DisplayName("Un usuario sin negocio responde como inexistente")
    void executeShouldFailWithoutBusiness() {
        givenUnUsuarioSinNegocio();

        whenSeProponeConIaHabilitada(mensajes());

        thenRespondeComoInexistenteSinConsultarLaIa();
    }

    // --- arrange ---
    private void givenLaIaResponde(PropuestaObjetivoIa respuesta) {
        when(consumoSamiRepository.reservar(NEGOCIO_ID, HOY, LIMITE_DIARIO, AHORA)).thenReturn(true);
        when(conversacionObjetivosIaPort.conversar(any(ConsultaObjetivoIa.class))).thenReturn(respuesta);
    }

    private void givenUnaIaQueFalla() {
        when(consumoSamiRepository.reservar(NEGOCIO_ID, HOY, LIMITE_DIARIO, AHORA)).thenReturn(true);
        when(conversacionObjetivosIaPort.conversar(any(ConsultaObjetivoIa.class)))
                .thenThrow(new IllegalStateException("La IA no respondió"));
    }

    private void givenLaCuotaDelMesAgotada() {
        when(controlPlanService.cuotaObjetivosIa(NEGOCIO_ID, AHORA)).thenReturn(new CuotaObjetivosIa(5, 5));
    }

    private void givenUnPlanSinLimiteDeObjetivosConIa() {
        when(controlPlanService.cuotaObjetivosIa(NEGOCIO_ID, AHORA)).thenReturn(new CuotaObjetivosIa(40, null));
    }

    private void givenUnCatalogoDeMasDeDoceMetricas() {
        when(propuestaMiniObjetivosService.candidatos(NEGOCIO_ID, medidor))
                .thenReturn(Collections.nCopies(METRICAS_DE_SOBRA, catalogo().getFirst()));
    }

    private void givenUnNegocioSinAccesoALaIa() {
        doThrow(new RestriccionPlanException("IA_NO_HABILITADA", "No habilitada"))
                .when(controlPlanService).exigirIa(NEGOCIO_ID);
    }

    private void givenElLimiteDiarioAgotado() {
        when(consumoSamiRepository.reservar(NEGOCIO_ID, HOY, LIMITE_DIARIO, AHORA)).thenReturn(false);
    }

    private void givenUnUsuarioSinNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID))
                .thenThrow(new ResourceNotFoundException("Negocio no encontrado"));
    }

    // --- act ---
    private void whenSeProponeConIaHabilitada(List<MensajeObjetivo> mensajes) {
        whenSePropone(true, mensajes);
    }

    private void whenSeProponeConIaApagada(List<MensajeObjetivo> mensajes) {
        whenSePropone(false, mensajes);
    }

    private void whenSePropone(boolean iaHabilitada, List<MensajeObjetivo> mensajes) {
        ProponerObjetivoConSami proponer = new ProponerObjetivoConSami(
                negocioRepository, propuestaMiniObjetivosService, controlPlanService, consumoSamiRepository,
                conversacionObjetivosIaPort, CLOCK, iaHabilitada, LIMITE_DIARIO, MAXIMO_MENSAJES);
        error = catchThrowable(() -> propuesta =
                proponer.execute(new ProponerObjetivoConSamiRequest(USUARIO_ID, mensajes)));
    }

    // --- assert ---
    private void thenSeContestaLoQueRespondioLaIa() {
        assertThat(propuesta.respuesta()).isEqualTo(RESPUESTA_IA);
        assertThat(propuesta.entendido()).isEqualTo(ENTENDIDO);
        assertThat(propuesta.origen()).isEqualTo(OrigenPropuesta.IA);
        assertThat(propuesta.tipo()).isEqualTo(TipoObjetivo.HIBRIDO);
        assertThat(propuesta.puedeGuardar()).isTrue();
        assertThat(propuesta.esMonetario()).isFalse();
        assertThat(propuesta.mensajesRestantes()).isEqualTo(MAXIMO_MENSAJES - 1);
        assertThat(propuesta.cuota()).isEqualTo(new CuotaItem(2, 5, 3));
    }

    private void thenSeProponenLaMetricaDelCatalogoYLaAccionManual() {
        assertThat(propuesta.minis()).extracting(item -> item.modo())
                .containsExactly(ModoMiniObjetivo.MEDIDO, ModoMiniObjetivo.MANUAL);
        assertThat(propuesta.minis().get(0).titulo()).isEqualTo(catalogo().get(2).titulo());
        assertThat(propuesta.minis().get(1).tipo()).isNull();
    }

    private void thenSeExplicaQueSeAgotoLaCuotaYSeOfreceElSeguimientoManual() {
        assertThat(propuesta.origen()).isEqualTo(OrigenPropuesta.RESPALDO);
        assertThat(propuesta.respuesta()).startsWith("Ya armaste los 5 objetivos con Sami").contains("«Planes»");
        assertThat(propuesta.tipo()).isEqualTo(TipoObjetivo.SIN_DATOS);
        assertThat(propuesta.puedeGuardar()).isTrue();
        assertThat(propuesta.cuota()).isEqualTo(new CuotaItem(5, 5, 0));
    }

    private void thenNoSeConsultaLaIaNiSuConsumo() {
        verifyNoInteractions(consumoSamiRepository, conversacionObjetivosIaPort);
        verify(controlPlanService, never()).exigirIa(any());
    }

    private void thenLaCuotaVieneSinLimiteNiRestantes() {
        assertThat(propuesta.origen()).isEqualTo(OrigenPropuesta.IA);
        assertThat(propuesta.cuota()).isEqualTo(new CuotaItem(40, null, null));
    }

    private void thenSeDiceQueFuentesDeDatosHay() {
        assertThat(propuesta.datos()).extracting(dato -> dato.fuente(), dato -> dato.hay())
                .contains(tuple(FuenteDato.VENTAS, true), tuple(FuenteDato.INVENTARIO, true),
                        tuple(FuenteDato.ZONA, false));
    }

    private void thenLaIaRecibeLaConversacionSinDatosPersonales() {
        assertThat(consultaALaIa().mensajes()).extracting(MensajeObjetivo::texto)
                .containsExactly("Quiero cobrarle a [dato oculto]");
    }

    private void thenLaIaRecibeElCatalogoYLosDatosDelNegocio() {
        ConsultaObjetivoIa consulta = consultaALaIa();
        assertThat(consulta.catalogo()).isEqualTo(catalogo());
        assertThat(consulta.contexto().hayVentas()).isTrue();
        assertThat(consulta.contexto().productos()).hasSize(3);
    }

    private void thenLaIaRecibeComoMuchoDoceMetricas() {
        assertThat(consultaALaIa().catalogo()).hasSize(MAXIMO_DE_METRICAS_PARA_LA_IA);
    }

    private void thenSeReservaUnaConsultaYSeRegistraElConsumo() {
        verify(controlPlanService).exigirIa(NEGOCIO_ID);
        verify(consumoSamiRepository).reservar(NEGOCIO_ID, HOY, LIMITE_DIARIO, AHORA);
        verify(consumoSamiRepository).registrar(NEGOCIO_ID, HOY, CONSUMO, AHORA);
    }

    private void thenLaMetaEnPesosNoArmaMinisNiSePuedeGuardar() {
        assertThat(propuesta.esMonetario()).isTrue();
        assertThat(propuesta.tipo()).isNull();
        assertThat(propuesta.minis()).isEmpty();
        assertThat(propuesta.puedeGuardar()).isFalse();
    }

    private void thenSeProponeElSeguimientoManualDeRespaldo() {
        assertThat(propuesta.origen()).isEqualTo(OrigenPropuesta.RESPALDO);
        assertThat(propuesta.tipo()).isEqualTo(TipoObjetivo.SIN_DATOS);
        assertThat(propuesta.minis()).singleElement().satisfies(mini -> {
            assertThat(mini.modo()).isEqualTo(ModoMiniObjetivo.MANUAL);
            assertThat(mini.justificacion()).contains(OBJETIVO);
        });
        assertThat(propuesta.puedeGuardar()).isTrue();
    }

    private void thenSeProponeElRespaldoSinConsultarLaIa() {
        assertThat(propuesta.origen()).isEqualTo(OrigenPropuesta.RESPALDO);
        verifyNoInteractions(conversacionObjetivosIaPort, consumoSamiRepository);
    }

    private void thenSeProponeElRespaldoSinConsultarLaIaNiRegistrarConsumo() {
        assertThat(propuesta.origen()).isEqualTo(OrigenPropuesta.RESPALDO);
        verifyNoInteractions(conversacionObjetivosIaPort);
        verify(consumoSamiRepository, never()).registrar(any(), any(), any(), any());
    }

    private void thenSeProponeElRespaldoSinRegistrarConsumo() {
        assertThat(propuesta.origen()).isEqualTo(OrigenPropuesta.RESPALDO);
        assertThat(propuesta.puedeGuardar()).isTrue();
        verify(consumoSamiRepository, never()).registrar(any(), any(), any(), any());
    }

    private void thenSeProponeElRespaldoYSeRegistraElConsumo() {
        assertThat(propuesta.origen()).isEqualTo(OrigenPropuesta.RESPALDO);
        verify(consumoSamiRepository).registrar(NEGOCIO_ID, HOY, CONSUMO, AHORA);
    }

    private void thenSeRechazaLaConversacionVaciaSinConsultarNada() {
        assertThat(error).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(propuestaMiniObjetivosService, controlPlanService, consumoSamiRepository,
                conversacionObjetivosIaPort);
    }

    private void thenRespondeComoInexistenteSinConsultarLaIa() {
        assertThat(error).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(conversacionObjetivosIaPort);
    }

    // --- helpers ---
    private ConsultaObjetivoIa consultaALaIa() {
        ArgumentCaptor<ConsultaObjetivoIa> captor = ArgumentCaptor.forClass(ConsultaObjetivoIa.class);
        verify(conversacionObjetivosIaPort).conversar(captor.capture());
        return captor.getValue();
    }

    private static List<MensajeObjetivo> mensajeConUnCorreo() {
        return List.of(new MensajeObjetivo(RolMensaje.PERSONA, "Quiero cobrarle a ana@mail.com"));
    }
}
