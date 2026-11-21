package com.ensambla.backend.infrastructure.ia;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Tool;
import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;
import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static com.ensambla.backend.infrastructure.ia.RespuestasAnthropicDePrueba.anthropicQueResponde;
import static com.ensambla.backend.infrastructure.ia.RespuestasAnthropicDePrueba.herramienta;
import static com.ensambla.backend.infrastructure.ia.RespuestasAnthropicDePrueba.texto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

class ClienteModeloConHerramientasAnthropicTest {

    private static final String MODELO = "claude-haiku-4-5";
    private static final DefinicionHerramienta AGREGAR_PIEZA = new DefinicionHerramienta("agregar_pieza",
            "Suma un componente.", Map.of(
                    "type", "object",
                    "properties", Map.of("tipo", Map.of("type", "string")),
                    "required", List.of("tipo")));
    private static final DefinicionHerramienta USAR_KIT = new DefinicionHerramienta("usar_kit",
            "Arma el kit de un rubro.", Map.of("type", "object", "properties", Map.of("rubro", Map.of("type", "string"))));
    private static final DefinicionHerramienta SIN_PROPIEDADES = new DefinicionHerramienta("nada",
            "Sin argumentos.", Map.of("type", "object"));

    private AnthropicClient anthropic;
    private RespuestaConHerramientas respuesta;
    private Tool herramientaConvertida;
    private Tool sinRequeridos;
    private Tool sinPropiedades;

    @Test
    @DisplayName("Devuelve el texto, las herramientas pedidas con sus argumentos y los tokens")
    void devuelveElTextoLasHerramientasPedidasConSusArgumentosYLosTokens() {
        dadoUnAnthropicQueDevuelveTextoYDosHerramientas();

        cuandoSeCompletaConInstruccionesYContexto();

        entoncesDevuelveElTextoLasLlamadasYLosTokens();
    }

    @Test
    @DisplayName("Una herramienta cuya entrada no es un objeto llega sin argumentos")
    void unaHerramientaCuyaEntradaNoEsUnObjetoLlegaSinArgumentos() {
        dadoUnAnthropicQueDevuelveUnaHerramientaSinObjetoDeEntrada();

        cuandoSeCompletaConInstruccionesYContexto();

        entoncesLaLlamadaLlegaSinArgumentos();
    }

    @Test
    @DisplayName("Manda el modelo, las instrucciones, el contexto, los tokens y las herramientas, sin temperatura")
    void mandaElModeloLasInstruccionesElContextoLosTokensYLasHerramientasSinTemperatura() {
        dadoUnAnthropicQueResponde("Hola");

        cuandoSeCompletaConInstruccionesYContexto();

        entoncesElPedidoLlevaModeloInstruccionesContextoYHerramientasSinTemperatura();
    }

    @Test
    @DisplayName("La foto adjunta va en el mismo mensaje como bloque de imagen en base64, antes del texto del turno")
    void laFotoAdjuntaVaEnElMismoMensajeComoBloqueDeImagenEnBase64AntesDelTextoDelTurno() {
        dadoUnAnthropicQueResponde("Leí la foto.");

        cuandoSeCompletaConUnaFotoAdjunta();

        entoncesLaFotoViajaComoBloqueDeImagenAntesDelTexto();
    }

    @Test
    @DisplayName("Convierte el JSON Schema de cada herramienta: propiedades, requeridos y descripción")
    void conviertelElJsonSchemaDeCadaHerramientaPropiedadesRequeridosYDescripcion() {
        cuandoSeConvierteLaHerramientaAgregarPieza();

        entoncesLaHerramientaTraePropiedadesRequeridosYDescripcion();
    }

    @Test
    @DisplayName("Sin requeridos ni propiedades, la herramienta va con un objeto vacío")
    void sinRequeridosNiPropiedadesLaHerramientaVaConUnObjetoVacio() {
        cuandoSeConvierteUnaHerramientaSinRequeridosYOtraSinPropiedades();

        entoncesAmbasVanSinRequeridosYConPropiedadesVacias();
    }

    // --- dado ---

    private void dadoUnAnthropicQueDevuelveTextoYDosHerramientas() {
        anthropic = anthropicQueResponde(
                texto("Listo, "),
                herramienta("agregar_pieza", Map.of("tipo", "expenses")),
                texto("sumé los gastos."),
                herramienta("editar_campo", Map.of("pieza", "e2", "posicion", 2)));
    }

    private void dadoUnAnthropicQueDevuelveUnaHerramientaSinObjetoDeEntrada() {
        anthropic = anthropicQueResponde(herramienta("usar_kit", null));
    }

    private void dadoUnAnthropicQueResponde(String texto) {
        anthropic = anthropicQueResponde(texto);
    }

    // --- cuando ---

    private void cuandoSeCompletaConInstruccionesYContexto() {
        respuesta = cliente().completar(new PeticionConHerramientas("Sos Sami", "contexto"));
    }

    private void cuandoSeCompletaConUnaFotoAdjunta() {
        respuesta = cliente().completar(new PeticionConHerramientas("i", "contexto",
                List.of(new ImagenTaller("image/jpeg", new byte[]{1, 2}))));
    }

    private void cuandoSeConvierteLaHerramientaAgregarPieza() {
        herramientaConvertida = ClienteModeloConHerramientasAnthropic.aHerramienta(AGREGAR_PIEZA);
    }

    private void cuandoSeConvierteUnaHerramientaSinRequeridosYOtraSinPropiedades() {
        sinRequeridos = ClienteModeloConHerramientasAnthropic.aHerramienta(USAR_KIT);
        sinPropiedades = ClienteModeloConHerramientasAnthropic.aHerramienta(SIN_PROPIEDADES);
    }

    // --- entonces ---

    private void entoncesDevuelveElTextoLasLlamadasYLosTokens() {
        assertThat(respuesta.texto()).isEqualTo("Listo, sumé los gastos.");
        assertThat(respuesta.llamadas()).containsExactly(
                new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "expenses")),
                new LlamadaHerramienta("editar_campo", Map.of("pieza", "e2", "posicion", 2)));
        assertThat(respuesta.proveedor()).isEqualTo("anthropic");
        assertThat(respuesta.modelo()).isEqualTo(MODELO);
        assertThat(respuesta.tokensEntrada()).isEqualTo(RespuestasAnthropicDePrueba.TOKENS_ENTRADA);
        assertThat(respuesta.tokensSalida()).isEqualTo(RespuestasAnthropicDePrueba.TOKENS_SALIDA);
    }

    private void entoncesLaLlamadaLlegaSinArgumentos() {
        assertThat(respuesta.texto()).isEmpty();
        assertThat(respuesta.llamadas()).containsExactly(new LlamadaHerramienta("usar_kit", Map.of()));
    }

    private void entoncesElPedidoLlevaModeloInstruccionesContextoYHerramientasSinTemperatura() {
        MessageCreateParams pedido = pedidoEnviado();
        assertThat(pedido.model().asString()).isEqualTo(MODELO);
        assertThat(pedido.maxTokens()).isEqualTo(900);
        assertThat(pedido.temperature()).isEmpty();
        assertThat(pedido.system().orElseThrow().asString()).isEqualTo("Sos Sami");
        assertThat(pedido.messages().getFirst().content().asBlockParams().getFirst().asText().text())
                .isEqualTo("contexto");
        assertThat(pedido.tools().orElseThrow()).extracting(herramienta -> herramienta.asTool().name())
                .containsExactly("agregar_pieza", "usar_kit", "nada");
    }

    private void entoncesLaFotoViajaComoBloqueDeImagenAntesDelTexto() {
        List<ContentBlockParam> bloques = pedidoEnviado().messages().getFirst().content().asBlockParams();
        assertThat(bloques).hasSize(2);
        assertThat(bloques.getFirst().asImage().source().asBase64().data()).isEqualTo("AQI=");
        assertThat(bloques.getFirst().asImage().source().asBase64().mediaType())
                .isEqualTo(Base64ImageSource.MediaType.IMAGE_JPEG);
        assertThat(bloques.get(1).asText().text()).isEqualTo("contexto");
    }

    private void entoncesLaHerramientaTraePropiedadesRequeridosYDescripcion() {
        assertThat(herramientaConvertida.name()).isEqualTo("agregar_pieza");
        assertThat(herramientaConvertida.description()).contains("Suma un componente.");
        assertThat(herramientaConvertida.inputSchema().properties().orElseThrow()._additionalProperties())
                .containsEntry("tipo", JsonValue.from(Map.of("type", "string")));
        assertThat(herramientaConvertida.inputSchema().required()).contains(List.of("tipo"));
    }

    private void entoncesAmbasVanSinRequeridosYConPropiedadesVacias() {
        assertThat(sinRequeridos.inputSchema().required()).isEmpty();
        assertThat(sinPropiedades.inputSchema().properties().orElseThrow()._additionalProperties()).isEmpty();
    }

    // --- datos ---

    private ClienteModeloConHerramientasAnthropic cliente() {
        return new ClienteModeloConHerramientasAnthropic(anthropic, MODELO, 900,
                List.of(AGREGAR_PIEZA, USAR_KIT, SIN_PROPIEDADES));
    }

    private MessageCreateParams pedidoEnviado() {
        ArgumentCaptor<MessageCreateParams> captor = ArgumentCaptor.forClass(MessageCreateParams.class);
        verify(anthropic.messages()).create(captor.capture());
        return captor.getValue();
    }
}
