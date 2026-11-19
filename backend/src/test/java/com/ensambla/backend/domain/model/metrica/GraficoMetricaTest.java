package com.ensambla.backend.domain.model.metrica;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.domain.model.metrica.GraficoMetrica.BARRAS;
import static com.ensambla.backend.domain.model.metrica.GraficoMetrica.DONA;
import static com.ensambla.backend.domain.model.metrica.GraficoMetrica.LINEA;
import static com.ensambla.backend.domain.model.metrica.GraficoMetrica.NUMERO;
import static org.assertj.core.api.Assertions.assertThat;

class GraficoMetricaTest {

    private List<GraficoMetrica> posibles;

    @Test
    @DisplayName("Una métrica sin repartir sólo se muestra como un número")
    void unaMetricaSinRepartirSoloSeMuestraComoNumero() {
        cuandoSeRepartePor(AgrupacionMetrica.NINGUNA);

        entoncesSePuedeVerComo(List.of(NUMERO));
    }

    @Test
    @DisplayName("Una métrica por producto se puede ver como número, barras o dona, pero no como línea")
    void unaMetricaPorProductoNoSeVeComoLinea() {
        cuandoSeRepartePor(AgrupacionMetrica.PRODUCTO);

        entoncesSePuedeVerComo(List.of(NUMERO, BARRAS, DONA));
    }

    @Test
    @DisplayName("Una métrica por día de la semana también se puede ver como línea")
    void unaMetricaPorDiaTambienSeVeComoLinea() {
        cuandoSeRepartePor(AgrupacionMetrica.DIA_DE_LA_SEMANA);

        entoncesSePuedeVerComo(List.of(NUMERO, BARRAS, DONA, LINEA));
    }

    @Test
    @DisplayName("Sin elegir, una métrica sin repartir se ve como número y una repartida, como barras")
    void sinElegirSeVeComoNumeroOBarras() {
        entoncesDeSiempreSeVenComo(NUMERO, BARRAS);
    }

    private void cuandoSeRepartePor(AgrupacionMetrica agrupacion) {
        posibles = GraficoMetrica.posiblesPara(agrupacion);
    }

    private void entoncesSePuedeVerComo(List<GraficoMetrica> esperados) {
        assertThat(posibles).isEqualTo(esperados);
    }

    private void entoncesDeSiempreSeVenComo(GraficoMetrica sinRepartir, GraficoMetrica repartida) {
        assertThat(AgrupacionMetrica.NINGUNA.graficoDeSiempre()).isEqualTo(sinRepartir);
        assertThat(AgrupacionMetrica.CLIENTE.graficoDeSiempre()).isEqualTo(repartida);
    }
}
