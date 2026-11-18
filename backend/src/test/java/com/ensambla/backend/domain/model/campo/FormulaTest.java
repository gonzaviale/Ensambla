package com.ensambla.backend.domain.model.campo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class FormulaTest {

    private static final CampoId CANTIDAD_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId PRECIO_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final String CANTIDAD = "{" + CANTIDAD_ID + "}";
    private static final String PRECIO = "{" + PRECIO_ID + "}";

    private BigDecimal resultado;
    private Set<CampoId> referencias;
    private Throwable rechazo;

    @Test
    @DisplayName("Multiplica dos campos del registro")
    void multiplicaDosCamposDelRegistro() {
        cuandoSeEvaluaLaCantidadPorElPrecioCon("3", "12.5");

        entoncesElResultadoEs("37.5");
    }

    @Test
    @DisplayName("La multiplicación se resuelve antes que la suma")
    void laMultiplicacionVaAntesQueLaSuma() {
        cuandoSeEvalua("2 + 3 * 4");

        entoncesElResultadoEs("14");
    }

    @Test
    @DisplayName("Los paréntesis cambian el orden de las cuentas")
    void losParentesisCambianElOrdenDeLasCuentas() {
        cuandoSeEvalua("(2 + 3) * 4");

        entoncesElResultadoEs("20");
    }

    @Test
    @DisplayName("El menos unario niega lo que sigue")
    void elMenosUnarioNiegaLoQueSigue() {
        cuandoSeEvalua("-(2 - 5) - 1");

        entoncesElResultadoEs("2");
    }

    @Test
    @DisplayName("La división puede dar decimales")
    void laDivisionPuedeDarDecimales() {
        cuandoSeEvalua("10 / 4");

        entoncesElResultadoEs("2.5");
    }

    @Test
    @DisplayName("El resultado no lleva notación científica")
    void elResultadoNoLlevaNotacionCientifica() {
        cuandoSeEvalua("1500 * 1");

        entoncesElResultadoSeEscribe("1500");
    }

    @Test
    @DisplayName("El resultado no lleva ceros de más en los decimales")
    void elResultadoNoLlevaCerosDeMasEnLosDecimales() {
        cuandoSeEvalua("2.50 * 1");

        entoncesElResultadoSeEscribe("2.5");
    }

    @Test
    @DisplayName("El cero se escribe sin decimales")
    void elCeroSeEscribeSinDecimales() {
        cuandoSeEvalua("0 * 5");

        entoncesElResultadoSeEscribe("0");
    }

    @Test
    @DisplayName("Queda sin valor si falta el dato del primer operando")
    void quedaSinValorSiFaltaElDatoDelPrimerOperando() {
        cuandoSeEvaluaSinDatos(CANTIDAD + " * 2");

        entoncesNoHayResultado();
    }

    @Test
    @DisplayName("Queda sin valor si falta el dato de una negación")
    void quedaSinValorSiFaltaElDatoDeUnaNegacion() {
        cuandoSeEvaluaSinDatos("-" + CANTIDAD);

        entoncesNoHayResultado();
    }

    @Test
    @DisplayName("Queda sin valor si falta el dato del segundo operando")
    void quedaSinValorSiFaltaElDatoDelSegundoOperando() {
        cuandoSeEvaluaSinDatos("2 * " + CANTIDAD);

        entoncesNoHayResultado();
    }

    @Test
    @DisplayName("Queda sin valor si se divide por cero")
    void quedaSinValorSiSeDividePorCero() {
        cuandoSeEvaluaSinDatos("5 / 0");

        entoncesNoHayResultado();
    }

    @Test
    @DisplayName("Informa los campos que usa, sin repetirlos")
    void informaLosCamposQueUsaSinRepetir() {
        cuandoSePidenLasReferenciasDe(CANTIDAD + " * " + PRECIO + " + " + CANTIDAD);

        entoncesLasReferenciasSonLaCantidadYElPrecio();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "2 +", "(2 + 3", "2 3", "abc", "*", ".", "{no-es-un-id}",
            "{c0000000-0000-4000-8000-000000000001", "{c0000000-0000-4000-8000-000000000001 + 1",
            "{zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz}", "2 + )"})
    @DisplayName("Una expresión mal armada se rechaza")
    void unaExpresionMalArmadaSeRechaza(String texto) {
        cuandoSeParsea(texto);

        entoncesSeRechazaLaExpresion();
    }

    @Test
    @DisplayName("Una expresión nula se rechaza")
    void unaExpresionNulaSeRechaza() {
        cuandoSeParsea(null);

        entoncesSeRechazaPorque("Armá la fórmula");
    }

    @Test
    @DisplayName("Una expresión demasiado larga se rechaza")
    void unaExpresionDemasiadoLargaSeRechaza() {
        cuandoSeParseaUnaExpresionDemasiadoLarga();

        entoncesSeRechazaPorque("Máximo " + Formula.MAX_CARACTERES + " caracteres");
    }

    private void cuandoSeEvalua(String expresion) {
        resultado = Formula.parsear(expresion).evaluar(id -> null);
    }

    private void cuandoSeEvaluaSinDatos(String expresion) {
        cuandoSeEvalua(expresion);
    }

    private void cuandoSeEvaluaLaCantidadPorElPrecioCon(String cantidad, String precio) {
        Map<CampoId, String> valores = Map.of(CANTIDAD_ID, cantidad, PRECIO_ID, precio);
        resultado = Formula.parsear(CANTIDAD + " * " + PRECIO).evaluar(id -> new BigDecimal(valores.get(id)));
    }

    private void cuandoSePidenLasReferenciasDe(String expresion) {
        referencias = Formula.parsear(expresion).referencias();
    }

    private void cuandoSeParsea(String texto) {
        rechazo = catchThrowable(() -> Formula.parsear(texto));
    }

    private void cuandoSeParseaUnaExpresionDemasiadoLarga() {
        cuandoSeParsea("1 + ".repeat(Formula.MAX_CARACTERES) + "1");
    }

    private void entoncesElResultadoEs(String esperado) {
        assertThat(resultado).isEqualByComparingTo(esperado);
    }

    private void entoncesElResultadoSeEscribe(String esperado) {
        assertThat(resultado.toString()).isEqualTo(esperado);
    }

    private void entoncesNoHayResultado() {
        assertThat(resultado).isNull();
    }

    private void entoncesLasReferenciasSonLaCantidadYElPrecio() {
        assertThat(referencias).containsExactly(CANTIDAD_ID, PRECIO_ID);
    }

    private void entoncesSeRechazaLaExpresion() {
        assertThat(rechazo).isInstanceOf(IllegalArgumentException.class);
    }

    private void entoncesSeRechazaPorque(String motivo) {
        assertThat(rechazo).isInstanceOf(IllegalArgumentException.class).hasMessage(motivo);
    }
}
