package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class ConfiguracionCampoTest {

    private static final BloqueId TABLA_RELACIONADA = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final String TIPO_INVALIDO =
            "Elegí un tipo válido: texto, número, fecha, opción, sí/no, conexión o fórmula";

    @Test
    @DisplayName("Un campo de texto queda con el nombre sin espacios, no obligatorio y sin valor inicial")
    void validarShouldNormalizeTextoWithDefaults() {
        Campo campo = validar(new ConfiguracionCampo("  Producto  ", "TEXTO", null, "  ", null, null, null));

        assertThat(campo.getNombre()).isEqualTo("Producto");
        assertThat(campo.esObligatorio()).isFalse();
        assertThat(campo.getValorInicial()).isNull();
        assertThat(campo).isInstanceOf(CampoTexto.class);
    }

    @Test
    @DisplayName("Conserva la marca de obligatorio")
    void validarShouldKeepObligatorio() {
        Campo campo = validar(new ConfiguracionCampo("Producto", "TEXTO", true, null, null, null, null));

        assertThat(campo.esObligatorio()).isTrue();
    }

    @Test
    @DisplayName("Cada tipo elegido da el campo de su clase")
    void validarShouldBuildTheCampoOfTheChosenTipo() {
        for (TipoCampo tipo : TipoCampo.values()) {
            assertThat(validar(configuracionValidaDe(tipo)).tipo()).isEqualTo(tipo);
        }
    }

    @Test
    @DisplayName("Un campo que no es de opción ignora las opciones que le mandan")
    void validarShouldIgnoreOpcionesWhenTipoIsNotOpcion() {
        Campo campo = validar(new ConfiguracionCampo("Precio", "NUMERO", false, null, talles(), null, null));

        assertThat(campo.getOpciones()).isEmpty();
    }

    @Test
    @DisplayName("Un campo de opción queda sin espacios en las opciones y en el valor inicial")
    void validarShouldStripOpcionesAndValorInicial() {
        Campo campo = validar(
                new ConfiguracionCampo("Talle", "OPCION", false, " Chico ", List.of(" Chico ", "Grande "), null, null));

        assertThat(campo.getOpciones()).containsExactly("Chico", "Grande");
        assertThat(campo.getValorInicial()).isEqualTo("Chico");
    }

    @Test
    @DisplayName("Acepta un nombre de 120 caracteres")
    void validarShouldAcceptNombreAtTheLimit() {
        Campo campo = validar(configuracion("a".repeat(120), "TEXTO"));

        assertThat(campo.getNombre()).hasSize(120);
    }

    @Test
    @DisplayName("Rechaza un nombre vacío")
    void validarShouldRejectBlankNombre() {
        Map<String, String> errores = erroresAlValidar(configuracion("   ", "TEXTO"));

        thenFallaSoloEl(errores, "nombre", "Escribí un nombre");
    }

    @Test
    @DisplayName("Rechaza un campo sin nombre")
    void validarShouldRejectNullNombre() {
        Map<String, String> errores = erroresAlValidar(configuracion(null, "TEXTO"));

        assertThat(errores).containsOnlyKeys("nombre");
    }

    @Test
    @DisplayName("Rechaza un nombre de más de 120 caracteres")
    void validarShouldRejectTooLongNombre() {
        Map<String, String> errores = erroresAlValidar(configuracion("a".repeat(121), "TEXTO"));

        thenFallaSoloEl(errores, "nombre", "Máximo 120 caracteres");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ARCHIVO", "texto", "", "SI NO"})
    @DisplayName("Rechaza un tipo fuera de la lista")
    void validarShouldRejectUnknownTipo(String tipo) {
        Map<String, String> errores = erroresAlValidar(configuracion("Dato", tipo));

        thenFallaSoloEl(errores, "tipo", TIPO_INVALIDO);
    }

    @Test
    @DisplayName("Rechaza un campo sin tipo")
    void validarShouldRejectNullTipo() {
        Map<String, String> errores = erroresAlValidar(configuracion("Dato", null));

        assertThat(errores).containsOnlyKeys("tipo");
    }

    @Test
    @DisplayName("Informa juntos el error del nombre y el del tipo")
    void validarShouldReportNombreAndTipoTogether() {
        Map<String, String> errores = erroresAlValidar(configuracion("", "ARCHIVO"));

        assertThat(errores).containsOnlyKeys("nombre", "tipo");
    }

    @Test
    @DisplayName("Un campo de opción sin opciones es inválido")
    void validarShouldRejectOpcionWithoutOpciones() {
        Map<String, String> errores = erroresAlValidar(opcion(null, null));

        thenFallaSoloEl(errores, "opciones", "Agregá al menos una opción");
    }

    @Test
    @DisplayName("Un campo de opción con la lista vacía es inválido")
    void validarShouldRejectOpcionWithEmptyList() {
        Map<String, String> errores = erroresAlValidar(opcion(null, List.of()));

        thenFallaSoloEl(errores, "opciones", "Agregá al menos una opción");
    }

    @Test
    @DisplayName("Rechaza una opción en blanco")
    void validarShouldRejectBlankOpcion() {
        Map<String, String> errores = erroresAlValidar(opcion(null, List.of("Chico", "  ")));

        thenFallaSoloEl(errores, "opciones", "Las opciones no pueden quedar vacías");
    }

    @Test
    @DisplayName("Rechaza una opción nula")
    void validarShouldRejectNullOpcion() {
        Map<String, String> errores = erroresAlValidar(opcion(null, Arrays.asList("Chico", null)));

        thenFallaSoloEl(errores, "opciones", "Las opciones no pueden quedar vacías");
    }

    @Test
    @DisplayName("Rechaza opciones repetidas sin distinguir mayúsculas ni espacios")
    void validarShouldRejectRepeatedOpciones() {
        Map<String, String> errores = erroresAlValidar(opcion(null, List.of("Chico", " CHICO")));

        thenFallaSoloEl(errores, "opciones", "Hay opciones repetidas");
    }

    @Test
    @DisplayName("Rechaza un valor inicial que no está entre las opciones")
    void validarShouldRejectValorInicialOutsideOpciones() {
        Map<String, String> errores = erroresAlValidar(opcion("Mediano", talles()));

        thenFallaSoloEl(errores, "valorInicial", "Tiene que ser una de las opciones");
    }

    @Test
    @DisplayName("Acepta un valor inicial de texto de hasta 1000 caracteres")
    void validarShouldAcceptTextoAtTheLimit() {
        Campo campo = validar(conValorInicial("TEXTO", "x".repeat(1000)));

        assertThat(campo.getValorInicial()).hasSize(1000);
    }

    @Test
    @DisplayName("Rechaza un valor inicial de texto de más de 1000 caracteres")
    void validarShouldRejectTooLongTexto() {
        Map<String, String> errores = erroresAlValidar(conValorInicial("TEXTO", "x".repeat(1001)));

        thenFallaSoloEl(errores, "valorInicial", "Máximo 1000 caracteres");
    }

    @ParameterizedTest
    @ValueSource(strings = {"12", "12.5", "-3", "0.25"})
    @DisplayName("Acepta números enteros y decimales con punto como valor inicial")
    void validarShouldAcceptNumero(String valor) {
        Campo campo = validar(conValorInicial("NUMERO", valor));

        assertThat(campo.getValorInicial()).isEqualTo(valor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12,5", "doce", "1e5", ".5", "12.", "+3"})
    @DisplayName("Rechaza un valor inicial que no es un número con punto decimal")
    void validarShouldRejectInvalidNumero(String valor) {
        Map<String, String> errores = erroresAlValidar(conValorInicial("NUMERO", valor));

        thenFallaSoloEl(errores, "valorInicial", "Tiene que ser un número, por ejemplo 12.5");
    }

    @Test
    @DisplayName("Acepta una fecha AAAA-MM-DD como valor inicial")
    void validarShouldAcceptFecha() {
        Campo campo = validar(conValorInicial("FECHA", "2026-10-09"));

        assertThat(campo.getValorInicial()).isEqualTo("2026-10-09");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-02-31", "09/10/2026", "2026-1-9", "+2026-10-09"})
    @DisplayName("Rechaza una fecha inexistente o con otro formato como valor inicial")
    void validarShouldRejectInvalidFecha(String valor) {
        Map<String, String> errores = erroresAlValidar(conValorInicial("FECHA", valor));

        thenFallaSoloEl(errores, "valorInicial", "Tiene que ser una fecha con el formato AAAA-MM-DD");
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    @DisplayName("Acepta true y false como valor inicial de sí/no")
    void validarShouldAcceptSiNo(String valor) {
        Campo campo = validar(conValorInicial("SI_NO", valor));

        assertThat(campo.getValorInicial()).isEqualTo(valor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"sí", "TRUE", "1"})
    @DisplayName("Rechaza otro valor inicial para sí/no")
    void validarShouldRejectInvalidSiNo(String valor) {
        Map<String, String> errores = erroresAlValidar(conValorInicial("SI_NO", valor));

        thenFallaSoloEl(errores, "valorInicial", "Tiene que ser true (sí) o false (no)");
    }

    @Test
    @DisplayName("Un campo de conexión conserva la tabla con la que se conecta")
    void validarShouldKeepBloqueRelacionadoWhenTipoIsRelacion() {
        Campo campo = validar(new ConfiguracionCampo("Cliente", "RELACION", true, null, null, TABLA_RELACIONADA, null));

        assertThat(campo.esRelacion()).isTrue();
        assertThat(campo.getBloqueRelacionadoId()).isEqualTo(TABLA_RELACIONADA);
    }

    @Test
    @DisplayName("Un campo de conexión sin tabla es inválido")
    void validarShouldRejectRelacionWithoutBloque() {
        Map<String, String> errores = erroresAlValidar(configuracion("Cliente", "RELACION"));

        thenFallaSoloEl(errores, "bloqueRelacionadoId", "Elegí la tabla con la que se conecta");
    }

    @Test
    @DisplayName("Un campo de conexión no admite valor inicial")
    void validarShouldRejectRelacionWithValorInicial() {
        Map<String, String> errores = erroresAlValidar(
                new ConfiguracionCampo("Cliente", "RELACION", null, "x", null, TABLA_RELACIONADA, null));

        thenFallaSoloEl(errores, "valorInicial", "Una conexión no tiene valor inicial");
    }

    @Test
    @DisplayName("Un campo que no es de conexión descarta la tabla conectada")
    void validarShouldDropBloqueRelacionadoWhenTipoIsNotRelacion() {
        Campo campo = validar(new ConfiguracionCampo("Cliente", "TEXTO", null, null, null, TABLA_RELACIONADA, null));

        assertThat(campo.getBloqueRelacionadoId()).isNull();
    }

    @Test
    @DisplayName("Una fórmula bien armada se guarda sin espacios en los bordes")
    void validarShouldAcceptFormula() {
        Campo campo = validar(new ConfiguracionCampo("Total", "FORMULA", true, null, null, null, " 2 * (3 + 1) "));

        assertThat(campo.esFormula()).isTrue();
        assertThat(campo.getFormula()).isEqualTo("2 * (3 + 1)");
    }

    @Test
    @DisplayName("Rechaza una fórmula vacía con el mensaje de la expresión")
    void validarShouldRejectEmptyFormula() {
        Map<String, String> errores = erroresAlValidar(configuracion("Total", "FORMULA"));

        assertThat(errores).containsEntry("formula", "Armá la fórmula");
    }

    @Test
    @DisplayName("Rechaza una fórmula incompleta con el mensaje de la expresión")
    void validarShouldRejectIncompleteFormula() {
        Map<String, String> errores = erroresAlValidar(
                new ConfiguracionCampo("Total", "FORMULA", null, null, null, null, "2 +"));

        assertThat(errores).containsEntry("formula", "La fórmula está incompleta");
    }

    @Test
    @DisplayName("Una fórmula no tiene valor inicial")
    void validarShouldRejectInitialValueOfFormula() {
        Map<String, String> errores = erroresAlValidar(
                new ConfiguracionCampo("Total", "FORMULA", null, "3", null, null, "1 + 1"));

        assertThat(errores).containsEntry("valorInicial", "Una fórmula no tiene valor inicial");
    }

    @Test
    @DisplayName("Un campo que no es fórmula descarta la expresión")
    void validarShouldDropFormulaOfOtherTypes() {
        Campo campo = validar(new ConfiguracionCampo("Dato", "TEXTO", null, null, null, null, "1 + 1"));

        assertThat(campo.getFormula()).isNull();
    }

    private Campo validar(ConfiguracionCampo configuracion) {
        return configuracion.validar();
    }

    private Map<String, String> erroresAlValidar(ConfiguracionCampo configuracion) {
        return catchThrowableOfType(CampoInvalidoException.class, configuracion::validar).getErrores();
    }

    private void thenFallaSoloEl(Map<String, String> errores, String dato, String mensaje) {
        assertThat(errores).containsExactly(Map.entry(dato, mensaje));
    }

    private ConfiguracionCampo configuracion(String nombre, String tipo) {
        return new ConfiguracionCampo(nombre, tipo, null, null, null, null, null);
    }

    private ConfiguracionCampo conValorInicial(String tipo, String valorInicial) {
        return new ConfiguracionCampo("Dato", tipo, null, valorInicial, null, null, null);
    }

    private ConfiguracionCampo opcion(String valorInicial, List<String> opciones) {
        return new ConfiguracionCampo("Talle", "OPCION", null, valorInicial, opciones, null, null);
    }

    private ConfiguracionCampo configuracionValidaDe(TipoCampo tipo) {
        return new ConfiguracionCampo("Dato", tipo.name(), null, null, talles(), TABLA_RELACIONADA, "1 + 1");
    }

    private List<String> talles() {
        return List.of("Chico", "Grande");
    }
}
