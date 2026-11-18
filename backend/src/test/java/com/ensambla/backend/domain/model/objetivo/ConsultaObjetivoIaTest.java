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
    void elCodigoDeCadaMetricaEsSuLugarEnLaListaEmpezandoPorC1(int posicion, String esperado) {
        cuandoSePideElCodigoDe(posicion);

        entoncesElCodigoEs(esperado);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Encuentra la métrica del catálogo por su código")
    @CsvSource({"c1, 0", "c3, 2"})
    void encuentraLaMetricaDelCatalogoPorSuCodigo(String codigoBuscado, int posicion) {
        cuandoSeBuscaElCandidato(codigoBuscado);

        entoncesElCandidatoEsElDelCatalogoEn(posicion);
    }

    @Test
    @DisplayName("Un código que no está en el catálogo no devuelve nada")
    void unCodigoQueNoEstaEnElCatalogoNoDevuelveNada() {
        cuandoSeBuscaElCandidato("c9");

        entoncesNoHayCandidato();
    }

    @Test
    @DisplayName("Un código nulo no devuelve nada")
    void unCodigoNuloNoDevuelveNada() {
        cuandoSeBuscaElCandidato(null);

        entoncesNoHayCandidato();
    }

    // --- act ---
    private void cuandoSePideElCodigoDe(int posicion) {
        codigo = ConsultaObjetivoIa.codigo(posicion);
    }

    private void cuandoSeBuscaElCandidato(String codigoBuscado) {
        candidato = consulta().candidato(codigoBuscado);
    }

    // --- assert ---
    private void entoncesElCodigoEs(String esperado) {
        assertThat(codigo).isEqualTo(esperado);
    }

    private void entoncesElCandidatoEsElDelCatalogoEn(int posicion) {
        assertThat(candidato).contains(catalogo().get(posicion));
    }

    private void entoncesNoHayCandidato() {
        assertThat(candidato).isEmpty();
    }
}
