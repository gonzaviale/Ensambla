package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ValidacionEstructuraIaTest {

    private static final String JSON = """
            {"tipoRegistro":"VENTAS","bloque":{"nombre":"Ventas","bloqueExistenteId":null,
             "campos":[{"clave":"monto","nombre":"Monto","tipo":"NUMERO","opciones":["A"]}]},
             "filas":[{"pagina":1,"renglon":1,"datos":{"monto":{"valor":"10","confianza":0.9}}}],
             "advertencias":["Aviso"]}
            """;

    private final ValidacionEstructuraIa validacionEstructuraIa = new ValidacionEstructuraIa();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private String json = JSON;
    private Throwable rechazo;

    @Test
    @DisplayName("Acepta objetos con los tipos JSON esperados")
    void aceptaObjetosConLosTiposJsonEsperados() {
        cuandoSeValida();

        entoncesSeAcepta();
    }

    @Test
    @DisplayName("Acepta un campo de conexión")
    void aceptaUnCampoDeConexion() {
        dadoUnJsonCon("\"tipo\":\"NUMERO\"", "\"tipo\":\"RELACION\"");

        cuandoSeValida();

        entoncesSeAcepta();
    }

    @Test
    @DisplayName("Acepta valores nulos sin inventar texto")
    void aceptaValoresNulosSinInventarTexto() {
        dadoUnJsonConValorNuloYBloqueExistenteDeTexto();

        cuandoSeValida();

        entoncesSeAcepta();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "\"tipoRegistro\":\"VENTAS\"|\"tipoRegistro\":1",
            "\"tipoRegistro\":\"VENTAS\"|\"tipoRegistro\":\"ventas\"",
            "\"tipo\":\"NUMERO\"|\"tipo\":\"numero\"",
            "\"bloqueExistenteId\":null|\"bloqueExistenteId\":1",
            "\"campos\":[|\"campos\":null, \"otro\":[",
            "\"opciones\":[\"A\"]|\"opciones\":null",
            "\"advertencias\":[\"Aviso\"]|\"advertencias\":[1]",
            "\"filas\":[|\"filas\":null, \"otro\":[",
            "\"pagina\":1|\"pagina\":1.5",
            "\"renglon\":1|\"renglon\":\"1\"",
            "\"datos\":{|\"datos\":null, \"otro\":{",
            "\"valor\":\"10\"|\"valor\":10",
            "\"confianza\":0.9|\"confianza\":\"0.9\""
    })
    @DisplayName("Rechaza conversiones implícitas y formas JSON incompatibles")
    void rechazaConversionesImplicitasYFormasJsonIncompatibles(String original, String reemplazo) {
        dadoUnJsonCon(original, reemplazo);

        cuandoSeIntentaValidar();

        entoncesSeRechaza();
    }

    @Test
    @DisplayName("Rechaza un bloque nulo")
    void rechazaUnBloqueNulo() {
        dadoUnJsonConElBloqueNulo();

        cuandoSeIntentaValidar();

        entoncesSeRechaza();
    }

    @Test
    @DisplayName("Rechaza una raíz que no es objeto")
    void rechazaUnaRaizQueNoEsObjeto() {
        dadoUnJsonDeRaiz("null");

        cuandoSeIntentaValidar();

        entoncesSeRechaza();
    }

    @Test
    @DisplayName("Rechaza elementos de campos que no son objetos")
    void rechazaElementosDeCamposQueNoSonObjetos() {
        dadoUnJsonCon("\"campos\":[", "\"campos\":[1,");

        cuandoSeIntentaValidar();

        entoncesSeRechaza();
    }

    @Test
    @DisplayName("Rechaza filas que no son objetos")
    void rechazaFilasQueNoSonObjetos() {
        dadoUnJsonCon("\"filas\":[", "\"filas\":[1,");

        cuandoSeIntentaValidar();

        entoncesSeRechaza();
    }

    @Test
    @DisplayName("Rechaza datos que no son objetos")
    void rechazaDatosQueNoSonObjetos() {
        dadoUnJsonCon("\"monto\":{", "\"monto\":1,\"otro\":{");

        cuandoSeIntentaValidar();

        entoncesSeRechaza();
    }

    // --- dado ---

    private void dadoUnJsonCon(String original, String reemplazo) {
        json = JSON.replace(original, reemplazo);
    }

    private void dadoUnJsonConValorNuloYBloqueExistenteDeTexto() {
        json = JSON.replace("\"valor\":\"10\"", "\"valor\":null")
                .replace("\"bloqueExistenteId\":null", "\"bloqueExistenteId\":\"id\"");
    }

    private void dadoUnJsonConElBloqueNulo() {
        json = JSON.replace("\"bloque\":", "\"otro\":")
                .replace("\"tipoRegistro\":", "\"bloque\":null,\"tipoRegistro\":");
    }

    private void dadoUnJsonDeRaiz(String raiz) {
        json = raiz;
    }

    // --- cuando ---

    private void cuandoSeValida() {
        validacionEstructuraIa.validar(jsonMapper.readTree(json));
    }

    private void cuandoSeIntentaValidar() {
        rechazo = catchThrowable(this::cuandoSeValida);
    }

    // --- entonces ---

    private void entoncesSeAcepta() {
        assertThat(rechazo).isNull();
    }

    private void entoncesSeRechaza() {
        assertThat(rechazo).isInstanceOf(LecturaCuadernoException.class);
    }
}
