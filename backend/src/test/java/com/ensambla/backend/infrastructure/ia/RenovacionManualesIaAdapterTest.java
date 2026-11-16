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
import static org.assertj.core.api.Assertions.catchThrowable;

class RenovacionManualesIaAdapterTest {

    private static final String TEXTO_LARGO_81 = "x".repeat(RedaccionObjetivosIaAdapter.LARGO_MAXIMO_TITULO + 1);
    private static final String RESPUESTA_VALIDA = """
            {"minis":[
              {"codigo":"r1","titulo":"Uno","justificacion":"Razón uno"},
              {"codigo":"r2","titulo":"Dos","justificacion":"Razón dos"}]}""";

    private final PromptRenovacionManuales prompt = new PromptRenovacionManuales(
            new ClassPathResource("ia/prompt-renovacion-manuales.md"), new ClassPathResource("ia/esquema-objetivos.json"));
    private final List<PeticionModeloTexto> pedidos = new ArrayList<>();
    private final List<PedidoRenovacion> renovaciones = List.of(pedido("Un cartel"), pedido("Hablar con vecinos"));
    private RenovacionManualesIaAdapter adapter;
    private RedaccionObjetivosIa redaccion;
    private Throwable error;

    @Test
    @DisplayName("Devuelve un texto por renovación, en su orden, sin espacios en los extremos, y lo que gastó el modelo")
    void renovarShouldReturnOneTextPerRenewalInOrder() {
        givenUnModeloQueResponde("""
                {"minis":[
                  {"codigo":"r2","titulo":" Hablá con 2 vecinas ","justificacion":"Razón dos"},
                  {"codigo":"r1","titulo":"Poné el cartel","justificacion":" Razón uno "}]}""");

        whenSeRenueva();

        thenHayUnTextoPorRenovacionEnOrdenYSeLeeElGasto();
    }

    @Test
    @DisplayName("Le pide al modelo cada renovación por su código")
    void renovarShouldAskTheModelWithShortCodes() {
        givenUnModeloQueResponde(RESPUESTA_VALIDA);

        whenSeRenueva();

        thenSeLePideAlModeloCadaRenovacionPorSuCodigo();
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
        givenUnModeloQueResponde(json);

        whenSeRenuevaConError();

        thenSeRechazaLaRespuesta();
    }

    @Test
    @DisplayName("Un título o una justificación más largos de lo permitido se rechazan")
    void renovarShouldRejectATextThatIsTooLong() {
        givenUnModeloQueResponde("""
                {"minis":[{"codigo":"r1","titulo":"%s","justificacion":"R"},
                  {"codigo":"r2","titulo":"Dos","justificacion":"R"}]}""".formatted(TEXTO_LARGO_81));

        whenSeRenuevaConError();

        thenSeRechazaPorTextoInvalido();
    }

    @Test
    @DisplayName("Si el modelo no respondió falla con la excepción del cliente")
    void renovarShouldPropagateTheClientFailure() {
        givenUnModeloSinRed();

        whenSeRenuevaConError();

        thenFallaConLaExcepcionDelCliente();
    }

    // --- arrange ---
    private void givenUnModeloQueResponde(String json) {
        ClienteModeloTexto modelo = peticion -> {
            pedidos.add(peticion);
            return new RespuestaModeloTexto(json, "simulado", "modelo", 120, 45);
        };
        adapter = new RenovacionManualesIaAdapter(modelo, prompt, JsonMapper.builder().build());
    }

    private void givenUnModeloSinRed() {
        ClienteModeloTexto roto = peticion -> {
            throw new IllegalArgumentException("sin red");
        };
        adapter = new RenovacionManualesIaAdapter(roto, prompt, JsonMapper.builder().build());
    }

    // --- act ---
    private void whenSeRenueva() {
        redaccion = adapter.renovar(renovaciones);
    }

    private void whenSeRenuevaConError() {
        error = catchThrowable(this::whenSeRenueva);
    }

    // --- assert ---
    private void thenHayUnTextoPorRenovacionEnOrdenYSeLeeElGasto() {
        assertThat(redaccion.textos()).hasSize(2);
        assertThat(redaccion.textos().get(0).titulo()).isEqualTo("Poné el cartel");
        assertThat(redaccion.textos().get(0).justificacion()).isEqualTo("Razón uno");
        assertThat(redaccion.textos().get(1).titulo()).isEqualTo("Hablá con 2 vecinas");
        assertThat(redaccion.consumo().proveedor()).isEqualTo("simulado");
        assertThat(redaccion.consumo().tokensEntrada()).isEqualTo(120);
        assertThat(redaccion.consumo().tokensSalida()).isEqualTo(45);
    }

    private void thenSeLePideAlModeloCadaRenovacionPorSuCodigo() {
        assertThat(pedidos).singleElement().satisfies(pedido -> {
            assertThat(pedido.contexto()).contains("r1: qué hacer: SE_DIVIDE", "r2: qué hacer: SE_DIVIDE");
            assertThat(pedido.instrucciones()).contains("Sos Sami");
        });
    }

    private void thenSeRechazaLaRespuesta() {
        assertThat(error).isInstanceOf(IllegalStateException.class);
    }

    private void thenSeRechazaPorTextoInvalido() {
        assertThat(error).isInstanceOf(IllegalStateException.class)
                .hasMessage("La IA no redactó un texto válido para cada renovación");
    }

    private void thenFallaConLaExcepcionDelCliente() {
        assertThat(error).isInstanceOf(IllegalArgumentException.class).hasMessage("sin red");
    }

    // --- helpers ---
    private static PedidoRenovacion pedido(String titulo) {
        return new PedidoRenovacion(AccionRenovacion.SE_DIVIDE, titulo, "Porque sí", HizoManual.EN_PARTE,
                QuePaso.NO_TUVE_TIEMPO, null);
    }
}
