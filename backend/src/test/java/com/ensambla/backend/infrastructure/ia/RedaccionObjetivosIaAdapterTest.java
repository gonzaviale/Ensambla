package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.objetivo.CandidatoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.RedaccionObjetivosIa;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidatoDelNegocio;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class RedaccionObjetivosIaAdapterTest {

    private static final String TEXTO_LARGO_81 = "x".repeat(RedaccionObjetivosIaAdapter.LARGO_MAXIMO_TITULO + 1);

    private final PromptObjetivos prompt = new PromptObjetivos(
            new ClassPathResource("ia/prompt-objetivos.md"), new ClassPathResource("ia/esquema-objetivos.json"));
    private final List<PeticionModeloTexto> pedidos = new ArrayList<>();

    private List<CandidatoMiniObjetivo> candidatos = List.of(
            candidato(), candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "6180"));
    private String respuestaDelModelo;
    private RedaccionObjetivosIa redaccion;
    private Throwable rechazo;

    @Test
    @DisplayName("Devuelve un texto por candidato, en su orden, sin espacios en los extremos, y lo que gastó el modelo")
    void devuelveUnTextoPorCandidatoEnSuOrdenSinEspaciosEnLosExtremosYLoQueGastoElModelo() {
        dadoQueElModeloResponde("""
                {"minis":[
                  {"codigo":"m2","titulo":" Dos puntos más ","justificacion":"Razón del margen"},
                  {"codigo":"m1","titulo":"Subile $250","justificacion":" Razón de la yerba "}]}""");

        cuandoSeRedacta();

        entoncesHayUnTextoPorCandidatoEnSuOrdenConElConsumo();
    }

    @Test
    @DisplayName("Le pide el texto de cada candidato por su código y con los números ya calculados")
    void lePideElTextoDeCadaCandidatoPorSuCodigoYConLosNumerosYaCalculados() {
        dadoQueElModeloResponde(respuestaValida());

        cuandoSeRedacta();

        entoncesElPedidoUsaCodigosCortosYNoPideCalcular();
    }

    @Test
    @DisplayName("Un texto con el largo máximo permitido sirve")
    void unTextoConElLargoMaximoPermitidoSirve() {
        dadoUnCandidatoYUnTextoConElLargoMaximoPermitido();

        cuandoSeRedacta();

        entoncesSeAceptanLosTextosConElLargoMaximo();
    }

    @Test
    @DisplayName("Un título más largo que el permitido no sirve")
    void unTituloMasLargoQueElPermitidoNoSirve() {
        dadoUnCandidatoYUnaRespuesta("""
                {"minis":[{"codigo":"m1","titulo":"%s","justificacion":"Razón"}]}""".formatted(TEXTO_LARGO_81));

        cuandoSeIntentaRedactar();

        entoncesSeRechazaConElMensaje("La IA no redactó un texto válido para cada mini-objetivo");
    }

    @Test
    @DisplayName("Una justificación más larga que la permitida no sirve")
    void unaJustificacionMasLargaQueLaPermitidaNoSirve() {
        String larga = "y".repeat(RedaccionObjetivosIaAdapter.LARGO_MAXIMO_JUSTIFICACION + 1);
        dadoUnCandidatoYUnaRespuesta("""
                {"minis":[{"codigo":"m1","titulo":"Título","justificacion":"%s"}]}""".formatted(larga));

        cuandoSeIntentaRedactar();

        entoncesSeRechazaPorUnTextoInvalido();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"minis\":[{\"codigo\":\"m1\",\"titulo\":\"  \",\"justificacion\":\"Razón\"}]}",
            "{\"minis\":[{\"codigo\":\"m1\",\"titulo\":\"Título\",\"justificacion\":\"\"}]}",
            "{\"minis\":[{\"codigo\":\"m1\",\"justificacion\":\"Razón\"}]}",
            "{\"minis\":[{\"codigo\":\"m1\",\"titulo\":\"Título\"}]}",
            "{\"minis\":[{\"titulo\":\"Sin código\",\"justificacion\":\"Razón\"}]}"})
    @DisplayName("Un texto vacío, que falta o sin código con el que ubicarlo no sirve")
    void unTextoVacioQueFaltaOSinCodigoConElQueUbicarloNoSirve(String json) {
        dadoUnCandidatoYUnaRespuesta(json);

        cuandoSeIntentaRedactar();

        entoncesSeRechazaPorUnTextoInvalido();
    }

    @Test
    @DisplayName("Si falta el texto de algún candidato no sirve la respuesta entera")
    void siFaltaElTextoDeAlgunCandidatoNoSirveLaRespuestaEntera() {
        dadoTresCandidatosYUnaRespuestaDeDos();

        cuandoSeIntentaRedactar();

        entoncesSeRechazaPorUnTextoInvalido();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "esto no es JSON",
            "{}",
            "{\"minis\":null}",
            "{\"minis\":[{\"codigo\":\"m1\",\"titulo\":\"A\",\"justificacion\":\"B\"},"
                    + "{\"codigo\":\"m1\",\"titulo\":\"C\",\"justificacion\":\"D\"}]}"})
    @DisplayName("Una respuesta ilegible, sin lista de textos o con un código repetido no sirve")
    void unaRespuestaIlegibleSinListaDeTextosOConUnCodigoRepetidoNoSirve(String json) {
        dadoUnCandidatoYUnaRespuesta(json);

        cuandoSeIntentaRedactar();

        entoncesSeRechazaConElMensaje("La IA devolvió una redacción ilegible");
    }

    @Test
    @DisplayName("Si el modelo no responde deja pasar su error")
    void siElModeloNoRespondeDejaPasarSuError() {
        cuandoSeIntentaRedactarConUnModeloSinConexion();

        entoncesSeDejaPasarElErrorDelModelo();
    }

    // --- dado ---

    private void dadoQueElModeloResponde(String json) {
        respuestaDelModelo = json;
    }

    private void dadoUnCandidatoYUnaRespuesta(String json) {
        candidatos = List.of(candidato());
        respuestaDelModelo = json;
    }

    private void dadoUnCandidatoYUnTextoConElLargoMaximoPermitido() {
        String titulo = "x".repeat(RedaccionObjetivosIaAdapter.LARGO_MAXIMO_TITULO);
        String justificacion = "y".repeat(RedaccionObjetivosIaAdapter.LARGO_MAXIMO_JUSTIFICACION);
        dadoUnCandidatoYUnaRespuesta("""
                {"minis":[{"codigo":"m1","titulo":"%s","justificacion":"%s"}]}""".formatted(titulo, justificacion));
    }

    private void dadoTresCandidatosYUnaRespuestaDeDos() {
        candidatos = List.of(candidato(), candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "6180"),
                candidatoDelNegocio(TipoMiniObjetivo.VENTAS_SEMANALES, "5000"));
        respuestaDelModelo = respuestaValida();
    }

    // --- cuando ---

    private void cuandoSeRedacta() {
        redaccion = adapterQueResponde().redactar(candidatos);
    }

    private void cuandoSeIntentaRedactar() {
        rechazo = catchThrowable(this::cuandoSeRedacta);
    }

    private void cuandoSeIntentaRedactarConUnModeloSinConexion() {
        RedaccionObjetivosIaAdapter adapter = new RedaccionObjetivosIaAdapter(
                peticion -> {
                    throw new IllegalArgumentException("sin conexión");
                }, prompt, JsonMapper.builder().build());
        rechazo = catchThrowable(() -> adapter.redactar(List.of(candidato())));
    }

    // --- entonces ---

    private void entoncesHayUnTextoPorCandidatoEnSuOrdenConElConsumo() {
        assertThat(redaccion.textos()).hasSize(2);
        assertThat(redaccion.textos().get(0).titulo()).isEqualTo("Subile $250");
        assertThat(redaccion.textos().get(0).justificacion()).isEqualTo("Razón de la yerba");
        assertThat(redaccion.textos().get(1).titulo()).isEqualTo("Dos puntos más");
        assertThat(redaccion.consumo().proveedor()).isEqualTo("simulado");
        assertThat(redaccion.consumo().modelo()).isEqualTo("modelo");
        assertThat(redaccion.consumo().tokensEntrada()).isEqualTo(120);
        assertThat(redaccion.consumo().tokensSalida()).isEqualTo(45);
    }

    private void entoncesElPedidoUsaCodigosCortosYNoPideCalcular() {
        assertThat(pedidos).singleElement().satisfies(pedido -> {
            assertThat(pedido.contexto())
                    .contains("m1: tipo SUBIR_PRECIO, sobre Yerba 1 kg", "m2: tipo MEJORAR_MARGEN");
            assertThat(pedido.esquemaJson()).contains("minis");
            assertThat(pedido.instrucciones()).contains("NO calcules");
        });
    }

    private void entoncesSeAceptanLosTextosConElLargoMaximo() {
        assertThat(redaccion.textos()).singleElement().satisfies(texto -> {
            assertThat(texto.titulo()).hasSize(RedaccionObjetivosIaAdapter.LARGO_MAXIMO_TITULO);
            assertThat(texto.justificacion()).hasSize(RedaccionObjetivosIaAdapter.LARGO_MAXIMO_JUSTIFICACION);
        });
    }

    private void entoncesSeRechazaConElMensaje(String mensaje) {
        assertThat(rechazo).isInstanceOf(IllegalStateException.class).hasMessage(mensaje);
    }

    private void entoncesSeRechazaPorUnTextoInvalido() {
        assertThat(rechazo).isInstanceOf(IllegalStateException.class);
    }

    private void entoncesSeDejaPasarElErrorDelModelo() {
        assertThat(rechazo).isInstanceOf(IllegalArgumentException.class).hasMessage("sin conexión");
    }

    // --- datos ---

    private RedaccionObjetivosIaAdapter adapterQueResponde() {
        return new RedaccionObjetivosIaAdapter(peticion -> {
            pedidos.add(peticion);
            return new RespuestaModeloTexto(respuestaDelModelo, "simulado", "modelo", 120, 45);
        }, prompt, JsonMapper.builder().build());
    }

    private static String respuestaValida() {
        return """
                {"minis":[
                  {"codigo":"m1","titulo":"Título 1","justificacion":"Razón 1"},
                  {"codigo":"m2","titulo":"Título 2","justificacion":"Razón 2"}]}""";
    }
}
