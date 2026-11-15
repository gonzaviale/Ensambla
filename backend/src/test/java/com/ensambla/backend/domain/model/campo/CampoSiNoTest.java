package com.ensambla.backend.domain.model.campo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CampoSiNoTest extends CampoPorTipoTest {

    @Override
    protected TipoCampo tipoEsperado() {
        return TipoCampo.SI_NO;
    }

    @Override
    protected List<Object> valoresValidos() {
        return List.of(true, false);
    }

    @Override
    protected List<Object> valoresInvalidos() {
        return List.of("sí", 1);
    }

    @Override
    protected String mensajeDeError() {
        return "Tiene que ser sí o no";
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "SI", "sí"})
    @DisplayName("Entiende como sí las formas de decir sí")
    void tiparShouldReadYes(String respuesta) {
        assertThat(campo().tipar(respuesta)).isEqualTo(true);
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "No"})
    @DisplayName("Entiende como no las formas de decir no")
    void tiparShouldReadNo(String respuesta) {
        assertThat(campo().tipar(respuesta)).isEqualTo(false);
    }

    @Test
    @DisplayName("Deja como texto lo que no es ni sí ni no")
    void tiparShouldKeepTheTextWhenItIsNeitherYesNorNo() {
        assertThat(campo().tipar("quizás")).isEqualTo("quizás");
    }

    @Test
    @DisplayName("En el reporte va como Sí o No")
    void celdaDeReporteShouldBeSiOrNo() {
        assertThat(campo().celdaDeReporte(true, id -> "otra fila")).isEqualTo("Sí");
        assertThat(campo().celdaDeReporte(false, id -> "otra fila")).isEqualTo("No");
    }
}
