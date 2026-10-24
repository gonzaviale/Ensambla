package com.ensambla.backend.domain.service.sami;

import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.sami.DiccionarioSami;
import com.ensambla.backend.domain.model.sami.OpcionSami;
import com.ensambla.backend.domain.model.sami.PasoConversacionSami;
import com.ensambla.backend.domain.model.sami.RespuestaSami;
import com.ensambla.backend.domain.model.sami.TipoPaso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConversacionSamiServiceTest {

    private static final double UMBRAL = 0.7;
    private static final int MAX_PREGUNTAS = 3;

    private ConversacionSamiService service;

    @BeforeEach
    void setUp() {
        DiccionarioSami diccionario = SamiPruebas.diccionarioReal();
        service = new ConversacionSamiService(new InterpretacionSamiService(diccionario), diccionario);
    }

    @Test
    @DisplayName("Si el texto alcanza, propone sin preguntar")
    void siguientePasoShouldProposeWhenTextIsEnough() {
        PasoConversacionSami paso = paso("Vendo velas y anoto quién me debe");

        assertThat(paso.tipo()).isEqualTo(TipoPaso.PROPUESTA);
        assertThat(paso.pregunta()).isNull();
        assertThat(paso.interpretacion().plantilla()).isEqualTo("Ventas");
        assertThat(paso.preguntasHechas()).isZero();
    }

    @Test
    @DisplayName("Con dos candidatas empatadas pregunta lo que las separa, con respuestas rápidas")
    void siguientePasoShouldAskQuestionThatSeparatesTiedCandidates() {
        PasoConversacionSami paso = paso("quiero un stock y llevar los pedidos");

        assertThat(paso.tipo()).isEqualTo(TipoPaso.PREGUNTA);
        assertThat(paso.pregunta().id()).isEqualTo("principal");
        assertThat(paso.pregunta().opciones()).extracting(OpcionSami::id)
                .containsExactly("ventas", "stock", "pedidos", "gastos", "clientes", "otra");
        assertThat(paso.pregunta().opciones().getFirst().texto()).isEqualTo("Lo que vendo");
    }

    @Test
    @DisplayName("Entre ventas y stock prefiere la pregunta específica a la general")
    void siguientePasoShouldPreferSpecificQuestion() {
        PasoConversacionSami paso = paso("vendo y llevo el stock de mi inventario");

        assertThat(paso.pregunta().id()).isEqualTo("venta-o-stock");
    }

    @Test
    @DisplayName("Con una sola candidata débil pide confirmación con la primera pregunta que la incluye")
    void siguientePasoShouldAskConfirmationForSingleWeakCandidate() {
        PasoConversacionSami paso = paso("vendo velas");

        assertThat(paso.tipo()).isEqualTo(TipoPaso.PREGUNTA);
        assertThat(paso.pregunta().id()).isEqualTo("venta-o-stock");
    }

    @Test
    @DisplayName("Sin ninguna pista hace la pregunta general")
    void siguientePasoShouldAskGeneralQuestionWithoutAnyClue() {
        PasoConversacionSami paso = paso("hola, necesito ayuda");

        assertThat(paso.pregunta().id()).isEqualTo("principal");
        assertThat(paso.interpretacion().plantilla()).isNull();
    }

    @Test
    @DisplayName("Tras responder queda decidida y propone")
    void siguientePasoShouldProposeAfterAnswering() {
        PasoConversacionSami paso = paso("vendo y llevo el stock de mi inventario",
                new RespuestaSami("venta-o-stock", "stock"));

        assertThat(paso.tipo()).isEqualTo(TipoPaso.PROPUESTA);
        assertThat(paso.interpretacion().plantilla()).isEqualTo("Inventario");
        assertThat(paso.preguntasHechas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Si responde «otra cosa» no sigue preguntando por plantillas y pasa directo a la IA")
    void siguientePasoShouldFallBackToAiAfterNoneOfThese() {
        PasoConversacionSami paso = paso("quiero ordenar mi negocio", new RespuestaSami("principal", "otra"));

        assertThat(paso.tipo()).isEqualTo(TipoPaso.RESPALDO_IA);
        assertThat(paso.pregunta()).isNull();
        assertThat(paso.preguntasHechas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Una respuesta que sí elige una plantilla no corta las preguntas")
    void siguientePasoShouldKeepAskingAfterAnAnswerThatPicksATemplate() {
        PasoConversacionSami paso = paso("quiero un stock y llevar los pedidos",
                new RespuestaSami("venta-o-stock", "stock"));

        assertThat(paso.tipo()).isEqualTo(TipoPaso.PREGUNTA);
        assertThat(paso.preguntasHechas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Al llegar al máximo de preguntas deriva al respaldo con IA")
    void siguientePasoShouldFallBackToAiWhenQuestionsAreExhausted() {
        PasoConversacionSami paso = service.siguientePaso(
                "quiero un stock y llevar los pedidos", List.of(new RespuestaSami("venta-o-stock", "stock")),
                SamiPruebas.plantillas(), UMBRAL, 1);

        assertThat(paso.tipo()).isEqualTo(TipoPaso.RESPALDO_IA);
        assertThat(paso.pregunta()).isNull();
        assertThat(paso.preguntasHechas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Si ninguna pregunta separa a las candidatas deriva al respaldo con IA")
    void siguientePasoShouldFallBackToAiWhenNoQuestionApplies() {
        List<PlantillaProyecto> soloOtra = List.of(
                SamiPruebas.plantilla("Otra", SamiPruebas.campo("Nombre", TipoCampo.TEXTO)));

        PasoConversacionSami paso = service.siguientePaso("hola", List.of(), soloOtra, UMBRAL, MAX_PREGUNTAS);

        assertThat(paso.tipo()).isEqualTo(TipoPaso.RESPALDO_IA);
    }

    private PasoConversacionSami paso(String texto, RespuestaSami... respuestas) {
        return service.siguientePaso(texto, List.of(respuestas), SamiPruebas.plantillas(), UMBRAL, MAX_PREGUNTAS);
    }
}
