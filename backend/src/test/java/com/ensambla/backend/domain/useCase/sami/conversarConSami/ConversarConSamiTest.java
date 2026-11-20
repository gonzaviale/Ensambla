package com.ensambla.backend.domain.useCase.sami.conversarConSami;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.sami.ConsultaIaSami;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.sami.PropuestaIaSami;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.SamiIaPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.support.SamiPruebas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversarConSamiTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("c0000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("c0000000-0000-4000-8000-000000000002");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");
    private static final OffsetDateTime TARDE_EN_ARGENTINA = OffsetDateTime.parse("2026-10-19T22:30:00-03:00");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final int MAX_PREGUNTAS = 3;
    private static final int SIN_PREGUNTAS = 0;
    private static final int LIMITE_IA = 20;
    private static final String TEXTO_AMBIGUO = "quiero un stock y llevar los pedidos";
    private static final ConsumoIaSami CONSUMO = new ConsumoIaSami("bedrock", "haiku", 300, 40);

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private PlantillaRepository plantillaRepository;

    @Mock
    private SamiIaPort samiIaPort;

    @Mock
    private ConsumoSamiRepository consumoSamiRepository;

    @Mock
    private ControlPlanService controlPlanService;

    private List<PlantillaProyecto> plantillas;
    private int maxPreguntas = MAX_PREGUNTAS;
    private OffsetDateTime ahora = AHORA;
    private ConversarConSamiResponse respuesta;
    private Throwable error;

    @BeforeEach
    void dadasLasPlantillasDeSami() {
        plantillas = SamiPruebas.plantillas();
        when(plantillaRepository.findAllParaProyectoActivas()).thenReturn(plantillas);
    }

    @Test
    @DisplayName("Con un texto claro propone la plantilla con sus columnas extra")
    void conUnTextoClaroProponeLaPlantilla() {
        cuandoLaPersonaEscribe("Vendo velas y anoto quién me debe");

        entoncesSePropone("Ventas", "ALGORITMO");
        entoncesLasColumnasExtraSon("Saldo pendiente");
        entoncesSeHicieronPreguntas(0);
    }

    @Test
    @DisplayName("Con un texto claro no pregunta ni toca la IA")
    void conUnTextoClaroNoPreguntaNiTocaLaIa() {
        cuandoLaPersonaEscribe("Vendo velas y anoto quién me debe");

        entoncesNoHayPregunta();
        entoncesNoSeUsoLaIaNiSeBuscoElNegocio();
    }

    @Test
    @DisplayName("Con dos candidatas empatadas pregunta lo que las separa, con respuestas rápidas")
    void conDosCandidatasEmpatadasPreguntaLoQueLasSepara() {
        cuandoLaPersonaEscribe(TEXTO_AMBIGUO);

        entoncesSePregunta("principal");
        entoncesLasOpcionesSon("ventas", "stock", "pedidos", "gastos", "clientes", "otra");
        entoncesLaPrimeraOpcionDice("Lo que vendo");
    }

    @Test
    @DisplayName("Entre ventas y stock prefiere la pregunta específica a la general")
    void entreVentasYStockPrefiereLaPreguntaEspecifica() {
        cuandoLaPersonaEscribe("vendo y llevo el stock de mi inventario");

        entoncesSePregunta("venta-o-stock");
    }

    @Test
    @DisplayName("Con una sola candidata débil pide confirmación con la primera pregunta que la incluye")
    void conUnaSolaCandidataDebilPideConfirmacion() {
        cuandoLaPersonaEscribe("vendo velas");

        entoncesSePregunta("venta-o-stock");
    }

    @Test
    @DisplayName("Sin ninguna pista hace la pregunta general")
    void sinNingunaPistaHaceLaPreguntaGeneral() {
        cuandoLaPersonaEscribe("hola, necesito ayuda");

        entoncesSePregunta("principal");
        entoncesNoHayPropuesta();
    }

    @Test
    @DisplayName("Tras responder queda decidida y propone")
    void trasResponderQuedaDecididaYPropone() {
        cuandoLaPersonaEscribe("vendo y llevo el stock de mi inventario", new RespuestaSamiItem("venta-o-stock", "stock"));

        entoncesSePropone("Inventario", "ALGORITMO");
        entoncesSeHicieronPreguntas(1);
    }

    @Test
    @DisplayName("Una respuesta que sí elige una plantilla no corta las preguntas")
    void unaRespuestaQueEligeUnaPlantillaNoCortaLasPreguntas() {
        cuandoLaPersonaEscribe(TEXTO_AMBIGUO, new RespuestaSamiItem("venta-o-stock", "stock"));

        entoncesSePreguntaOtraVez();
        entoncesSeHicieronPreguntas(1);
    }

    @Test
    @DisplayName("Si responde «otra cosa» no sigue preguntando por plantillas y pasa directo a la IA")
    void siRespondeOtraCosaPasaDirectoALaIa() {
        dadoQueLaIaPropone("Gastos");

        cuandoLaPersonaEscribe("quiero ordenar mi negocio", new RespuestaSamiItem("principal", "otra"));

        entoncesLaPropuestaVieneDe("IA");
        entoncesNoHayPregunta();
        entoncesSeHicieronPreguntas(1);
    }

    @Test
    @DisplayName("Al llegar al máximo de preguntas deriva al respaldo con IA")
    void alLlegarAlMaximoDePreguntasDerivaALaIa() {
        dadoQueSamiPuedeHacer(1);
        dadoQueLaIaPropone("Inventario");

        cuandoLaPersonaEscribe(TEXTO_AMBIGUO, new RespuestaSamiItem("venta-o-stock", "stock"));

        entoncesLaPropuestaVieneDe("IA");
        entoncesSeHicieronPreguntas(1);
    }

    @Test
    @DisplayName("Si ninguna pregunta separa a las candidatas deriva al respaldo con IA")
    void siNingunaPreguntaSeparaALasCandidatasDerivaALaIa() {
        dadoQueSoloHayLaPlantilla("Otra");
        dadoQueLaIaPropone("Otra");

        cuandoLaPersonaEscribe("hola");

        entoncesSePropone("Otra", "IA");
    }

    @Test
    @DisplayName("Si el algoritmo no alcanza la IA propone con su confianza y se registra el consumo")
    void siElAlgoritmoNoAlcanzaProponeLaIaYSeRegistraElConsumo() {
        dadoQueSamiPuedeHacer(SIN_PREGUNTAS);
        dadoQueLaIaPropone("Gastos");

        cuandoLaPersonaEscribe("pago cosas");

        entoncesSePropone("Gastos", "IA");
        entoncesLaConfianzaEs(0.9);
        entoncesSeRegistroElConsumo();
    }

    @Test
    @DisplayName("Con el límite diario agotado no llama a la IA")
    void conElLimiteDiarioAgotadoNoLlamaALaIa() {
        dadoQueSamiPuedeHacer(SIN_PREGUNTAS);
        dadoQueSeAgotoElLimiteDelDia(HOY, AHORA);

        cuandoLaPersonaIntentaEscribir("texto");

        entoncesSeRechazaPorque(ConversarConSami.LIMITE_ALCANZADO);
        entoncesNoSeConsultoLaIa();
        entoncesNoSeRegistroConsumo();
    }

    @Test
    @DisplayName("Si la IA no está habilitada para el dueño, no reserva ni consulta")
    void siLaIaNoEstaHabilitadaNoReservaNiConsulta() {
        dadoQueSamiPuedeHacer(SIN_PREGUNTAS);
        dadoQueLaIaNoEstaHabilitada();

        cuandoLaPersonaIntentaEscribir("texto");

        entoncesElPlanLoRestringePorque("La IA no está habilitada para tu cuenta.");
        entoncesNoSeConsultoLaIaNiSeReservo();
    }

    @Test
    @DisplayName("El día del límite es el argentino aunque en UTC ya sea el día siguiente")
    void elDiaDelLimiteEsElArgentino() {
        dadoQueSamiPuedeHacer(SIN_PREGUNTAS);
        dadoQueAhoraEs(TARDE_EN_ARGENTINA);
        dadoQueSeAgotoElLimiteDelDia(LocalDate.of(2026, 10, 19), TARDE_EN_ARGENTINA.withOffsetSameInstant(ZoneOffset.UTC));

        cuandoLaPersonaIntentaEscribir("texto");

        entoncesSeRechaza();
        entoncesNoSeConsultoLaIa();
    }

    @Test
    @DisplayName("Si la IA falla avisa con un mensaje simple y deja reservada la consulta")
    void siLaIaFallaAvisaConUnMensajeSimple() {
        dadoQueSamiPuedeHacer(SIN_PREGUNTAS);
        dadoQueLaIaFalla();

        cuandoLaPersonaIntentaEscribir("texto");

        entoncesSeRechazaPorque(ConversarConSami.IA_NO_DISPONIBLE);
        entoncesNoSeRegistroConsumo();
    }

    @Test
    @DisplayName("Si la IA inventó una plantilla igual registra lo que gastó y rechaza la propuesta")
    void siLaIaInventoUnaPlantillaRegistraElConsumoYRechaza() {
        dadoQueSamiPuedeHacer(SIN_PREGUNTAS);
        dadoQueLaIaPropone("Peluquería");

        cuandoLaPersonaIntentaEscribir("texto");

        entoncesSeRechazaPorque(ConversarConSami.RESPUESTA_INVALIDA);
        entoncesSeRegistroElConsumo();
    }

    @Test
    @DisplayName("Le pasa a la IA el texto, las plantillas y las respuestas como «pregunta → opción»")
    void lePasaALaIaElTextoLasPlantillasYLasRespuestas() {
        dadoQueLaIaPropone("Gastos");

        cuandoLaPersonaEscribe("quiero ordenar", new RespuestaSamiItem("principal", "otra"));

        entoncesSeConsultoALaIaCon("quiero ordenar", "¿Qué es lo principal que querés anotar? → Otra cosa");
    }

    private void dadoQueSamiPuedeHacer(int preguntas) {
        maxPreguntas = preguntas;
    }

    private void dadoQueAhoraEs(OffsetDateTime momento) {
        ahora = momento;
    }

    private void dadoQueSoloHayLaPlantilla(String nombre) {
        plantillas = List.of(SamiPruebas.plantilla(nombre, SamiPruebas.campo("Nombre", TipoCampo.TEXTO)));
        when(plantillaRepository.findAllParaProyectoActivas()).thenReturn(plantillas);
    }

    private void dadoUnNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    private void dadoQueQuedanConsultasHoy() {
        dadoUnNegocio();
        when(consumoSamiRepository.reservar(NEGOCIO_ID, HOY, LIMITE_IA, AHORA)).thenReturn(true);
    }

    private void dadoQueSeAgotoElLimiteDelDia(LocalDate dia, OffsetDateTime momento) {
        dadoUnNegocio();
        when(consumoSamiRepository.reservar(NEGOCIO_ID, dia, LIMITE_IA, momento)).thenReturn(false);
    }

    private void dadoQueLaIaPropone(String plantilla) {
        dadoQueQuedanConsultasHoy();
        when(samiIaPort.proponer(any())).thenReturn(new PropuestaIaSami(plantilla, List.of(), 0.9, CONSUMO));
    }

    private void dadoQueLaIaFalla() {
        dadoQueQuedanConsultasHoy();
        when(samiIaPort.proponer(any())).thenThrow(new IllegalStateException("timeout"));
    }

    private void dadoQueLaIaNoEstaHabilitada() {
        dadoUnNegocio();
        doThrow(new RestriccionPlanException("IA_NO_HABILITADA", "La IA no está habilitada para tu cuenta."))
                .when(controlPlanService).exigirIa(NEGOCIO_ID);
    }

    private void cuandoLaPersonaEscribe(String texto, RespuestaSamiItem... respuestas) {
        ConversarConSami conversacion = new ConversarConSami(negocioRepository, plantillaRepository,
                SamiPruebas.diccionarioReal(), samiIaPort, consumoSamiRepository, controlPlanService,
                Clock.fixed(ahora.toInstant(), ZoneOffset.UTC), 0.7, maxPreguntas, LIMITE_IA);
        respuesta = conversacion.execute(new ConversarConSamiRequest(USUARIO_ID, texto, List.of(respuestas)));
    }

    private void cuandoLaPersonaIntentaEscribir(String texto) {
        error = catchThrowable(() -> cuandoLaPersonaEscribe(texto));
    }

    private void entoncesSePropone(String plantilla, String origen) {
        assertThat(respuesta.tipo()).isEqualTo("PROPUESTA");
        assertThat(respuesta.propuesta().plantilla()).isEqualTo(plantilla);
        entoncesLaPropuestaVieneDe(origen);
    }

    private void entoncesLaPropuestaVieneDe(String origen) {
        assertThat(respuesta.propuesta().origen()).isEqualTo(origen);
    }

    private void entoncesLasColumnasExtraSon(String... nombres) {
        assertThat(respuesta.propuesta().columnas()).extracting("nombre").containsExactly((Object[]) nombres);
    }

    private void entoncesLaConfianzaEs(double confianza) {
        assertThat(respuesta.propuesta().confianza()).isEqualTo(confianza);
    }

    private void entoncesSeHicieronPreguntas(int preguntas) {
        assertThat(respuesta.preguntasHechas()).isEqualTo(preguntas);
    }

    private void entoncesNoHayPregunta() {
        assertThat(respuesta.pregunta()).isNull();
    }

    private void entoncesNoHayPropuesta() {
        assertThat(respuesta.propuesta()).isNull();
    }

    private void entoncesSePregunta(String pregunta) {
        assertThat(respuesta.tipo()).isEqualTo("PREGUNTA");
        assertThat(respuesta.pregunta().id()).isEqualTo(pregunta);
    }

    private void entoncesSePreguntaOtraVez() {
        assertThat(respuesta.tipo()).isEqualTo("PREGUNTA");
    }

    private void entoncesLasOpcionesSon(String... opciones) {
        assertThat(respuesta.pregunta().opciones()).extracting(OpcionSamiItem::id).containsExactly(opciones);
    }

    private void entoncesLaPrimeraOpcionDice(String texto) {
        assertThat(respuesta.pregunta().opciones().getFirst().texto()).isEqualTo(texto);
    }

    private void entoncesNoSeUsoLaIaNiSeBuscoElNegocio() {
        verifyNoInteractions(samiIaPort, consumoSamiRepository, negocioRepository);
    }

    private void entoncesSeRegistroElConsumo() {
        verify(consumoSamiRepository).registrar(NEGOCIO_ID, HOY, CONSUMO, AHORA);
    }

    private void entoncesNoSeRegistroConsumo() {
        verify(consumoSamiRepository, never()).registrar(any(), any(), any(), any());
    }

    private void entoncesNoSeConsultoLaIa() {
        verifyNoInteractions(samiIaPort);
    }

    private void entoncesNoSeConsultoLaIaNiSeReservo() {
        verifyNoInteractions(samiIaPort, consumoSamiRepository);
    }

    private void entoncesSeRechaza() {
        assertThat(error).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesSeRechazaPorque(String mensaje) {
        assertThat(error).isInstanceOf(BusinessRuleException.class).hasMessage(mensaje);
    }

    private void entoncesElPlanLoRestringePorque(String mensaje) {
        assertThat(error).isInstanceOf(RestriccionPlanException.class).hasMessage(mensaje);
    }

    private void entoncesSeConsultoALaIaCon(String texto, String... aclaraciones) {
        ArgumentCaptor<ConsultaIaSami> captor = ArgumentCaptor.forClass(ConsultaIaSami.class);
        verify(samiIaPort).proponer(captor.capture());
        assertThat(captor.getValue().texto()).isEqualTo(texto);
        assertThat(captor.getValue().plantillas()).isSameAs(plantillas);
        assertThat(captor.getValue().aclaraciones()).containsExactly(aclaraciones);
    }
}