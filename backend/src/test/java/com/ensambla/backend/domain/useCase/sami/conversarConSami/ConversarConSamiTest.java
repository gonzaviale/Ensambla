package com.ensambla.backend.domain.useCase.sami.conversarConSami;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.sami.ConsultaIaSami;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.sami.PropuestaIaSami;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.SamiIaPort;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversarConSamiTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("c0000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("c0000000-0000-4000-8000-000000000002");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final int MAX_PREGUNTAS = 3;
    private static final int LIMITE_IA = 20;
    private static final String TEXTO_AMBIGUO = "quiero un stock y llevar los pedidos";
    private static final ConsumoIaSami CONSUMO = new ConsumoIaSami("bedrock", "haiku", 300, 40);

    @Mock
    private NegocioService negocioService;

    @Mock
    private PlantillaRepository plantillaRepository;

    @Mock
    private SamiIaPort samiIaPort;

    @Mock
    private ConsumoSamiRepository consumoSamiRepository;

    private List<PlantillaProyecto> plantillas;

    @BeforeEach
    void setUp() {
        plantillas = SamiPruebas.plantillas();
        when(plantillaRepository.findAllParaProyectoActivas()).thenReturn(plantillas);
    }

    @Test
    @DisplayName("Con un texto claro propone la plantilla con sus columnas extra sin tocar la IA")
    void executeShouldProposeTemplateForClearTextWithoutAi() {
        ConversarConSamiResponse response = conversar("Vendo velas y anoto quién me debe");

        assertThat(response.tipo()).isEqualTo("PROPUESTA");
        assertThat(response.pregunta()).isNull();
        assertThat(response.propuesta().plantilla()).isEqualTo("Ventas");
        assertThat(response.propuesta().columnas()).extracting("nombre").containsExactly("Saldo pendiente");
        assertThat(response.propuesta().origen()).isEqualTo("ALGORITMO");
        assertThat(response.preguntasHechas()).isZero();
        verifyNoInteractions(samiIaPort, consumoSamiRepository, negocioService);
    }

    @Test
    @DisplayName("Con dos candidatas empatadas pregunta lo que las separa, con respuestas rápidas")
    void executeShouldAskQuestionThatSeparatesTiedCandidates() {
        ConversarConSamiResponse response = conversar(TEXTO_AMBIGUO);

        assertThat(response.tipo()).isEqualTo("PREGUNTA");
        assertThat(response.pregunta().id()).isEqualTo("principal");
        assertThat(response.pregunta().opciones()).extracting(OpcionSamiItem::id)
                .containsExactly("ventas", "stock", "pedidos", "gastos", "clientes", "otra");
        assertThat(response.pregunta().opciones().getFirst().texto()).isEqualTo("Lo que vendo");
    }

    @Test
    @DisplayName("Entre ventas y stock prefiere la pregunta específica a la general")
    void executeShouldPreferSpecificQuestion() {
        ConversarConSamiResponse response = conversar("vendo y llevo el stock de mi inventario");

        assertThat(response.pregunta().id()).isEqualTo("venta-o-stock");
    }

    @Test
    @DisplayName("Con una sola candidata débil pide confirmación con la primera pregunta que la incluye")
    void executeShouldAskConfirmationForSingleWeakCandidate() {
        ConversarConSamiResponse response = conversar("vendo velas");

        assertThat(response.tipo()).isEqualTo("PREGUNTA");
        assertThat(response.pregunta().id()).isEqualTo("venta-o-stock");
    }

    @Test
    @DisplayName("Sin ninguna pista hace la pregunta general")
    void executeShouldAskGeneralQuestionWithoutAnyClue() {
        ConversarConSamiResponse response = conversar("hola, necesito ayuda");

        assertThat(response.pregunta().id()).isEqualTo("principal");
        assertThat(response.propuesta()).isNull();
    }

    @Test
    @DisplayName("Tras responder queda decidida y propone")
    void executeShouldProposeAfterAnswering() {
        ConversarConSamiResponse response = conversar("vendo y llevo el stock de mi inventario",
                new RespuestaSamiItem("venta-o-stock", "stock"));

        assertThat(response.tipo()).isEqualTo("PROPUESTA");
        assertThat(response.propuesta().plantilla()).isEqualTo("Inventario");
        assertThat(response.preguntasHechas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Una respuesta que sí elige una plantilla no corta las preguntas")
    void executeShouldKeepAskingAfterAnAnswerThatPicksATemplate() {
        ConversarConSamiResponse response = conversar(TEXTO_AMBIGUO, new RespuestaSamiItem("venta-o-stock", "stock"));

        assertThat(response.tipo()).isEqualTo("PREGUNTA");
        assertThat(response.preguntasHechas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Si responde «otra cosa» no sigue preguntando por plantillas y pasa directo a la IA")
    void executeShouldFallBackToAiAfterNoneOfThese() {
        givenIaPropone("Gastos");

        ConversarConSamiResponse response = conversar("quiero ordenar mi negocio",
                new RespuestaSamiItem("principal", "otra"));

        assertThat(response.propuesta().origen()).isEqualTo("IA");
        assertThat(response.pregunta()).isNull();
        assertThat(response.preguntasHechas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Al llegar al máximo de preguntas deriva al respaldo con IA")
    void executeShouldFallBackToAiWhenQuestionsAreExhausted() {
        givenIaPropone("Inventario");

        ConversarConSamiResponse response = conversacion(1).execute(new ConversarConSamiRequest(
                USUARIO_ID, TEXTO_AMBIGUO, List.of(new RespuestaSamiItem("venta-o-stock", "stock"))));

        assertThat(response.propuesta().origen()).isEqualTo("IA");
        assertThat(response.preguntasHechas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Si ninguna pregunta separa a las candidatas deriva al respaldo con IA")
    void executeShouldFallBackToAiWhenNoQuestionApplies() {
        plantillas = List.of(SamiPruebas.plantilla("Otra", SamiPruebas.campo("Nombre", TipoCampo.TEXTO)));
        when(plantillaRepository.findAllParaProyectoActivas()).thenReturn(plantillas);
        givenIaPropone("Otra");

        ConversarConSamiResponse response = conversar("hola");

        assertThat(response.propuesta().plantilla()).isEqualTo("Otra");
        assertThat(response.propuesta().origen()).isEqualTo("IA");
    }

    @Test
    @DisplayName("Si el algoritmo no alcanza reserva la consulta, llama a la IA, registra el consumo y marca el origen")
    void executeShouldReserveCallRegisterAndMarkOriginWhenAlgorithmIsNotEnough() {
        givenIaPropone("Gastos");

        ConversarConSamiResponse response = conversacion(0).execute(
                new ConversarConSamiRequest(USUARIO_ID, "pago cosas", List.of()));

        assertThat(response.tipo()).isEqualTo("PROPUESTA");
        assertThat(response.propuesta().plantilla()).isEqualTo("Gastos");
        assertThat(response.propuesta().origen()).isEqualTo("IA");
        assertThat(response.propuesta().confianza()).isEqualTo(0.9);
        verify(consumoSamiRepository).registrar(NEGOCIO_ID, HOY, CONSUMO, AHORA);
    }

    @Test
    @DisplayName("Con el límite diario agotado no llama a la IA")
    void executeShouldNotCallAiWhenLimitIsReached() {
        givenNegocio();
        when(consumoSamiRepository.reservar(NEGOCIO_ID, HOY, LIMITE_IA, AHORA)).thenReturn(false);
        ConversarConSami conversacion = conversacion(0);
        ConversarConSamiRequest request = new ConversarConSamiRequest(USUARIO_ID, "texto", List.of());

        assertThatThrownBy(() -> conversacion.execute(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(ConversarConSami.LIMITE_ALCANZADO);
        verifyNoInteractions(samiIaPort);
        verify(consumoSamiRepository, never()).registrar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("El día del límite es UTC aunque la hora local ya sea otro día")
    void executeShouldUseUtcDayForTheLimit() {
        OffsetDateTime tardeEnArgentina = OffsetDateTime.parse("2026-10-19T22:30:00-03:00");
        givenNegocio();
        when(consumoSamiRepository.reservar(NEGOCIO_ID, LocalDate.of(2026, 10, 20), LIMITE_IA,
                tardeEnArgentina.withOffsetSameInstant(ZoneOffset.UTC))).thenReturn(false);
        ConversarConSami conversacion = new ConversarConSami(negocioService, plantillaRepository,
                SamiPruebas.diccionarioReal(), samiIaPort, consumoSamiRepository,
                Clock.fixed(tardeEnArgentina.toInstant(), ZoneOffset.UTC), 0.7, 0, LIMITE_IA);
        ConversarConSamiRequest request = new ConversarConSamiRequest(USUARIO_ID, "texto", List.of());

        assertThatThrownBy(() -> conversacion.execute(request)).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(samiIaPort);
    }

    @Test
    @DisplayName("Si la IA falla avisa con un mensaje simple y deja reservada la consulta")
    void executeShouldTranslateAiFailure() {
        givenReserva();
        when(samiIaPort.proponer(any())).thenThrow(new IllegalStateException("timeout"));
        ConversarConSami conversacion = conversacion(0);
        ConversarConSamiRequest request = new ConversarConSamiRequest(USUARIO_ID, "texto", List.of());

        assertThatThrownBy(() -> conversacion.execute(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(ConversarConSami.IA_NO_DISPONIBLE);
        verify(consumoSamiRepository, never()).registrar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Si la IA inventó una plantilla igual registra lo que gastó y rechaza la propuesta")
    void executeShouldRegisterConsumptionAndRejectInvalidProposal() {
        givenIaPropone("Peluquería");
        ConversarConSami conversacion = conversacion(0);
        ConversarConSamiRequest request = new ConversarConSamiRequest(USUARIO_ID, "texto", List.of());

        assertThatThrownBy(() -> conversacion.execute(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(ConversarConSami.RESPUESTA_INVALIDA);
        verify(consumoSamiRepository).registrar(NEGOCIO_ID, HOY, CONSUMO, AHORA);
    }

    @Test
    @DisplayName("Le pasa a la IA el texto, las plantillas y las respuestas como «pregunta → opción»")
    void executeShouldSendTextTemplatesAndReadableAnswers() {
        givenIaPropone("Gastos");

        conversar("quiero ordenar", new RespuestaSamiItem("principal", "otra"));

        ArgumentCaptor<ConsultaIaSami> captor = ArgumentCaptor.forClass(ConsultaIaSami.class);
        verify(samiIaPort).proponer(captor.capture());
        assertThat(captor.getValue().texto()).isEqualTo("quiero ordenar");
        assertThat(captor.getValue().plantillas()).isSameAs(plantillas);
        assertThat(captor.getValue().aclaraciones())
                .containsExactly("¿Qué es lo principal que querés anotar? → Otra cosa");
    }

    // --- arrange ---
    private ConversarConSami conversacion(int maxPreguntas) {
        return new ConversarConSami(negocioService, plantillaRepository, SamiPruebas.diccionarioReal(), samiIaPort,
                consumoSamiRepository, Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC), 0.7, maxPreguntas,
                LIMITE_IA);
    }

    private void givenNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    private void givenReserva() {
        givenNegocio();
        when(consumoSamiRepository.reservar(NEGOCIO_ID, HOY, LIMITE_IA, AHORA)).thenReturn(true);
    }

    private void givenIaPropone(String plantilla) {
        givenReserva();
        when(samiIaPort.proponer(any())).thenReturn(new PropuestaIaSami(plantilla, List.of(), 0.9, CONSUMO));
    }

    // --- act ---
    private ConversarConSamiResponse conversar(String texto, RespuestaSamiItem... respuestas) {
        return conversacion(MAX_PREGUNTAS).execute(new ConversarConSamiRequest(USUARIO_ID, texto, List.of(respuestas)));
    }
}
