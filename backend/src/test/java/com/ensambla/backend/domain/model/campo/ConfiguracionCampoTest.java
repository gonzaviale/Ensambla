package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class ConfiguracionCampoTest {

    private static final BloqueId TABLA_RELACIONADA = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final String TIPO_INVALIDO =
            "Elegí un tipo válido: texto, número, fecha, opción, sí/no, conexión o fórmula";

    private Campo campo;
    private List<Campo> camposPorTipo;
    private Map<String, String> errores;

    @Test
    @DisplayName("Un campo de texto queda con el nombre sin espacios, no obligatorio y sin valor inicial")
    void unCampoDeTextoQuedaConElNombreSinEspaciosNoObligatorioYSinValorInicial() {
        cuandoSeValida(unTextoConBlancosAlrededor());

        entoncesElCampoEsDeTextoNoObligatorioSinValorInicialLlamado("Producto");
    }

    @Test
    @DisplayName("Se conserva la marca de obligatorio")
    void seConservaLaMarcaDeObligatorio() {
        cuandoSeValida(unTextoObligatorio());

        entoncesElCampoEsObligatorio();
    }

    @Test
    @DisplayName("Cada tipo elegido da el campo de su clase")
    void cadaTipoElegidoDaElCampoDeSuClase() {
        cuandoSeValidaUnaConfiguracionDeCadaTipo();

        entoncesSeObtieneUnCampoDeCadaTipo();
    }

    @Test
    @DisplayName("Un campo que no es de opción ignora las opciones que le mandan")
    void unCampoQueNoEsDeOpcionIgnoraLasOpcionesQueLeMandan() {
        cuandoSeValida(unNumeroConOpcionesDeMas());

        entoncesElCampoNoTieneOpciones();
    }

    @Test
    @DisplayName("Un campo de opción queda sin espacios en las opciones y en el valor inicial")
    void unCampoDeOpcionQuedaSinEspaciosEnLasOpcionesYEnElValorInicial() {
        cuandoSeValida(unaOpcionConEspacios());

        entoncesLaOpcionInicialEsChicoEntreChicoYGrande();
    }

    @Test
    @DisplayName("Un nombre de ciento veinte caracteres se acepta")
    void unNombreDeCientoVeinteCaracteresSeAcepta() {
        cuandoSeValida(unaConfiguracion("a".repeat(120), "TEXTO"));

        entoncesElNombreTieneCaracteres(120);
    }

    @Test
    @DisplayName("Un nombre vacío se rechaza")
    void unNombreVacioSeRechaza() {
        cuandoSeIntentaValidar(unaConfiguracion("   ", "TEXTO"));

        entoncesSoloFallaElDato("nombre", "Escribí un nombre");
    }

    @Test
    @DisplayName("Un campo sin nombre se rechaza")
    void unCampoSinNombreSeRechaza() {
        cuandoSeIntentaValidar(unaConfiguracion(null, "TEXTO"));

        entoncesSoloFallaElDato("nombre");
    }

    @Test
    @DisplayName("Un nombre de más de ciento veinte caracteres se rechaza")
    void unNombreDeMasDeCientoVeinteCaracteresSeRechaza() {
        cuandoSeIntentaValidar(unaConfiguracion("a".repeat(121), "TEXTO"));

        entoncesSoloFallaElDato("nombre", "Máximo 120 caracteres");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ARCHIVO", "texto", "", "SI NO"})
    @DisplayName("Un tipo fuera de la lista se rechaza")
    void unTipoFueraDeLaListaSeRechaza(String tipo) {
        cuandoSeIntentaValidar(unaConfiguracion("Dato", tipo));

        entoncesSoloFallaElDato("tipo", TIPO_INVALIDO);
    }

    @Test
    @DisplayName("Un campo sin tipo se rechaza")
    void unCampoSinTipoSeRechaza() {
        cuandoSeIntentaValidar(unaConfiguracion("Dato", null));

        entoncesSoloFallaElDato("tipo");
    }

    @Test
    @DisplayName("Se informan juntos el error del nombre y el del tipo")
    void seInformanJuntosElErrorDelNombreYElDelTipo() {
        cuandoSeIntentaValidar(unaConfiguracion("", "ARCHIVO"));

        entoncesFallanLosDatos("nombre", "tipo");
    }

    @Test
    @DisplayName("Un campo de opción sin opciones se rechaza")
    void unCampoDeOpcionSinOpcionesSeRechaza() {
        cuandoSeIntentaValidar(unaOpcion(null, null));

        entoncesSoloFallaElDato("opciones", "Agregá al menos una opción");
    }

    @Test
    @DisplayName("Un campo de opción con la lista vacía se rechaza")
    void unCampoDeOpcionConLaListaVaciaSeRechaza() {
        cuandoSeIntentaValidar(unaOpcion(null, List.of()));

        entoncesSoloFallaElDato("opciones", "Agregá al menos una opción");
    }

    @Test
    @DisplayName("Una opción en blanco se rechaza")
    void unaOpcionEnBlancoSeRechaza() {
        cuandoSeIntentaValidar(unaOpcion(null, List.of("Chico", "  ")));

        entoncesSoloFallaElDato("opciones", "Las opciones no pueden quedar vacías");
    }

    @Test
    @DisplayName("Una opción nula se rechaza")
    void unaOpcionNulaSeRechaza() {
        cuandoSeIntentaValidar(unaOpcion(null, Arrays.asList("Chico", null)));

        entoncesSoloFallaElDato("opciones", "Las opciones no pueden quedar vacías");
    }

    @Test
    @DisplayName("Las opciones repetidas se rechazan sin distinguir mayúsculas ni espacios")
    void lasOpcionesRepetidasSeRechazanSinDistinguirMayusculasNiEspacios() {
        cuandoSeIntentaValidar(unaOpcion(null, List.of("Chico", " CHICO")));

        entoncesSoloFallaElDato("opciones", "Hay opciones repetidas");
    }

    @Test
    @DisplayName("Un valor inicial que no está entre las opciones se rechaza")
    void unValorInicialQueNoEstaEntreLasOpcionesSeRechaza() {
        cuandoSeIntentaValidar(unaOpcion("Mediano", talles()));

        entoncesSoloFallaElDato("valorInicial", "Tiene que ser una de las opciones");
    }

    @Test
    @DisplayName("Un valor inicial de texto de mil caracteres se acepta")
    void unValorInicialDeTextoDeMilCaracteresSeAcepta() {
        cuandoSeValida(conValorInicial("TEXTO", "x".repeat(1000)));

        entoncesElValorInicialTieneCaracteres(1000);
    }

    @Test
    @DisplayName("Un valor inicial de texto de más de mil caracteres se rechaza")
    void unValorInicialDeTextoDeMasDeMilCaracteresSeRechaza() {
        cuandoSeIntentaValidar(conValorInicial("TEXTO", "x".repeat(1001)));

        entoncesSoloFallaElDato("valorInicial", "Máximo 1000 caracteres");
    }

    @ParameterizedTest
    @ValueSource(strings = {"12", "12.5", "-3", "0.25"})
    @DisplayName("Un número entero o decimal con punto se acepta como valor inicial")
    void unNumeroEnteroODecimalConPuntoSeAceptaComoValorInicial(String valor) {
        cuandoSeValida(conValorInicial("NUMERO", valor));

        entoncesElValorInicialEs(valor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12,5", "doce", "1e5", ".5", "12.", "+3"})
    @DisplayName("Un valor inicial que no es un número con punto decimal se rechaza")
    void unValorInicialQueNoEsUnNumeroConPuntoDecimalSeRechaza(String valor) {
        cuandoSeIntentaValidar(conValorInicial("NUMERO", valor));

        entoncesSoloFallaElDato("valorInicial", "Tiene que ser un número, por ejemplo 12.5");
    }

    @Test
    @DisplayName("Una fecha con formato año-mes-día se acepta como valor inicial")
    void unaFechaConFormatoAnioMesDiaSeAceptaComoValorInicial() {
        cuandoSeValida(conValorInicial("FECHA", "2026-10-09"));

        entoncesElValorInicialEs("2026-10-09");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-02-31", "09/10/2026", "2026-1-9", "+2026-10-09"})
    @DisplayName("Una fecha inexistente o con otro formato se rechaza como valor inicial")
    void unaFechaInexistenteOConOtroFormatoSeRechazaComoValorInicial(String valor) {
        cuandoSeIntentaValidar(conValorInicial("FECHA", valor));

        entoncesSoloFallaElDato("valorInicial", "Tiene que ser una fecha con el formato AAAA-MM-DD");
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    @DisplayName("Verdadero y falso se aceptan como valor inicial de un campo sí/no")
    void trueYFalseSeAceptanComoValorInicialDeSiNo(String valor) {
        cuandoSeValida(conValorInicial("SI_NO", valor));

        entoncesElValorInicialEs(valor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"sí", "TRUE", "1"})
    @DisplayName("Cualquier otro valor inicial para un campo sí/no se rechaza")
    void otroValorInicialParaSiNoSeRechaza(String valor) {
        cuandoSeIntentaValidar(conValorInicial("SI_NO", valor));

        entoncesSoloFallaElDato("valorInicial", "Tiene que ser true (sí) o false (no)");
    }

    @Test
    @DisplayName("Un campo de conexión conserva la tabla con la que se conecta")
    void unCampoDeConexionConservaLaTablaConLaQueSeConecta() {
        cuandoSeValida(unaConexionAUnaTabla());

        entoncesEsUnaConexionConLaTabla(TABLA_RELACIONADA);
    }

    @Test
    @DisplayName("Un campo de conexión sin tabla se rechaza")
    void unCampoDeConexionSinTablaSeRechaza() {
        cuandoSeIntentaValidar(unaConfiguracion("Cliente", "RELACION"));

        entoncesSoloFallaElDato("bloqueRelacionadoId", "Elegí la tabla con la que se conecta");
    }

    @Test
    @DisplayName("Un campo de conexión no admite valor inicial")
    void unCampoDeConexionNoAdmiteValorInicial() {
        cuandoSeIntentaValidar(unaConexionConValorInicial());

        entoncesSoloFallaElDato("valorInicial", "Una conexión no tiene valor inicial");
    }

    @Test
    @DisplayName("Un campo que no es de conexión descarta la tabla conectada")
    void unCampoQueNoEsDeConexionDescartaLaTablaConectada() {
        cuandoSeValida(unTextoConTablaConectada());

        entoncesNoTieneTablaConectada();
    }

    @Test
    @DisplayName("Una fórmula bien armada se guarda sin espacios en los bordes")
    void unaFormulaBienArmadaSeGuardaSinEspaciosEnLosBordes() {
        cuandoSeValida(unaFormulaConEspaciosEnLosBordes());

        entoncesEsUnaFormulaConLaExpresion("2 * (3 + 1)");
    }

    @Test
    @DisplayName("Una fórmula vacía se rechaza con el mensaje de la expresión")
    void unaFormulaVaciaSeRechazaConElMensajeDeLaExpresion() {
        cuandoSeIntentaValidar(unaConfiguracion("Total", "FORMULA"));

        entoncesFallaElDato("formula", "Armá la fórmula");
    }

    @Test
    @DisplayName("Una fórmula incompleta se rechaza con el mensaje de la expresión")
    void unaFormulaIncompletaSeRechazaConElMensajeDeLaExpresion() {
        cuandoSeIntentaValidar(unaFormulaIncompleta());

        entoncesFallaElDato("formula", "La fórmula está incompleta");
    }

    @Test
    @DisplayName("Una fórmula no tiene valor inicial")
    void unaFormulaNoTieneValorInicial() {
        cuandoSeIntentaValidar(unaFormulaConValorInicial());

        entoncesFallaElDato("valorInicial", "Una fórmula no tiene valor inicial");
    }

    @Test
    @DisplayName("Un campo que no es fórmula descarta la expresión")
    void unCampoQueNoEsFormulaDescartaLaExpresion() {
        cuandoSeValida(unTextoConExpresion());

        entoncesNoTieneExpresion();
    }

    private void cuandoSeValida(ConfiguracionCampo configuracion) {
        campo = configuracion.validar();
    }

    private void cuandoSeIntentaValidar(ConfiguracionCampo configuracion) {
        errores = catchThrowableOfType(CampoInvalidoException.class, configuracion::validar).getErrores();
    }

    private void cuandoSeValidaUnaConfiguracionDeCadaTipo() {
        camposPorTipo = new ArrayList<>();
        for (TipoCampo tipo : TipoCampo.values()) {
            camposPorTipo.add(configuracionValidaDe(tipo).validar());
        }
    }

    private void entoncesElCampoEsDeTextoNoObligatorioSinValorInicialLlamado(String nombre) {
        assertThat(campo.getNombre()).isEqualTo(nombre);
        assertThat(campo.esObligatorio()).isFalse();
        assertThat(campo.getValorInicial()).isNull();
        assertThat(campo).isInstanceOf(CampoTexto.class);
    }

    private void entoncesElCampoEsObligatorio() {
        assertThat(campo.esObligatorio()).isTrue();
    }

    private void entoncesSeObtieneUnCampoDeCadaTipo() {
        assertThat(camposPorTipo).extracting(Campo::tipo).containsExactly(TipoCampo.values());
    }

    private void entoncesElCampoNoTieneOpciones() {
        assertThat(campo.getOpciones()).isEmpty();
    }

    private void entoncesLaOpcionInicialEsChicoEntreChicoYGrande() {
        assertThat(campo.getOpciones()).containsExactly("Chico", "Grande");
        assertThat(campo.getValorInicial()).isEqualTo("Chico");
    }

    private void entoncesElNombreTieneCaracteres(int cantidad) {
        assertThat(campo.getNombre()).hasSize(cantidad);
    }

    private void entoncesElValorInicialTieneCaracteres(int cantidad) {
        assertThat(campo.getValorInicial()).hasSize(cantidad);
    }

    private void entoncesElValorInicialEs(String valor) {
        assertThat(campo.getValorInicial()).isEqualTo(valor);
    }

    private void entoncesEsUnaConexionConLaTabla(BloqueId tabla) {
        assertThat(campo.esRelacion()).isTrue();
        assertThat(campo.getBloqueRelacionadoId()).isEqualTo(tabla);
    }

    private void entoncesNoTieneTablaConectada() {
        assertThat(campo.getBloqueRelacionadoId()).isNull();
    }

    private void entoncesEsUnaFormulaConLaExpresion(String expresion) {
        assertThat(campo.esFormula()).isTrue();
        assertThat(campo.getFormula()).isEqualTo(expresion);
    }

    private void entoncesNoTieneExpresion() {
        assertThat(campo.getFormula()).isNull();
    }

    private void entoncesSoloFallaElDato(String dato, String mensaje) {
        assertThat(errores).containsExactly(Map.entry(dato, mensaje));
    }

    private void entoncesSoloFallaElDato(String dato) {
        assertThat(errores).containsOnlyKeys(dato);
    }

    private void entoncesFallanLosDatos(String... datos) {
        assertThat(errores).containsOnlyKeys(datos);
    }

    private void entoncesFallaElDato(String dato, String mensaje) {
        assertThat(errores).containsEntry(dato, mensaje);
    }

    private ConfiguracionCampo unaConfiguracion(String nombre, String tipo) {
        return new ConfiguracionCampo(nombre, tipo, null, null, null, null, null);
    }

    private ConfiguracionCampo conValorInicial(String tipo, String valorInicial) {
        return new ConfiguracionCampo("Dato", tipo, null, valorInicial, null, null, null);
    }

    private ConfiguracionCampo unaOpcion(String valorInicial, List<String> opciones) {
        return new ConfiguracionCampo("Talle", "OPCION", null, valorInicial, opciones, null, null);
    }

    private ConfiguracionCampo unTextoConBlancosAlrededor() {
        return new ConfiguracionCampo("  Producto  ", "TEXTO", null, "  ", null, null, null);
    }

    private ConfiguracionCampo unTextoObligatorio() {
        return new ConfiguracionCampo("Producto", "TEXTO", true, null, null, null, null);
    }

    private ConfiguracionCampo unNumeroConOpcionesDeMas() {
        return new ConfiguracionCampo("Precio", "NUMERO", false, null, talles(), null, null);
    }

    private ConfiguracionCampo unaOpcionConEspacios() {
        return new ConfiguracionCampo("Talle", "OPCION", false, " Chico ", List.of(" Chico ", "Grande "), null, null);
    }

    private ConfiguracionCampo unaConexionAUnaTabla() {
        return new ConfiguracionCampo("Cliente", "RELACION", true, null, null, TABLA_RELACIONADA, null);
    }

    private ConfiguracionCampo unaConexionConValorInicial() {
        return new ConfiguracionCampo("Cliente", "RELACION", null, "x", null, TABLA_RELACIONADA, null);
    }

    private ConfiguracionCampo unTextoConTablaConectada() {
        return new ConfiguracionCampo("Cliente", "TEXTO", null, null, null, TABLA_RELACIONADA, null);
    }

    private ConfiguracionCampo unaFormulaConEspaciosEnLosBordes() {
        return new ConfiguracionCampo("Total", "FORMULA", true, null, null, null, " 2 * (3 + 1) ");
    }

    private ConfiguracionCampo unaFormulaIncompleta() {
        return new ConfiguracionCampo("Total", "FORMULA", null, null, null, null, "2 +");
    }

    private ConfiguracionCampo unaFormulaConValorInicial() {
        return new ConfiguracionCampo("Total", "FORMULA", null, "3", null, null, "1 + 1");
    }

    private ConfiguracionCampo unTextoConExpresion() {
        return new ConfiguracionCampo("Dato", "TEXTO", null, null, null, null, "1 + 1");
    }

    private ConfiguracionCampo configuracionValidaDe(TipoCampo tipo) {
        return new ConfiguracionCampo("Dato", tipo.name(), null, null, talles(), TABLA_RELACIONADA, "1 + 1");
    }

    private List<String> talles() {
        return List.of("Chico", "Grande");
    }
}
