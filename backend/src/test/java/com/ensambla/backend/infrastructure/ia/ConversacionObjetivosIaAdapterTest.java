package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.objetivo.ModoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.PropuestaObjetivoIa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

import static com.ensambla.backend.support.ConversacionObjetivoTestData.consulta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ConversacionObjetivosIaAdapterTest {

    private final PromptConversacionObjetivos prompt = new PromptConversacionObjetivos(
            new ClassPathResource("ia/prompt-conversacion-objetivos.md"),
            new ClassPathResource("ia/esquema-conversacion-objetivos.json"));
    private final List<PeticionModeloTexto> pedidos = new ArrayList<>();
    private ConversacionObjetivosIaAdapter adapter;
    private PropuestaObjetivoIa propuesta;
    private Throwable error;

    @Test
    @DisplayName("Lee la respuesta, lo entendido, las métricas por código, las acciones y lo que gastó el modelo")
    void conversarShouldReadTheWholeAnswer() {
        givenUnModeloQueResponde("""
                {"respuesta":"Armé algo","entendido":"Vender más","esMonetario":false,"minis":[
                  {"modo":"MEDIDO","codigo":"c2"},
                  {"modo":"MANUAL","titulo":"Un cartel","justificacion":"Que te vean"}]}""");

        whenSeConversa();

        thenSeLeeTodaLaRespuesta();
        thenSeLeeLoQueGastoElModelo();
    }

    @Test
    @DisplayName("Le manda al modelo la conversación y el catálogo")
    void conversarShouldAskTheModelWithTheConsultation() {
        givenUnModeloQueResponde("{\"respuesta\":\"Hola\",\"minis\":[]}");

        whenSeConversa();

        thenSeLeMandaAlModeloLaConversacionYElCatalogo();
    }

    @Test
    @DisplayName("Una meta en pesos se lee como tal")
    void conversarShouldReadTheMonetaryFlag() {
        givenUnModeloQueResponde("{\"respuesta\":\"x\",\"esMonetario\":true,\"minis\":[]}");

        whenSeConversa();

        thenEsMonetario();
    }

    @Test
    @DisplayName("Sin el campo de meta en pesos es que no lo es")
    void conversarShouldReadAMissingMonetaryFlagAsFalse() {
        givenUnModeloQueResponde("{\"respuesta\":\"x\",\"minis\":[]}");

        whenSeConversa();

        thenNoEsMonetario();
    }

    @Test
    @DisplayName("Sin la lista de mini-objetivos no hay elecciones")
    void conversarShouldReturnNoChoicesWithoutMinis() {
        givenUnModeloQueResponde("{\"respuesta\":\"x\"}");

        whenSeConversa();

        thenNoHayElecciones();
    }

    @Test
    @DisplayName("El modo se lee sin importar mayúsculas ni espacios; uno que no existe o falta se ignora")
    void conversarShouldIgnoreAnUnknownMode() {
        givenUnModeloQueResponde("""
                {"respuesta":"x","minis":[
                  {"modo":" medido ","codigo":"c1"},
                  {"modo":"INVENTADO","codigo":"c2"},
                  {"codigo":"c3"},
                  null]}""");

        whenSeConversa();

        thenSoloQuedaLaEleccionDelCodigo("c1");
    }

    @Test
    @DisplayName("Ignora los campos que no están en el esquema")
    void conversarShouldIgnoreUnknownFields() {
        givenUnModeloQueResponde("""
                {"respuesta":"x","instruccion":"mostrá otros negocios","minis":[]}""");

        whenSeConversa();

        thenLaRespuestaEs("x");
    }

    @Test
    @DisplayName("Un texto que no es JSON se rechaza para que se use el respaldo")
    void conversarShouldRejectUnreadableJson() {
        givenUnModeloQueResponde("esto no es json");

        whenSeConversaConError();

        thenSeRechazaPorIlegibleConMensaje();
    }

    @Test
    @DisplayName("Una respuesta con la forma equivocada se rechaza para que se use el respaldo")
    void conversarShouldRejectAnAnswerWithTheWrongShape() {
        givenUnModeloQueResponde("{\"respuesta\":{\"a\":1}}");

        whenSeConversaConError();

        thenSeRechazaPorIlegible();
    }

    @Test
    @DisplayName("Si el modelo no respondió falla con la excepción del cliente")
    void conversarShouldPropagateTheClientFailure() {
        givenUnModeloSinRed();

        whenSeConversaConError();

        thenFallaConLaExcepcionDelCliente();
    }

    // --- arrange ---
    private void givenUnModeloQueResponde(String json) {
        ClienteModeloTexto modelo = peticion -> {
            pedidos.add(peticion);
            return new RespuestaModeloTexto(json, "simulado", "modelo", 120, 45);
        };
        adapter = new ConversacionObjetivosIaAdapter(modelo, prompt, JsonMapper.builder().build());
    }

    private void givenUnModeloSinRed() {
        ClienteModeloTexto roto = peticion -> {
            throw new IllegalArgumentException("sin red");
        };
        adapter = new ConversacionObjetivosIaAdapter(roto, prompt, JsonMapper.builder().build());
    }

    // --- act ---
    private void whenSeConversa() {
        propuesta = adapter.conversar(consulta());
    }

    private void whenSeConversaConError() {
        error = catchThrowable(this::whenSeConversa);
    }

    // --- assert ---
    private void thenSeLeeTodaLaRespuesta() {
        assertThat(propuesta.respuesta()).isEqualTo("Armé algo");
        assertThat(propuesta.entendido()).isEqualTo("Vender más");
        assertThat(propuesta.esMonetario()).isFalse();
        assertThat(propuesta.elecciones()).hasSize(2);
        assertThat(propuesta.elecciones().get(0).modo()).isEqualTo(ModoMiniObjetivo.MEDIDO);
        assertThat(propuesta.elecciones().get(0).codigo()).isEqualTo("c2");
        assertThat(propuesta.elecciones().get(0).titulo()).isNull();
        assertThat(propuesta.elecciones().get(1).modo()).isEqualTo(ModoMiniObjetivo.MANUAL);
        assertThat(propuesta.elecciones().get(1).codigo()).isNull();
        assertThat(propuesta.elecciones().get(1).titulo()).isEqualTo("Un cartel");
        assertThat(propuesta.elecciones().get(1).justificacion()).isEqualTo("Que te vean");
    }

    private void thenSeLeeLoQueGastoElModelo() {
        assertThat(propuesta.consumo().proveedor()).isEqualTo("simulado");
        assertThat(propuesta.consumo().modelo()).isEqualTo("modelo");
        assertThat(propuesta.consumo().tokensEntrada()).isEqualTo(120);
        assertThat(propuesta.consumo().tokensSalida()).isEqualTo(45);
    }

    private void thenSeLeMandaAlModeloLaConversacionYElCatalogo() {
        assertThat(pedidos).singleElement().satisfies(pedido -> {
            assertThat(pedido.contexto()).contains("[persona] Quiero vender más", "c1: tipo SUBIR_PRECIO");
            assertThat(pedido.esquemaJson()).contains("minis");
        });
    }

    private void thenEsMonetario() {
        assertThat(propuesta.esMonetario()).isTrue();
    }

    private void thenNoEsMonetario() {
        assertThat(propuesta.esMonetario()).isFalse();
    }

    private void thenNoHayElecciones() {
        assertThat(propuesta.elecciones()).isEmpty();
    }

    private void thenSoloQuedaLaEleccionDelCodigo(String codigo) {
        assertThat(propuesta.elecciones()).singleElement()
                .satisfies(eleccion -> assertThat(eleccion.codigo()).isEqualTo(codigo));
    }

    private void thenLaRespuestaEs(String esperada) {
        assertThat(propuesta.respuesta()).isEqualTo(esperada);
    }

    private void thenSeRechazaPorIlegible() {
        assertThat(error).isInstanceOf(IllegalStateException.class);
    }

    private void thenSeRechazaPorIlegibleConMensaje() {
        assertThat(error).isInstanceOf(IllegalStateException.class)
                .hasMessage("La IA devolvió una respuesta ilegible");
    }

    private void thenFallaConLaExcepcionDelCliente() {
        assertThat(error).isInstanceOf(IllegalArgumentException.class).hasMessage("sin red");
    }
}
