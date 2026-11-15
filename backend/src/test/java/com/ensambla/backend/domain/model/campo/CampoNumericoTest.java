package com.ensambla.backend.domain.model.campo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

abstract class CampoNumericoTest extends CampoPorTipoTest {

    @Override
    protected List<Object> valoresValidos() {
        return List.of(12, 12.5);
    }

    @Override
    protected List<Object> valoresInvalidos() {
        return List.of("12", Double.POSITIVE_INFINITY, Double.NaN);
    }

    @Override
    protected String mensajeDeError() {
        return "Tiene que ser un número";
    }

    @Test
    @DisplayName("Es un campo numérico")
    void esNumericoShouldBeTrue() {
        assertThat(campo().esNumerico()).isTrue();
    }

    @Test
    @DisplayName("Entiende un número escrito con coma o con espacios")
    void tiparShouldReadTheNumberWrittenByThePerson() {
        assertThat(campo().tipar("1 200,5")).isEqualTo(new BigDecimal("1200.5"));
    }

    @Test
    @DisplayName("Deja como texto lo que no es un número")
    void tiparShouldKeepTheTextWhenItIsNotANumber() {
        assertThat(campo().tipar("mucho")).isEqualTo("mucho");
    }

    @Test
    @DisplayName("En el reporte va como número")
    void celdaDeReporteShouldBeANumber() {
        assertThat(campo().celdaDeReporte("12.50", id -> "otra fila")).isEqualTo(new BigDecimal("12.50"));
    }
}
