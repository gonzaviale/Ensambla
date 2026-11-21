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
    void devuelveUnTextoPorRenovacionEnSuOrdenSinEspaciosEnLosExtremosYLoQueGastoEl() {
        dadoUnModeloQueResponde("""
                {"minis":[
                  {"codigo":"r2","titulo":" Hablá con 2 vecinas ","justificacion":"Razón dos"},
                  {"codigo":"r1","titulo":"Poné el cartel","justificacion":" Razón uno "}]}""");

        cuandoSeRenueva();

        entoncesHayUnTextoPorRenovacionEnOrdenYSeLeeElGasto();
    }

    @Test
    @DisplayName("Le pide al modelo cada renovación por su código")
    void lePideAlModeloCadaRenovacionPorSuCodigo() {
        dadoUnModeloQueResponde(RESPUESTA_VALIDA);

        cuandoSeRenueva();

        entoncesSeLePideAlModeloCadaRenovacionPorSuCodigo();
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
    void unaRespuestaIlegibleIncompletaRepetidaOConUnTextoVacioSeRechaza(String json) {
        dadoUnModeloQueResponde(json);

        cuandoSeRenuevaConError();

        entoncesSeRechazaLaRespuesta();
    }

    @Test
    @DisplayName("Un título o una justificación más largos de lo permitido se rechazan")
    void unTituloOUnaJustificacionMasLargosDeLoPermitidoSeRechazan() {
        dadoUnModeloQueResponde("""
                {"minis":[{"codigo":"r1","titulo":"%s","justificacion":"R"},
                  {"codigo":"r2","titulo":"Dos","justificacion":"R"}]}""".formatted(TEXTO_LARGO_81));

        cuandoSeRenuevaConError();

        entoncesSeRechazaPorTextoInvalido();
    }

    @Test
    @DisplayName("Si el modelo no respondió falla con la excepción del cliente")
    void siElModeloNoRespondioFallaConLaExcepcionDelCliente() {
        dadoUnModeloSinRed();

        cuandoSeRenuevaConError();

        entoncesFallaConLaExcepcionDelCliente();
    }

    // --- arrange ---
    private void dadoUnModeloQueResponde(String json) {
        ClienteModeloTexto modelo = peticion -> {
            pedidos.add(peticion);
            return new RespuestaModeloTexto(json, "simulado", "modelo", 120, 45);
        };
        adapter = new RenovacionManualesIaAdapter(modelo, prompt, JsonMapper.builder().build());
    }

    private void dadoUnModeloSinRed() {
        ClienteModeloTexto roto = peticion -> {
            throw new IllegalArgumentException("sin red");
        };
        adapter = new RenovacionManualesIaAdapter(roto, prompt, JsonMapper.builder().build());
    }

    // --- act ---
    private void cuandoSeRenueva() {
        redaccion = adapter.renovar(renovaciones);
    }

    private void cuandoSeRenuevaConError() {
        error = catchThrowable(this::cuandoSeRenueva);
    }

    // --- assert ---
    private void entoncesHayUnTextoPorRenovacionEnOrdenYSeLeeElGasto() {
        assertThat(redaccion.textos()).hasSize(2);
        assertThat(redaccion.textos().get(0).titulo()).isEqualTo("Poné el cartel");
        assertThat(redaccion.textos().get(0).justificacion()).isEqualTo("Razón uno");
        assertThat(redaccion.textos().get(1).titulo()).isEqualTo("Hablá con 2 vecinas");
        assertThat(redaccion.consumo().proveedor()).isEqualTo("simulado");
        assertThat(redaccion.consumo().tokensEntrada()).isEqualTo(120);
        assertThat(redaccion.consumo().tokensSalida()).isEqualTo(45);
    }

    private void entoncesSeLePideAlModeloCadaRenovacionPorSuCodigo() {
        assertThat(pedidos).singleElement().satisfies(pedido -> {
            assertThat(pedido.contexto()).contains("r1: qué hacer: SE_DIVIDE", "r2: qué hacer: SE_DIVIDE");
            assertThat(pedido.instrucciones()).contains("Sos Sami");
        });
    }

    private void entoncesSeRechazaLaRespuesta() {
        assertThat(error).isInstanceOf(IllegalStateException.class);
    }

    private void entoncesSeRechazaPorTextoInvalido() {
        assertThat(error).isInstanceOf(IllegalStateException.class)
                .hasMessage("La IA no redactó un texto válido para cada renovación");
    }

    private void entoncesFallaConLaExcepcionDelCliente() {
        assertThat(error).isInstanceOf(IllegalArgumentException.class).hasMessage("sin red");
    }

    // --- helpers ---
    private static PedidoRenovacion pedido(String titulo) {
        return new PedidoRenovacion(AccionRenovacion.SE_DIVIDE, titulo, "Porque sí", HizoManual.EN_PARTE,
                QuePaso.NO_TUVE_TIEMPO, null);
    }
}
