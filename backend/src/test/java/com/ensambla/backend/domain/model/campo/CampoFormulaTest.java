package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.exception.CampoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.ensambla.backend.support.CamposDePrueba.referenciaA;
import static com.ensambla.backend.support.CamposDePrueba.unCampoDeTexto;
import static com.ensambla.backend.support.CamposDePrueba.unCampoNumerico;
import static com.ensambla.backend.support.CamposDePrueba.unaFormulaQueSeCitaASiMisma;
import static com.ensambla.backend.support.CamposDePrueba.unaFormulaQueUsa;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CampoFormulaTest extends CampoDeCualquierTipoTest {

    private BigDecimal calculo;
    private Throwable rechazo;

    @Override
    protected TipoCampo tipoEsperado() {
        return TipoCampo.FORMULA;
    }

    @Override
    protected List<Object> valoresQueAcepta() {
        return List.of();
    }

    @Override
    protected List<Object> valoresQueRechaza() {
        return List.of(3, "3");
    }

    @Override
    protected String motivoDelRechazo() {
        return "Este dato se calcula solo";
    }

    @Test
    @DisplayName("Es una fórmula y conoce su expresión")
    void esUnaFormulaYConoceSuExpresion() {
        Campo precio = unCampoNumerico("Precio");
        CampoFormula doble = unaFormulaQueUsa(precio, "Doble");

        entoncesEsUnaFormulaConLaExpresion(doble, referenciaA(precio) + " * 2");
    }

    @Test
    @DisplayName("Calcula la fórmula con los valores que le da el registro")
    void calculaLaFormulaConLosValoresQueLeDaElRegistro() {
        CampoFormula doble = unaFormulaQueUsa(unCampoNumerico("Precio"), "Doble");

        cuandoSeCalculaConElValor(doble, "4");

        entoncesElCalculoEs("8");
    }

    @Test
    @DisplayName("En el reporte el resultado va como número")
    void enElReporteVaComoNumero() {
        cuandoSePideLaCeldaDeReporte("8");

        entoncesElResultadoEs(new BigDecimal("8"));
    }

    @Test
    @DisplayName("Acepta una fórmula que usa campos numéricos de la tabla")
    void aceptaUnaFormulaQueUsaCamposNumericosDeLaTabla() {
        Campo precio = unCampoNumerico("Precio");

        cuandoSeValidanLasReferencias(unaFormulaQueUsa(precio, "Doble"), precio);

        entoncesNoSeRechazaNada();
    }

    @Test
    @DisplayName("Rechaza una fórmula que usa un campo que no es numérico")
    void rechazaUnaFormulaQueUsaUnCampoQueNoEsNumerico() {
        Campo precioEnTexto = unCampoDeTexto("Precio");

        cuandoSeValidanLasReferencias(unaFormulaQueUsa(precioEnTexto, "Doble"), precioEnTexto);

        entoncesSeRechazaLaFormula();
    }

    @Test
    @DisplayName("Rechaza una fórmula que se cita a sí misma")
    void rechazaUnaFormulaQueSeCitaASiMisma() {
        CampoFormula total = unaFormulaQueSeCitaASiMisma("Total");

        cuandoSeValidanLasReferencias(total, unNumericoConElIdDe(total));

        entoncesSeRechazaLaFormula();
    }

    private Campo unNumericoConElIdDe(Campo otro) {
        Campo numerico = unCampoNumerico("Total");
        numerico.setId(otro.getId());
        return numerico;
    }

    private void cuandoSeCalculaConElValor(CampoFormula formula, String valor) {
        calculo = formula.calcular(id -> new BigDecimal(valor));
    }

    private void cuandoSeValidanLasReferencias(CampoFormula formula, Campo... camposDelBloque) {
        rechazo = catchThrowable(() -> formula.validarReferenciasDeFormula(List.of(camposDelBloque)));
    }

    private void entoncesEsUnaFormulaConLaExpresion(Campo campo, String expresion) {
        assertThat(campo.esFormula()).isTrue();
        assertThat(campo.getFormula()).isEqualTo(expresion);
    }

    private void entoncesElCalculoEs(String esperado) {
        assertThat(calculo).isEqualByComparingTo(esperado);
    }

    private void entoncesNoSeRechazaNada() {
        assertThat(rechazo).isNull();
    }

    private void entoncesSeRechazaLaFormula() {
        assertThat(rechazo).isInstanceOf(CampoInvalidoException.class);
    }
}
