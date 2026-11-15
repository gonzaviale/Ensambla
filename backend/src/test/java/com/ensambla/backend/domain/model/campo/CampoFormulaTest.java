package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.exception.CampoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CampoFormulaTest extends CampoPorTipoTest {

    private static final CampoId PRECIO_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId TOTAL_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");

    @Override
    protected TipoCampo tipoEsperado() {
        return TipoCampo.FORMULA;
    }

    @Override
    protected List<Object> valoresValidos() {
        return List.of();
    }

    @Override
    protected List<Object> valoresInvalidos() {
        return List.of(3, "3");
    }

    @Override
    protected String mensajeDeError() {
        return "Este dato se calcula solo";
    }

    @Test
    @DisplayName("Es una fórmula y conoce su expresión")
    void esFormulaShouldBeTrueAndKnowItsExpression() {
        assertThat(formulaQueDuplica(PRECIO_ID).esFormula()).isTrue();
        assertThat(formulaQueDuplica(PRECIO_ID).getFormula()).isEqualTo("{" + PRECIO_ID + "} * 2");
    }

    @Test
    @DisplayName("Calcula la fórmula con los valores que le da el registro")
    void calcularShouldEvaluateWithTheGivenValues() {
        CampoFormula formula = formulaQueDuplica(PRECIO_ID);

        assertThat(formula.calcular(id -> new BigDecimal("4"))).isEqualByComparingTo("8");
    }

    @Test
    @DisplayName("En el reporte va como número")
    void celdaDeReporteShouldBeANumber() {
        assertThat(campo().celdaDeReporte("8", id -> "otra fila")).isEqualTo(new BigDecimal("8"));
    }

    @Test
    @DisplayName("Acepta una fórmula que usa campos numéricos de la tabla")
    void validarReferenciasShouldAcceptNumericFields() {
        CampoFormula formula = formulaQueDuplica(PRECIO_ID);

        assertThatCode(() -> formula.validarReferenciasDeFormula(List.of(precioNumerico()))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rechaza una fórmula que usa un campo que no es numérico")
    void validarReferenciasShouldRejectNonNumericFields() {
        CampoFormula formula = formulaQueDuplica(PRECIO_ID);
        Campo precioEnTexto = Campo.builder().id(PRECIO_ID).tipo(TipoCampo.TEXTO).build();

        assertThatThrownBy(() -> formula.validarReferenciasDeFormula(List.of(precioEnTexto)))
                .isInstanceOf(CampoInvalidoException.class);
    }

    @Test
    @DisplayName("Rechaza una fórmula que se cita a sí misma")
    void validarReferenciasShouldRejectAFormulaThatQuotesItself() {
        CampoFormula formula = formulaQueDuplica(TOTAL_ID);
        Campo totalNumerico = Campo.builder().id(TOTAL_ID).tipo(TipoCampo.NUMERO).build();

        assertThatThrownBy(() -> formula.validarReferenciasDeFormula(List.of(totalNumerico)))
                .isInstanceOf(CampoInvalidoException.class);
    }

    private CampoFormula formulaQueDuplica(CampoId campoId) {
        return (CampoFormula) Campo.builder()
                .id(TOTAL_ID)
                .tipo(TipoCampo.FORMULA)
                .formula("{" + campoId + "} * 2")
                .build();
    }

    private Campo precioNumerico() {
        return Campo.builder().id(PRECIO_ID).tipo(TipoCampo.NUMERO).build();
    }
}
