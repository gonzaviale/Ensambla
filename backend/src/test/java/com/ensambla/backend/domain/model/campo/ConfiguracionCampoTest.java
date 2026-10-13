package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.model.enums.TipoCampo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class ConfiguracionCampoTest {

    private static final List<String> TALLES = List.of("Chico", "Grande");
    private static final UUID RELACIONADO_ID = UUID.fromString("b0000000-0000-4000-8000-000000000002");

    @Test
    @DisplayName("Normaliza un campo de texto: nombre sin espacios, no obligatorio por defecto y sin valor inicial")
    void validarShouldNormalizeTextoWithDefaults() {
        ConfiguracionCampo.Validada result =
                validar(new ConfiguracionCampo("  Producto  ", "TEXTO", null, "  ", null, null, null));

        assertThat(result.nombre()).isEqualTo("Producto");
        assertThat(result.tipo()).isEqualTo(TipoCampo.TEXTO);
        assertThat(result.obligatorio()).isFalse();
        assertThat(result.valorInicial()).isNull();
        assertThat(result.bloqueRelacionadoId()).isNull();
        assertThat(result.opciones()).isEmpty();
    }

    @Test
    @DisplayName("Conserva la marca de obligatorio")
    void validarShouldKeepObligatorio() {
        ConfiguracionCampo.Validada result =
                validar(new ConfiguracionCampo("Producto", "TEXTO", true, null, null, null, null));

        assertThat(result.obligatorio()).isTrue();
    }

    @Test
    @DisplayName("Ignora las opciones de un campo que no es de opción")
    void validarShouldDropOpcionesWhenTipoIsNotOpcion() {
        ConfiguracionCampo.Validada result =
                validar(new ConfiguracionCampo("Precio", "NUMERO", false, null, TALLES, null, null));

        assertThat(result.opciones()).isEmpty();
    }

    @Test
    @DisplayName("Limpia los espacios de las opciones y del valor inicial de un campo de opción")
    void validarShouldStripOpcionesAndValorInicial() {
        ConfiguracionCampo.Validada result = validar(
                new ConfiguracionCampo("Talle", "OPCION", false, " Chico ", List.of(" Chico ", "Grande "), null, null));

        assertThat(result.opciones()).containsExactly("Chico", "Grande");
        assertThat(result.valorInicial()).isEqualTo("Chico");
    }

    @Test
    @DisplayName("Acepta un nombre de 120 caracteres")
    void validarShouldAcceptNombreAtTheLimit() {
        ConfiguracionCampo.Validada result =
                validar(new ConfiguracionCampo("a".repeat(120), "TEXTO", null, null, null, null, null));

        assertThat(result.nombre()).hasSize(120);
    }

    @Test
    @DisplayName("Rechaza un nombre vacío")
    void validarShouldRejectBlankNombre() {
        Map<String, String> errores = erroresDe(new ConfiguracionCampo("   ", "TEXTO", null, null, null, null, null));

        assertThat(errores).containsExactly(Map.entry("nombre", "Escribí un nombre"));
    }

    @Test
    @DisplayName("Rechaza un campo sin nombre")
    void validarShouldRejectNullNombre() {
        Map<String, String> errores = erroresDe(new ConfiguracionCampo(null, "TEXTO", null, null, null, null, null));

        assertThat(errores).containsOnlyKeys("nombre");
    }

    @Test
    @DisplayName("Rechaza un nombre de más de 120 caracteres")
    void validarShouldRejectTooLongNombre() {
        Map<String, String> errores = erroresDe(
                new ConfiguracionCampo("a".repeat(121), "TEXTO", null, null, null, null, null));

        assertThat(errores).containsExactly(Map.entry("nombre", "Máximo 120 caracteres"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MONEDA", "texto", "", "SI NO"})
    @DisplayName("Rechaza un tipo fuera de la lista")
    void validarShouldRejectUnknownTipo(String tipo) {
        Map<String, String> errores = erroresDe(new ConfiguracionCampo("Dato", tipo, null, null, null, null, null));

        assertThat(errores).containsExactly(
                Map.entry("tipo", "Elegí un tipo válido: texto, número, fecha, opción, sí/no, conexión o fórmula"));
    }

    @Test
    @DisplayName("Rechaza un campo sin tipo")
    void validarShouldRejectNullTipo() {
        Map<String, String> errores = erroresDe(new ConfiguracionCampo("Dato", null, null, null, null, null, null));

        assertThat(errores).containsOnlyKeys("tipo");
    }

    @Test
    @DisplayName("Informa todos los errores juntos: nombre y tipo")
    void validarShouldReportEveryError() {
        Map<String, String> errores = erroresDe(new ConfiguracionCampo("", "MONEDA", null, "x", null, null, null));

        assertThat(errores).containsOnlyKeys("nombre", "tipo");
    }

    @Test
    @DisplayName("Un campo de opción sin opciones es inválido")
    void validarShouldRejectOpcionWithoutOpciones() {
        Map<String, String> errores = erroresDe(new ConfiguracionCampo("Talle", "OPCION", null, null, null, null, null));

        assertThat(errores).containsExactly(Map.entry("opciones", "Agregá al menos una opción"));
    }

    @Test
    @DisplayName("Un campo de opción con la lista vacía es inválido")
    void validarShouldRejectOpcionWithEmptyList() {
        Map<String, String> errores = erroresDe(new ConfiguracionCampo("Talle", "OPCION", null, null, List.of(), null, null));

        assertThat(errores).containsExactly(Map.entry("opciones", "Agregá al menos una opción"));
    }

    @Test
    @DisplayName("Rechaza una opción en blanco")
    void validarShouldRejectBlankOpcion() {
        Map<String, String> errores = erroresDe(
                new ConfiguracionCampo("Talle", "OPCION", null, null, List.of("Chico", "  "), null, null));

        assertThat(errores).containsExactly(Map.entry("opciones", "Las opciones no pueden quedar vacías"));
    }

    @Test
    @DisplayName("Rechaza una opción nula")
    void validarShouldRejectNullOpcion() {
        Map<String, String> errores = erroresDe(
                new ConfiguracionCampo("Talle", "OPCION", null, null, Arrays.asList("Chico", null), null, null));

        assertThat(errores).containsExactly(Map.entry("opciones", "Las opciones no pueden quedar vacías"));
    }

    @Test
    @DisplayName("Rechaza opciones repetidas sin distinguir mayúsculas ni espacios")
    void validarShouldRejectRepeatedOpciones() {
        Map<String, String> errores = erroresDe(
                new ConfiguracionCampo("Talle", "OPCION", null, null, List.of("Chico", " CHICO"), null, null));

        assertThat(errores).containsExactly(Map.entry("opciones", "Hay opciones repetidas"));
    }

    @Test
    @DisplayName("Rechaza un valor inicial que no está entre las opciones")
    void validarShouldRejectValorInicialOutsideOpciones() {
        Map<String, String> errores = erroresDe(
                new ConfiguracionCampo("Talle", "OPCION", null, "Mediano", TALLES, null, null));

        assertThat(errores).containsExactly(Map.entry("valorInicial", "Tiene que ser una de las opciones"));
    }

    @Test
    @DisplayName("Acepta un valor inicial de texto de hasta 1000 caracteres")
    void validarShouldAcceptTextoAtTheLimit() {
        ConfiguracionCampo.Validada result = validar(
                new ConfiguracionCampo("Notas", "TEXTO", null, "x".repeat(1000), null, null, null));

        assertThat(result.valorInicial()).hasSize(1000);
    }

    @Test
    @DisplayName("Rechaza un valor inicial de texto de más de 1000 caracteres")
    void validarShouldRejectTooLongTexto() {
        Map<String, String> errores = erroresDe(
                new ConfiguracionCampo("Notas", "TEXTO", null, "x".repeat(1001), null, null, null));

        assertThat(errores).containsExactly(Map.entry("valorInicial", "Máximo 1000 caracteres"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12", "12.5", "-3", "0.25"})
    @DisplayName("Acepta números enteros y decimales con punto como valor inicial")
    void validarShouldAcceptNumero(String valor) {
        ConfiguracionCampo.Validada result =
                validar(new ConfiguracionCampo("Precio", "NUMERO", null, valor, null, null, null));

        assertThat(result.valorInicial()).isEqualTo(valor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12,5", "doce", "1e5", ".5", "12.", "+3"})
    @DisplayName("Rechaza un valor inicial que no es un número con punto decimal")
    void validarShouldRejectInvalidNumero(String valor) {
        Map<String, String> errores = erroresDe(new ConfiguracionCampo("Precio", "NUMERO", null, valor, null, null, null));

        assertThat(errores).containsExactly(
                Map.entry("valorInicial", "Tiene que ser un número, por ejemplo 12.5"));
    }

    @Test
    @DisplayName("Acepta una fecha AAAA-MM-DD como valor inicial")
    void validarShouldAcceptFecha() {
        ConfiguracionCampo.Validada result = validar(
                new ConfiguracionCampo("Entrega", "FECHA", null, "2026-10-09", null, null, null));

        assertThat(result.valorInicial()).isEqualTo("2026-10-09");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-02-31", "09/10/2026", "2026-1-9", "+2026-10-09"})
    @DisplayName("Rechaza una fecha inexistente o con otro formato como valor inicial")
    void validarShouldRejectInvalidFecha(String valor) {
        Map<String, String> errores = erroresDe(new ConfiguracionCampo("Entrega", "FECHA", null, valor, null, null, null));

        assertThat(errores).containsExactly(
                Map.entry("valorInicial", "Tiene que ser una fecha con el formato AAAA-MM-DD"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    @DisplayName("Acepta true y false como valor inicial de sí/no")
    void validarShouldAcceptSiNo(String valor) {
        ConfiguracionCampo.Validada result =
                validar(new ConfiguracionCampo("Pagado", "SI_NO", null, valor, null, null, null));

        assertThat(result.valorInicial()).isEqualTo(valor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"sí", "TRUE", "1"})
    @DisplayName("Rechaza otro valor inicial para sí/no")
    void validarShouldRejectInvalidSiNo(String valor) {
        Map<String, String> errores = erroresDe(new ConfiguracionCampo("Pagado", "SI_NO", null, valor, null, null, null));

        assertThat(errores).containsExactly(Map.entry("valorInicial", "Tiene que ser true (sí) o false (no)"));
    }

    @Test
    @DisplayName("Un campo de conexión conserva la tabla con la que se conecta")
    void validarShouldKeepBloqueRelacionadoWhenTipoIsRelacion() {
        ConfiguracionCampo.Validada result =
                validar(new ConfiguracionCampo("Cliente", "RELACION", true, null, null, RELACIONADO_ID, null));

        assertThat(result.tipo()).isEqualTo(TipoCampo.RELACION);
        assertThat(result.bloqueRelacionadoId()).isEqualTo(RELACIONADO_ID);
        assertThat(result.opciones()).isEmpty();
    }

    @Test
    @DisplayName("Un campo de conexión sin tabla es inválido")
    void validarShouldRejectRelacionWithoutBloque() {
        Map<String, String> errores = erroresDe(new ConfiguracionCampo("Cliente", "RELACION", null, null, null, null, null));

        assertThat(errores).containsExactly(Map.entry("bloqueRelacionadoId", "Elegí la tabla con la que se conecta"));
    }

    @Test
    @DisplayName("Un campo de conexión no admite valor inicial")
    void validarShouldRejectRelacionWithValorInicial() {
        Map<String, String> errores = erroresDe(
                new ConfiguracionCampo("Cliente", "RELACION", null, RELACIONADO_ID.toString(), null, RELACIONADO_ID, null));

        assertThat(errores).containsExactly(Map.entry("valorInicial", "Una conexión no tiene valor inicial"));
    }

    @Test
    @DisplayName("Descarta la tabla conectada de un campo que no es de conexión")
    void validarShouldDropBloqueRelacionadoWhenTipoIsNotRelacion() {
        ConfiguracionCampo.Validada result =
                validar(new ConfiguracionCampo("Cliente", "TEXTO", null, null, null, RELACIONADO_ID, null));

        assertThat(result.bloqueRelacionadoId()).isNull();
    }

    // --- act ---
    private ConfiguracionCampo.Validada validar(ConfiguracionCampo configuracion) {
        return configuracion.validar();
    }

    private Map<String, String> erroresDe(ConfiguracionCampo configuracion) {
        return catchThrowableOfType(CampoInvalidoException.class, configuracion::validar).getErrores();
    }

    @Test
    @DisplayName("Acepta una fórmula bien armada y guarda la expresión sin espacios en los bordes")
    void validarShouldAcceptFormula() {
        ConfiguracionCampo.Validada result =
                new ConfiguracionCampo("Total", "FORMULA", true, null, null, null, " 2 * (3 + 1) ").validar();

        assertThat(result.tipo()).isEqualTo(TipoCampo.FORMULA);
        assertThat(result.formula()).isEqualTo("2 * (3 + 1)");
    }

    @Test
    @DisplayName("Rechaza una fórmula vacía o mal armada con el mensaje de la expresión")
    void validarShouldRejectInvalidFormula() {
        assertThat(erroresDe(new ConfiguracionCampo("Total", "FORMULA", null, null, null, null, null)))
                .containsEntry("formula", "Armá la fórmula");
        assertThat(erroresDe(new ConfiguracionCampo("Total", "FORMULA", null, null, null, null, "2 +")))
                .containsEntry("formula", "La fórmula está incompleta");
    }

    @Test
    @DisplayName("Una fórmula no tiene valor inicial")
    void validarShouldRejectInitialValueOfFormula() {
        assertThat(erroresDe(new ConfiguracionCampo("Total", "FORMULA", null, "3", null, null, "1 + 1")))
                .containsEntry("valorInicial", "Una fórmula no tiene valor inicial");
    }

    @Test
    @DisplayName("Descarta la fórmula de un campo que no es de tipo fórmula")
    void validarShouldDropFormulaOfOtherTypes() {
        ConfiguracionCampo.Validada result =
                new ConfiguracionCampo("Dato", "TEXTO", null, null, null, null, "1 + 1").validar();

        assertThat(result.formula()).isNull();
    }
}
