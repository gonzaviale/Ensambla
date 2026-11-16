package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static com.ensambla.backend.support.ConversacionObjetivoTestData.catalogo;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.consulta;
import static org.assertj.core.api.Assertions.assertThat;

class ConsultaObjetivoIaTest {

    private String codigo;
    private Optional<CandidatoMiniObjetivo> candidato;

    @ParameterizedTest(name = "posición {0}")
    @DisplayName("El código de cada métrica es su lugar en la lista, empezando por c1")
    @CsvSource({"0, c1", "3, c4"})
    void codigoShouldBeThePositionInTheList(int posicion, String esperado) {
        whenSePideElCodigoDe(posicion);

        thenElCodigoEs(esperado);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Encuentra la métrica del catálogo por su código")
    @CsvSource({"c1, 0", "c3, 2"})
    void candidatoShouldFindTheMetricByCode(String codigoBuscado, int posicion) {
        whenSeBuscaElCandidato(codigoBuscado);

        thenElCandidatoEsElDelCatalogoEn(posicion);
    }

    @Test
    @DisplayName("Un código que no está en el catálogo no devuelve nada")
    void candidatoShouldBeEmptyForAnUnknownCode() {
        whenSeBuscaElCandidato("c9");

        thenNoHayCandidato();
    }

    @Test
    @DisplayName("Un código nulo no devuelve nada")
    void candidatoShouldBeEmptyForANullCode() {
        whenSeBuscaElCandidato(null);

        thenNoHayCandidato();
    }

    // --- act ---
    private void whenSePideElCodigoDe(int posicion) {
        codigo = ConsultaObjetivoIa.codigo(posicion);
    }

    private void whenSeBuscaElCandidato(String codigoBuscado) {
        candidato = consulta().candidato(codigoBuscado);
    }

    // --- assert ---
    private void thenElCodigoEs(String esperado) {
        assertThat(codigo).isEqualTo(esperado);
    }

    private void thenElCandidatoEsElDelCatalogoEn(int posicion) {
        assertThat(candidato).contains(catalogo().get(posicion));
    }

    private void thenNoHayCandidato() {
        assertThat(candidato).isEmpty();
    }
}
