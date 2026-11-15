package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.objetivo.AccionRenovacion;
import com.ensambla.backend.domain.model.objetivo.HizoManual;
import com.ensambla.backend.domain.model.objetivo.PedidoRenovacion;
import com.ensambla.backend.domain.model.objetivo.QuePaso;
import com.ensambla.backend.domain.model.objetivo.RedaccionObjetivosIa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RenovacionManualesIaAdapterTest {

    private static final String TEXTO_LARGO_81 = "x".repeat(RedaccionObjetivosIaAdapter.LARGO_MAXIMO_TITULO + 1);

    private final PromptRenovacionManuales prompt = new PromptRenovacionManuales(
            new ClassPathResource("ia/prompt-renovacion-manuales.md"), new ClassPathResource("ia/esquema-objetivos.json"));
    private final List<PeticionModeloTexto> pedidos = new ArrayList<>();
    private final List<PedidoRenovacion> renovaciones = List.of(pedido("Un cartel"), pedido("Hablar con vecinos"));

    @Test
    @DisplayName("Devuelve un texto por renovación, en su orden, sin espacios en los extremos, y lo que gastó el modelo")
    void renovarShouldReturnOneTextPerRenewalInOrder() {
        RedaccionObjetivosIa result = adapterQueResponde("""
                {"minis":[
                  {"codigo":"r2","titulo":" Hablá con 2 vecinas ","justificacion":"Razón dos"},
                  {"codigo":"r1","titulo":"Poné el cartel","justificacion":" Razón uno "}]}""")
                .renovar(renovaciones);

        assertThat(result.textos()).hasSize(2);
        assertThat(result.textos().get(0).titulo()).isEqualTo("Poné el cartel");
        assertThat(result.textos().get(0).justificacion()).isEqualTo("Razón uno");
        assertThat(result.textos().get(1).titulo()).isEqualTo("Hablá con 2 vecinas");
        assertThat(result.consumo().proveedor()).isEqualTo("simulado");
        assertThat(result.consumo().tokensEntrada()).isEqualTo(120);
        assertThat(result.consumo().tokensSalida()).isEqualTo(45);
    }

    @Test
    @DisplayName("Le pide al modelo cada renovación por su código")
    void renovarShouldAskTheModelWithShortCodes() {
        adapterQueResponde(respuestaValida()).renovar(renovaciones);

        assertThat(pedidos).singleElement().satisfies(pedido -> {
            assertThat(pedido.contexto()).contains("r1: qué hacer: SE_DIVIDE", "r2: qué hacer: SE_DIVIDE");
            assertThat(pedido.instrucciones()).contains("Sos Sami");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "esto no es json",
            "{\"minis\":[{\"codigo\":\"r1\",\"titulo\":\"Uno\",\"justificacion\":\"Razón\"}]}",
            "{\"minis\":[{\"codigo\":\"r1\",\"titulo\":\"Uno\",\"justificacion\":\"R\"},"
                    + "{\"codigo\":\"r1\",\"titulo\":\"Dos\",\"justificacion\":\"R\"}]}",
            "{\"minis\":[{\"codigo\":\"r1\",\"titulo\":\"\",\"justificacion\":\"R\"},"
                    + "{\"codigo\":\"r2\",\"titulo\":\"Dos\",\"justificacion\":\"R\"}]}",
            "{\"minis\":[{\"codigo\":\"r1\",\"titulo\":\"Uno\",\"justificacion\":null},"
                    + "{\"codigo\":\"r2\",\"titulo\":\"Dos\",\"justificacion\":\"R\"}]}"})
    @DisplayName("Una respuesta ilegible, incompleta, repetida o con un texto vacío se rechaza")
    void renovarShouldRejectAnUnusableAnswer(String json) {
        assertThatThrownBy(() -> adapterQueResponde(json).renovar(renovaciones))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Un título o una justificación más largos de lo permitido se rechazan")
    void renovarShouldRejectATextThatIsTooLong() {
        String largo = """
                {"minis":[{"codigo":"r1","titulo":"%s","justificacion":"R"},
                  {"codigo":"r2","titulo":"Dos","justificacion":"R"}]}""".formatted(TEXTO_LARGO_81);

        assertThatThrownBy(() -> adapterQueResponde(largo).renovar(renovaciones))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("La IA no redactó un texto válido para cada renovación");
    }

    @Test
    @DisplayName("Si el modelo no respondió falla con la excepción del cliente")
    void renovarShouldPropagateTheClientFailure() {
        ClienteModeloTexto roto = peticion -> {
            throw new IllegalArgumentException("sin red");
        };
        RenovacionManualesIaAdapter adapter = new RenovacionManualesIaAdapter(
                roto, prompt, JsonMapper.builder().build());

        assertThatThrownBy(() -> adapter.renovar(renovaciones))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sin red");
    }

    // --- helpers ---
    private RenovacionManualesIaAdapter adapterQueResponde(String json) {
        ClienteModeloTexto modelo = peticion -> {
            pedidos.add(peticion);
            return new RespuestaModeloTexto(json, "simulado", "modelo", 120, 45);
        };
        return new RenovacionManualesIaAdapter(modelo, prompt, JsonMapper.builder().build());
    }

    private static String respuestaValida() {
        return """
                {"minis":[
                  {"codigo":"r1","titulo":"Uno","justificacion":"Razón uno"},
                  {"codigo":"r2","titulo":"Dos","justificacion":"Razón dos"}]}""";
    }

    private static PedidoRenovacion pedido(String titulo) {
        return new PedidoRenovacion(AccionRenovacion.SE_DIVIDE, titulo, "Porque sí", HizoManual.EN_PARTE,
                QuePaso.NO_TUVE_TIEMPO, null);
    }
}
