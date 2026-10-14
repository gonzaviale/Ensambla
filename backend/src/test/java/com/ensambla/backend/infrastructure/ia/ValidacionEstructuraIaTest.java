package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidacionEstructuraIaTest {

    private static final String JSON = """
            {"tipoRegistro":"VENTAS","bloque":{"nombre":"Ventas","bloqueExistenteId":null,
             "campos":[{"clave":"monto","nombre":"Monto","tipo":"NUMERO","opciones":["A"]}]},
             "filas":[{"pagina":1,"renglon":1,"datos":{"monto":{"valor":"10","confianza":0.9}}}],
             "advertencias":["Aviso"]}
            """;
    private final ValidacionEstructuraIa validacionEstructuraIa = new ValidacionEstructuraIa();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("Acepta objetos con los tipos JSON esperados")
    void validarShouldAcceptExpectedShapes() {
        assertThatCode(() -> validacionEstructuraIa.validar(jsonMapper.readTree(JSON))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Acepta un campo de conexión")
    void validarShouldAcceptRelacionTipo() {
        String json = JSON.replace("\"tipo\":\"NUMERO\"", "\"tipo\":\"RELACION\"");

        assertThatCode(() -> validacionEstructuraIa.validar(jsonMapper.readTree(json))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Acepta valores nulos sin inventar texto")
    void validarShouldAcceptNullValuesAndStringBlockId() {
        String json = JSON.replace("\"valor\":\"10\"", "\"valor\":null")
                .replace("\"bloqueExistenteId\":null", "\"bloqueExistenteId\":\"id\"");

        assertThatCode(() -> validacionEstructuraIa.validar(jsonMapper.readTree(json))).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "\"tipoRegistro\":\"VENTAS\"|\"tipoRegistro\":1",
            "\"tipoRegistro\":\"VENTAS\"|\"tipoRegistro\":\"ventas\"",
            "\"tipo\":\"NUMERO\"|\"tipo\":\"numero\"",
            "\"bloque\":{|\"bloque\":[",
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
    void validarShouldRejectWrongShapes(String original, String reemplazo) {
        String json = JSON.replace(original, reemplazo);
        if (original.equals("\"bloque\":{")) {
            json = JSON.replace("\"bloque\":", "\"otro\":")
                    .replace("\"tipoRegistro\":", "\"bloque\":null,\"tipoRegistro\":");
        }

        String salida = json;
        assertThatThrownBy(() -> validacionEstructuraIa.validar(jsonMapper.readTree(salida)))
                .isInstanceOf(LecturaCuadernoException.class);
    }

    @Test
    @DisplayName("Rechaza una raíz que no es objeto")
    void validarShouldRejectNonObjectRoot() {
        assertThatThrownBy(() -> validacionEstructuraIa.validar(jsonMapper.readTree("null")))
                .isInstanceOf(LecturaCuadernoException.class);
    }

    @Test
    @DisplayName("Rechaza elementos de campos que no son objetos")
    void validarShouldRejectNonObjectField() {
        String json = JSON.replace("\"campos\":[", "\"campos\":[1,");
        assertThatThrownBy(() -> validacionEstructuraIa.validar(jsonMapper.readTree(json)))
                .isInstanceOf(LecturaCuadernoException.class);
    }

    @Test
    @DisplayName("Rechaza filas que no son objetos")
    void validarShouldRejectNonObjectRow() {
        String json = JSON.replace("\"filas\":[", "\"filas\":[1,");
        assertThatThrownBy(() -> validacionEstructuraIa.validar(jsonMapper.readTree(json)))
                .isInstanceOf(LecturaCuadernoException.class);
    }

    @Test
    @DisplayName("Rechaza datos que no son objetos")
    void validarShouldRejectNonObjectData() {
        String json = JSON.replace("\"monto\":{", "\"monto\":1,\"otro\":{");
        assertThatThrownBy(() -> validacionEstructuraIa.validar(jsonMapper.readTree(json)))
                .isInstanceOf(LecturaCuadernoException.class);
    }
}
