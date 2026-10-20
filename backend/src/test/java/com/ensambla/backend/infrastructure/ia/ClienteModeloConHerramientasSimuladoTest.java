package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.service.sami.SamiPruebas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClienteModeloConHerramientasSimuladoTest {

    private static final KitsRubro KITS = SamiPruebas.kitsReales();
    private static final EstadoTaller VACIO = new EstadoTaller("Mi sistema", "mosaic", List.of());
    private static final EstadoTaller CON_PIEZAS = new EstadoTaller("Mi sistema", "mosaic", List.of(
            new PiezaTaller("e1", "inventory", "Velas", null, List.of())));

    private final ClienteModeloConHerramientasSimulado cliente = new ClienteModeloConHerramientasSimulado(KITS);
    private final PromptTaller prompt = new PromptTaller(
            new ClassPathResource("ia/prompt-taller.md"), SamiPruebas.catalogoReal(), KITS);

    @Test
    @DisplayName("Si la persona nombra gastos, suma la pieza de gastos")
    void completarShouldAddExpensesWhenMessageMentionsThem() {
        RespuestaConHerramientas result = cliente.completar(contextoCon("Mensaje de la persona: sumá los gastos"));

        assertThat(result.llamadas()).hasSize(1);
        assertThat(result.llamadas().getFirst().nombre()).isEqualTo("agregar_pieza");
        assertThat(result.llamadas().getFirst().argumentos()).containsEntry("tipo", "expenses");
        assertThat(result.proveedor()).isEqualTo("simulado");
        assertThat(result.tokensEntrada()).isZero();
    }

    @Test
    @DisplayName("Sin rubro reconocido pregunta de qué es el negocio ofreciendo los primeros cuatro kits")
    void completarShouldAskForTheRubroWithoutKit() {
        RespuestaConHerramientas result = cliente.completar(prompt.peticion(new ConsultaTaller("hola", VACIO, List.of())));

        assertThat(result.llamadas()).hasSize(1);
        assertThat(result.llamadas().getFirst().nombre()).isEqualTo("ofrecer_opciones");
        assertThat(result.llamadas().getFirst().argumentos()).isEqualTo(Map.of(
                "pregunta", ClienteModeloConHerramientasSimulado.PREGUNTA_RUBRO,
                "opciones", List.of("Almacén y kiosco", "Dietética", "Velas y aromas", "Panadería")));
    }

    @Test
    @DisplayName("Con el kit del rubro y el sistema vacío lo arma y hace la pregunta del kit")
    void completarShouldUseKitWhenSystemIsEmpty() {
        RespuestaConHerramientas result = cliente.completar(prompt.peticion(
                new ConsultaTaller("vendo velas", VACIO, List.of(), KITS.buscar("velas").orElseThrow())));

        assertThat(result.llamadas()).extracting(llamada -> llamada.nombre())
                .containsExactly("usar_kit", "ofrecer_opciones");
        assertThat(result.llamadas().getFirst().argumentos()).isEqualTo(Map.of("rubro", "velas"));
        assertThat(result.llamadas().get(1).argumentos()).containsEntry(
                "pregunta", "¿Querés que te avise cuando te quedan pocos materiales?");
        assertThat(result.texto())
                .isEqualTo("Te armé una base para velas y aromas: Velas, Materiales, Ventas, Quién me debe. Mirá cómo queda.");
    }

    @Test
    @DisplayName("Con un sistema con piezas no arma nada: pregunta qué ajustar")
    void completarShouldOnlyAskWhenSystemHasPieces() {
        RespuestaConHerramientas result = cliente.completar(prompt.peticion(
                new ConsultaTaller("dale", CON_PIEZAS, List.of(), KITS.buscar("velas").orElseThrow())));

        assertThat(result.llamadas()).hasSize(1);
        assertThat(result.llamadas().getFirst().argumentos()).isEqualTo(Map.of(
                "pregunta", ClienteModeloConHerramientasSimulado.PREGUNTA_AJUSTE,
                "opciones", ClienteModeloConHerramientasSimulado.OPCIONES_AJUSTE));
    }

    @Test
    @DisplayName("Si el kit que nombra el contexto no existe, pregunta el rubro")
    void completarShouldAskForTheRubroWhenKitIsUnknown() {
        RespuestaConHerramientas result = cliente.completar(contextoCon(
                "Todavía no tiene piezas.\n\nKit del rubro: astronautas (Naves).\n\nMensaje de la persona: hola"));

        assertThat(result.llamadas().getFirst().argumentos())
                .containsEntry("pregunta", ClienteModeloConHerramientasSimulado.PREGUNTA_RUBRO);
    }

    @Test
    @DisplayName("Sólo mira el mensaje de la persona para los gastos, no el resto del contexto")
    void completarShouldIgnoreTheRestOfTheContext() {
        String contexto = "- id e3, tipo expenses, «Gastos»: Detalle (text)\n\nMensaje de la persona: hola";

        RespuestaConHerramientas result = cliente.completar(contextoCon(contexto));

        assertThat(result.llamadas().getFirst().nombre()).isEqualTo("ofrecer_opciones");
    }

    @Test
    @DisplayName("Sin la marca del mensaje mira todo el contexto")
    void completarShouldReadTheWholeContextWhenThereIsNoMarker() {
        RespuestaConHerramientas result = cliente.completar(contextoCon("quiero gastos"));

        assertThat(result.llamadas().getFirst().nombre()).isEqualTo("agregar_pieza");
    }

    private PeticionConHerramientas contextoCon(String contexto) {
        return new PeticionConHerramientas("instrucciones", contexto);
    }
}
