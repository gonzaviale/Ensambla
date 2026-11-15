package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.ConversacionObjetivoTestData.catalogo;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.consulta;
import static org.assertj.core.api.Assertions.assertThat;

class ConsultaObjetivoIaTest {

    @Test
    @DisplayName("El código de cada métrica es su lugar en la lista, empezando por c1")
    void codigoShouldBeThePositionInTheList() {
        assertThat(ConsultaObjetivoIa.codigo(0)).isEqualTo("c1");
        assertThat(ConsultaObjetivoIa.codigo(3)).isEqualTo("c4");
    }

    @Test
    @DisplayName("Encuentra la métrica del catálogo por su código")
    void candidatoShouldFindTheMetricByCode() {
        assertThat(consulta().candidato("c1")).contains(catalogo().get(0));
        assertThat(consulta().candidato("c3")).contains(catalogo().get(2));
    }

    @Test
    @DisplayName("Un código que no está en el catálogo no devuelve nada")
    void candidatoShouldBeEmptyForAnUnknownCode() {
        assertThat(consulta().candidato("c9")).isEmpty();
        assertThat(consulta().candidato(null)).isEmpty();
    }
}
