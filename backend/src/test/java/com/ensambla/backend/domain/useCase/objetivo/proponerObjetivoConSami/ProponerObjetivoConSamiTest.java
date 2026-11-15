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
import java.util.ArrayList;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final PropuestaMiniObjetivosService propuestaMiniObjetivosService =
            mock(PropuestaMiniObjetivosService.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final ConsumoSamiRepository consumoSamiRepository = mock(ConsumoSamiRepository.class);
    private final ConversacionObjetivosIaPort conversacionObjetivosIaPort = mock(ConversacionObjetivosIaPort.class);
    private final MedidorMiniObjetivo medidor =
            MedidorMiniObjetivo.de(List.of(almacen()), new MetricasVentas(30, new BigDecimal("0.5")), HOY);

    @BeforeEach
    void setUp() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(propuestaMiniObjetivosService.medidor(NEGOCIO_ID, HOY)).thenReturn(medidor);
        when(propuestaMiniObjetivosService.candidatos(NEGOCIO_ID, medidor)).thenReturn(catalogo());
        when(propuestaMiniObjetivosService.zona(NEGOCIO_ID)).thenReturn(AnalisisZona.sinZona());
        when(controlPlanService.cuotaObjetivosIa(NEGOCIO_ID, AHORA)).thenReturn(new CuotaObjetivosIa(2, 5));
    }

    @Test
    @DisplayName("Contesta con la propuesta de la IA: métricas del catálogo y acciones, y cómo entendió el objetivo")
    void executeShouldReturnTheValidatedProposalOfTheIa() {
        givenIaDisponible();
        when(conversacionObjetivosIaPort.conversar(any(ConsultaObjetivoIa.class)))
                .thenReturn(respuestaIa(medido("c3"), manual(TITULO_ACCION, JUSTIFICACION_ACCION)));

        ProponerObjetivoConSamiResponse result = whenProponer(true, mensajes());

        assertThat(result.respuesta()).isEqualTo(RESPUESTA_IA);
        assertThat(result.entendido()).isEqualTo(ENTENDIDO);
        assertThat(result.origen()).isEqualTo(OrigenPropuesta.IA);
        assertThat(result.tipo()).isEqualTo(TipoObjetivo.HIBRIDO);
        assertThat(result.puedeGuardar()).isTrue();
        assertThat(result.esMonetario()).isFalse();
        assertThat(result.mensajesRestantes()).isEqualTo(MAXIMO_MENSAJES - 1);
        assertThat(result.minis()).extracting(item -> item.modo())
                .containsExactly(ModoMiniObjetivo.MEDIDO, ModoMiniObjetivo.MANUAL);
        assertThat(result.minis().get(0).titulo()).isEqualTo(catalogo().get(2).titulo());
        assertThat(result.minis().get(1).tipo()).isNull();
        assertThat(result.cuota()).isEqualTo(new CuotaItem(2, 5, 3));
    }

    @Test
    @DisplayName("Con la cuota del mes agotada no consulta a la IA: explica y ofrece el seguimiento manual")
    void executeShouldNotConsultTheIaWhenTheMonthlyAllowanceIsUsed() {
        when(controlPlanService.cuotaObjetivosIa(NEGOCIO_ID, AHORA)).thenReturn(new CuotaObjetivosIa(5, 5));

        ProponerObjetivoConSamiResponse result = whenProponer(true, mensajes());

        assertThat(result.origen()).isEqualTo(OrigenPropuesta.RESPALDO);
        assertThat(result.respuesta()).startsWith("Ya armaste los 5 objetivos con Sami").contains("«Planes»");
        assertThat(result.tipo()).isEqualTo(TipoObjetivo.SIN_DATOS);
        assertThat(result.puedeGuardar()).isTrue();
        assertThat(result.cuota()).isEqualTo(new CuotaItem(5, 5, 0));
        verifyNoInteractions(consumoSamiRepository, conversacionObjetivosIaPort);
        verify(controlPlanService, never()).exigirIa(any());
    }

    @Test
    @DisplayName("En un plan sin límite la cuota viene sin límite ni restantes y no se agota")
    void executeShouldReportAnUnlimitedAllowance() {
        givenIaDisponible();
        when(controlPlanService.cuotaObjetivosIa(NEGOCIO_ID, AHORA)).thenReturn(new CuotaObjetivosIa(40, null));
        when(conversacionObjetivosIaPort.conversar(any(ConsultaObjetivoIa.class))).thenReturn(respuestaIa());

        ProponerObjetivoConSamiResponse result = whenProponer(true, mensajes());

        assertThat(result.origen()).isEqualTo(OrigenPropuesta.IA);
        assertThat(result.cuota()).isEqualTo(new CuotaItem(40, null, null));
    }

    @Test
    @DisplayName("Dice qué fuentes de datos tiene el negocio y cuáles no")
    void executeShouldTellWhichDataSourcesThereAre() {
        givenIaDisponible();
        when(conversacionObjetivosIaPort.conversar(any(ConsultaObjetivoIa.class))).thenReturn(respuestaIa());

        ProponerObjetivoConSamiResponse result = whenProponer(true, mensajes());

        assertThat(result.datos()).extracting(dato -> dato.fuente(), dato -> dato.hay())
                .contains(tuple(FuenteDato.VENTAS, true), tuple(FuenteDato.INVENTARIO, true),
                        tuple(FuenteDato.ZONA, false));
    }

    @Test
    @DisplayName("A la IA le manda la conversación sin datos personales, el catálogo y los datos del negocio")
    void executeShouldSendTheIaTheSafeConversationTheCatalogAndTheContext() {
        givenIaDisponible();
        when(conversacionObjetivosIaPort.conversar(any(ConsultaObjetivoIa.class))).thenReturn(respuestaIa());
        List<MensajeObjetivo> conCorreo = List.of(
                new MensajeObjetivo(RolMensaje.PERSONA, "Quiero cobrarle a ana@mail.com"));

        whenProponer(true, conCorreo);

        ArgumentCaptor<ConsultaObjetivoIa> captor = ArgumentCaptor.forClass(ConsultaObjetivoIa.class);
        verify(conversacionObjetivosIaPort).conversar(captor.capture());
        assertThat(captor.getValue().mensajes()).extracting(MensajeObjetivo::texto)
                .containsExactly("Quiero cobrarle a [dato oculto]");
        assertThat(captor.getValue().catalogo()).isEqualTo(catalogo());
        assertThat(captor.getValue().contexto().hayVentas()).isTrue();
        assertThat(captor.getValue().contexto().productos()).hasSize(3);
    }

    @Test
    @DisplayName("Al catálogo de la IA van como mucho las 12 métricas que más suman")
    void executeShouldSendAtMostTwelveMetrics() {
        List<CandidatoMiniObjetivo> muchas = new ArrayList<>();
        for (int indice = 0; indice < 15; indice++) {
            muchas.add(catalogo().getFirst());
        }
        when(propuestaMiniObjetivosService.candidatos(NEGOCIO_ID, medidor)).thenReturn(muchas);
        givenIaDisponible();
        when(conversacionObjetivosIaPort.conversar(any(ConsultaObjetivoIa.class))).thenReturn(respuestaIa());

        whenProponer(true, mensajes());

        ArgumentCaptor<ConsultaObjetivoIa> captor = ArgumentCaptor.forClass(ConsultaObjetivoIa.class);
        verify(conversacionObjetivosIaPort).conversar(captor.capture());
        assertThat(captor.getValue().catalogo()).hasSize(12);
    }

    @Test
    @DisplayName("Reserva una consulta del día y registra lo que gastó la IA")
    void executeShouldReserveAndRegisterTheConsumption() {
        givenIaDisponible();
        when(conversacionObjetivosIaPort.conversar(any(ConsultaObjetivoIa.class))).thenReturn(respuestaIa());

        whenProponer(true, mensajes());

        verify(controlPlanService).exigirIa(NEGOCIO_ID);
        verify(consumoSamiRepository).reservar(NEGOCIO_ID, HOY, LIMITE_DIARIO, AHORA);
        verify(consumoSamiRepository).registrar(NEGOCIO_ID, HOY, CONSUMO, AHORA);
    }

    @Test
    @DisplayName("Una meta en pesos no arma mini-objetivos y no se puede guardar")
    void executeShouldReportAMonetaryGoal() {
        givenIaDisponible();
        when(conversacionObjetivosIaPort.conversar(any(ConsultaObjetivoIa.class))).thenReturn(
                new PropuestaObjetivoIa(RESPUESTA_IA, ENTENDIDO, true, List.of(medido("c1")), CONSUMO));

        ProponerObjetivoConSamiResponse result = whenProponer(true, mensajes());

        assertThat(result.esMonetario()).isTrue();
        assertThat(result.tipo()).isNull();
        assertThat(result.minis()).isEmpty();
        assertThat(result.puedeGuardar()).isFalse();
    }

    @Test
    @DisplayName("Con la IA apagada propone el seguimiento manual de respaldo sin consultar nada")
    void executeShouldFallBackWhenTheIaIsDisabled() {
        ProponerObjetivoConSamiResponse result = whenProponer(false, mensajes());

        assertThat(result.origen()).isEqualTo(OrigenPropuesta.RESPALDO);
        assertThat(result.tipo()).isEqualTo(TipoObjetivo.SIN_DATOS);
        assertThat(result.minis()).singleElement().satisfies(mini -> {
            assertThat(mini.modo()).isEqualTo(ModoMiniObjetivo.MANUAL);
            assertThat(mini.justificacion()).contains(OBJETIVO);
        });
        assertThat(result.puedeGuardar()).isTrue();
        verify(controlPlanService, never()).exigirIa(any());
        verifyNoInteractions(consumoSamiRepository, conversacionObjetivosIaPort);
    }

    @Test
    @DisplayName("Sin acceso a la IA propone el respaldo y no la consulta")
    void executeShouldFallBackWithoutAccessToTheIa() {
        doThrow(new RestriccionPlanException("IA_NO_HABILITADA", "No habilitada"))
                .when(controlPlanService).exigirIa(NEGOCIO_ID);

        ProponerObjetivoConSamiResponse result = whenProponer(true, mensajes());

        assertThat(result.origen()).isEqualTo(OrigenPropuesta.RESPALDO);
        verifyNoInteractions(conversacionObjetivosIaPort, consumoSamiRepository);
    }

    @Test
    @DisplayName("Con el límite diario agotado propone el respaldo y no consulta a la IA")
    void executeShouldFallBackWhenTheDailyLimitIsReached() {
        when(consumoSamiRepository.reservar(NEGOCIO_ID, HOY, LIMITE_DIARIO, AHORA)).thenReturn(false);

        ProponerObjetivoConSamiResponse result = whenProponer(true, mensajes());

        assertThat(result.origen()).isEqualTo(OrigenPropuesta.RESPALDO);
        verifyNoInteractions(conversacionObjetivosIaPort);
        verify(consumoSamiRepository, never()).registrar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Si la IA falla propone el respaldo y no registra consumo")
    void executeShouldFallBackWhenTheIaFails() {
        givenIaDisponible();
        when(conversacionObjetivosIaPort.conversar(any(ConsultaObjetivoIa.class)))
                .thenThrow(new IllegalStateException("La IA no respondió"));

        ProponerObjetivoConSamiResponse result = whenProponer(true, mensajes());

        assertThat(result.origen()).isEqualTo(OrigenPropuesta.RESPALDO);
        assertThat(result.puedeGuardar()).isTrue();
        verify(consumoSamiRepository, never()).registrar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Si la IA no da una respuesta válida propone el respaldo, y su consumo queda registrado")
    void executeShouldFallBackWhenTheIaAnswerIsInvalid() {
        givenIaDisponible();
        when(conversacionObjetivosIaPort.conversar(any(ConsultaObjetivoIa.class))).thenReturn(
                new PropuestaObjetivoIa(" ", ENTENDIDO, false, List.of(), CONSUMO));

        ProponerObjetivoConSamiResponse result = whenProponer(true, mensajes());

        assertThat(result.origen()).isEqualTo(OrigenPropuesta.RESPALDO);
        verify(consumoSamiRepository).registrar(NEGOCIO_ID, HOY, CONSUMO, AHORA);
    }

    @Test
    @DisplayName("Una conversación sin mensajes se rechaza antes de consultar nada")
    void executeShouldRejectAnEmptyConversation() {
        assertThatThrownBy(() -> whenProponer(true, List.of()))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(propuestaMiniObjetivosService, controlPlanService, consumoSamiRepository,
                conversacionObjetivosIaPort);
    }

    @Test
    @DisplayName("Un usuario sin negocio responde como inexistente")
    void executeShouldFailWithoutBusiness() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID))
                .thenThrow(new ResourceNotFoundException("Negocio no encontrado"));

        assertThatThrownBy(() -> whenProponer(true, mensajes()))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(conversacionObjetivosIaPort);
    }

    // --- arrange ---
    private void givenIaDisponible() {
        when(consumoSamiRepository.reservar(NEGOCIO_ID, HOY, LIMITE_DIARIO, AHORA)).thenReturn(true);
    }

    // --- act ---
    private ProponerObjetivoConSamiResponse whenProponer(boolean iaHabilitada, List<MensajeObjetivo> mensajes) {
        ProponerObjetivoConSami proponer = new ProponerObjetivoConSami(
                negocioRepository, propuestaMiniObjetivosService, controlPlanService, consumoSamiRepository,
                conversacionObjetivosIaPort, CLOCK, iaHabilitada, LIMITE_DIARIO, MAXIMO_MENSAJES);
        return proponer.execute(new ProponerObjetivoConSamiRequest(USUARIO_ID, mensajes));
    }
}
