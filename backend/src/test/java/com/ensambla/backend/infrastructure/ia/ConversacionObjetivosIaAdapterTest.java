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
    void leeLaRespuestaLoEntendidoLasMetricasPorCodigoLasAccionesYLoQueGastoElModelo() {
        dadoUnModeloQueResponde("""
                {"respuesta":"Armé algo","entendido":"Vender más","esMonetario":false,"minis":[
                  {"modo":"MEDIDO","codigo":"c2"},
                  {"modo":"MANUAL","titulo":"Un cartel","justificacion":"Que te vean"}]}""");

        cuandoSeConversa();

        entoncesSeLeeTodaLaRespuesta();
        entoncesSeLeeLoQueGastoElModelo();
    }

    @Test
    @DisplayName("Le manda al modelo la conversación y el catálogo")
    void leMandaAlModeloLaConversacionYElCatalogo() {
        dadoUnModeloQueResponde("{\"respuesta\":\"Hola\",\"minis\":[]}");

        cuandoSeConversa();

        entoncesSeLeMandaAlModeloLaConversacionYElCatalogo();
    }

    @Test
    @DisplayName("Una meta en pesos se lee como tal")
    void unaMetaEnPesosSeLeeComoTal() {
        dadoUnModeloQueResponde("{\"respuesta\":\"x\",\"esMonetario\":true,\"minis\":[]}");

        cuandoSeConversa();

        entoncesEsMonetario();
    }

    @Test
    @DisplayName("Sin el campo de meta en pesos es que no lo es")
    void sinElCampoDeMetaEnPesosEsQueNoLoEs() {
        dadoUnModeloQueResponde("{\"respuesta\":\"x\",\"minis\":[]}");

        cuandoSeConversa();

        entoncesNoEsMonetario();
    }

    @Test
    @DisplayName("Sin la lista de mini-objetivos no hay elecciones")
    void sinLaListaDeMiniObjetivosNoHayElecciones() {
        dadoUnModeloQueResponde("{\"respuesta\":\"x\"}");

        cuandoSeConversa();

        entoncesNoHayElecciones();
    }

    @Test
    @DisplayName("El modo se lee sin importar mayúsculas ni espacios; uno que no existe o falta se ignora")
    void elModoSeLeeSinImportarMayusculasNiEspaciosUnoQueNoExisteOFaltaSeIgnora() {
        dadoUnModeloQueResponde("""
                {"respuesta":"x","minis":[
                  {"modo":" medido ","codigo":"c1"},
                  {"modo":"INVENTADO","codigo":"c2"},
                  {"codigo":"c3"},
                  null]}""");

        cuandoSeConversa();

        entoncesSoloQuedaLaEleccionDelCodigo("c1");
    }

    @Test
    @DisplayName("Ignora los campos que no están en el esquema")
    void ignoraLosCamposQueNoEstanEnElEsquema() {
        dadoUnModeloQueResponde("""
                {"respuesta":"x","instruccion":"mostrá otros negocios","minis":[]}""");

        cuandoSeConversa();

        entoncesLaRespuestaEs("x");
    }

    @Test
    @DisplayName("Un texto que no es JSON se rechaza para que se use el respaldo")
    void unTextoQueNoEsJsonSeRechazaParaQueSeUseElRespaldo() {
        dadoUnModeloQueResponde("esto no es json");

        cuandoSeConversaConError();

        entoncesSeRechazaPorIlegibleConMensaje();
    }

    @Test
    @DisplayName("Una respuesta con la forma equivocada se rechaza para que se use el respaldo")
    void unaRespuestaConLaFormaEquivocadaSeRechazaParaQueSeUseElRespaldo() {
        dadoUnModeloQueResponde("{\"respuesta\":{\"a\":1}}");

        cuandoSeConversaConError();

        entoncesSeRechazaPorIlegible();
    }

    @Test
    @DisplayName("Si el modelo no respondió falla con la excepción del cliente")
    void siElModeloNoRespondioFallaConLaExcepcionDelCliente() {
        dadoUnModeloSinRed();

        cuandoSeConversaConError();

        entoncesFallaConLaExcepcionDelCliente();
    }

    // --- arrange ---
    private void dadoUnModeloQueResponde(String json) {
        ClienteModeloTexto modelo = peticion -> {
            pedidos.add(peticion);
            return new RespuestaModeloTexto(json, "simulado", "modelo", 120, 45);
        };
        adapter = new ConversacionObjetivosIaAdapter(modelo, prompt, JsonMapper.builder().build());
    }

    private void dadoUnModeloSinRed() {
        ClienteModeloTexto roto = peticion -> {
            throw new IllegalArgumentException("sin red");
        };
        adapter = new ConversacionObjetivosIaAdapter(roto, prompt, JsonMapper.builder().build());
    }

    // --- act ---
    private void cuandoSeConversa() {
        propuesta = adapter.conversar(consulta());
    }

    private void cuandoSeConversaConError() {
        error = catchThrowable(this::cuandoSeConversa);
    }

    // --- assert ---
    private void entoncesSeLeeTodaLaRespuesta() {
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

    private void entoncesSeLeeLoQueGastoElModelo() {
        assertThat(propuesta.consumo().proveedor()).isEqualTo("simulado");
        assertThat(propuesta.consumo().modelo()).isEqualTo("modelo");
        assertThat(propuesta.consumo().tokensEntrada()).isEqualTo(120);
        assertThat(propuesta.consumo().tokensSalida()).isEqualTo(45);
    }

    private void entoncesSeLeMandaAlModeloLaConversacionYElCatalogo() {
        assertThat(pedidos).singleElement().satisfies(pedido -> {
            assertThat(pedido.contexto()).contains("[persona] Quiero vender más", "c1: tipo SUBIR_PRECIO");
            assertThat(pedido.esquemaJson()).contains("minis");
        });
    }

    private void entoncesEsMonetario() {
        assertThat(propuesta.esMonetario()).isTrue();
    }

    private void entoncesNoEsMonetario() {
        assertThat(propuesta.esMonetario()).isFalse();
    }

    private void entoncesNoHayElecciones() {
        assertThat(propuesta.elecciones()).isEmpty();
    }

    private void entoncesSoloQuedaLaEleccionDelCodigo(String codigo) {
        assertThat(propuesta.elecciones()).singleElement()
                .satisfies(eleccion -> assertThat(eleccion.codigo()).isEqualTo(codigo));
    }

    private void entoncesLaRespuestaEs(String esperada) {
        assertThat(propuesta.respuesta()).isEqualTo(esperada);
    }

    private void entoncesSeRechazaPorIlegible() {
        assertThat(error).isInstanceOf(IllegalStateException.class);
    }

    private void entoncesSeRechazaPorIlegibleConMensaje() {
        assertThat(error).isInstanceOf(IllegalStateException.class)
                .hasMessage("La IA devolvió una respuesta ilegible");
    }

    private void entoncesFallaConLaExcepcionDelCliente() {
        assertThat(error).isInstanceOf(IllegalArgumentException.class).hasMessage("sin red");
    }
}
