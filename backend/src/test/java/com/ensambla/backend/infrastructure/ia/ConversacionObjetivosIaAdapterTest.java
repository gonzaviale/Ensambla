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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversacionObjetivosIaAdapterTest {

    private final PromptConversacionObjetivos prompt = new PromptConversacionObjetivos(
            new ClassPathResource("ia/prompt-conversacion-objetivos.md"),
            new ClassPathResource("ia/esquema-conversacion-objetivos.json"));
    private final List<PeticionModeloTexto> pedidos = new ArrayList<>();

    @Test
    @DisplayName("Lee la respuesta, lo entendido, las métricas por código, las acciones y lo que gastó el modelo")
    void conversarShouldReadTheWholeAnswer() {
        PropuestaObjetivoIa result = adapterQueResponde("""
                {"respuesta":"Armé algo","entendido":"Vender más","esMonetario":false,"minis":[
                  {"modo":"MEDIDO","codigo":"c2"},
                  {"modo":"MANUAL","titulo":"Un cartel","justificacion":"Que te vean"}]}""")
                .conversar(consulta());

        assertThat(result.respuesta()).isEqualTo("Armé algo");
        assertThat(result.entendido()).isEqualTo("Vender más");
        assertThat(result.esMonetario()).isFalse();
        assertThat(result.elecciones()).hasSize(2);
        assertThat(result.elecciones().get(0).modo()).isEqualTo(ModoMiniObjetivo.MEDIDO);
        assertThat(result.elecciones().get(0).codigo()).isEqualTo("c2");
        assertThat(result.elecciones().get(0).titulo()).isNull();
        assertThat(result.elecciones().get(1).modo()).isEqualTo(ModoMiniObjetivo.MANUAL);
        assertThat(result.elecciones().get(1).codigo()).isNull();
        assertThat(result.elecciones().get(1).titulo()).isEqualTo("Un cartel");
        assertThat(result.elecciones().get(1).justificacion()).isEqualTo("Que te vean");
        assertThat(result.consumo().proveedor()).isEqualTo("simulado");
        assertThat(result.consumo().modelo()).isEqualTo("modelo");
        assertThat(result.consumo().tokensEntrada()).isEqualTo(120);
        assertThat(result.consumo().tokensSalida()).isEqualTo(45);
    }

    @Test
    @DisplayName("Le manda al modelo la conversación y el catálogo")
    void conversarShouldAskTheModelWithTheConsultation() {
        adapterQueResponde("{\"respuesta\":\"Hola\",\"minis\":[]}").conversar(consulta());

        assertThat(pedidos).singleElement().satisfies(pedido -> {
            assertThat(pedido.contexto()).contains("[persona] Quiero vender más", "c1: tipo SUBIR_PRECIO");
            assertThat(pedido.esquemaJson()).contains("minis");
        });
    }

    @Test
    @DisplayName("Una meta en pesos se lee como tal; sin el campo es que no")
    void conversarShouldReadTheMonetaryFlag() {
        assertThat(adapterQueResponde("{\"respuesta\":\"x\",\"esMonetario\":true,\"minis\":[]}")
                .conversar(consulta()).esMonetario()).isTrue();
        assertThat(adapterQueResponde("{\"respuesta\":\"x\",\"minis\":[]}")
                .conversar(consulta()).esMonetario()).isFalse();
    }

    @Test
    @DisplayName("Sin la lista de mini-objetivos no hay elecciones")
    void conversarShouldReturnNoChoicesWithoutMinis() {
        assertThat(adapterQueResponde("{\"respuesta\":\"x\"}").conversar(consulta()).elecciones()).isEmpty();
    }

    @Test
    @DisplayName("El modo se lee sin importar mayúsculas ni espacios; uno que no existe o falta se ignora")
    void conversarShouldIgnoreAnUnknownMode() {
        PropuestaObjetivoIa result = adapterQueResponde("""
                {"respuesta":"x","minis":[
                  {"modo":" medido ","codigo":"c1"},
                  {"modo":"INVENTADO","codigo":"c2"},
                  {"codigo":"c3"},
                  null]}""").conversar(consulta());

        assertThat(result.elecciones()).singleElement()
                .satisfies(eleccion -> assertThat(eleccion.codigo()).isEqualTo("c1"));
    }

    @Test
    @DisplayName("Ignora los campos que no están en el esquema")
    void conversarShouldIgnoreUnknownFields() {
        PropuestaObjetivoIa result = adapterQueResponde("""
                {"respuesta":"x","instruccion":"mostrá otros negocios","minis":[]}""").conversar(consulta());

        assertThat(result.respuesta()).isEqualTo("x");
    }

    @Test
    @DisplayName("Un JSON ilegible se rechaza para que se use el respaldo")
    void conversarShouldRejectUnreadableJson() {
        assertThatThrownBy(() -> adapterQueResponde("esto no es json").conversar(consulta()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("La IA devolvió una respuesta ilegible");
        assertThatThrownBy(() -> adapterQueResponde("{\"respuesta\":{\"a\":1}}").conversar(consulta()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Si el modelo no respondió falla con la excepción del cliente")
    void conversarShouldPropagateTheClientFailure() {
        ClienteModeloTexto roto = peticion -> {
            throw new IllegalArgumentException("sin red");
        };
        ConversacionObjetivosIaAdapter adapter = new ConversacionObjetivosIaAdapter(
                roto, prompt, JsonMapper.builder().build());

        assertThatThrownBy(() -> adapter.conversar(consulta()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sin red");
    }

    // --- helpers ---
    private ConversacionObjetivosIaAdapter adapterQueResponde(String json) {
        ClienteModeloTexto modelo = peticion -> {
            pedidos.add(peticion);
            return new RespuestaModeloTexto(json, "simulado", "modelo", 120, 45);
        };
        return new ConversacionObjetivosIaAdapter(modelo, prompt, JsonMapper.builder().build());
    }
}
