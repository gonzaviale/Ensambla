package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.MetricaTestData.metrica;
import static com.ensambla.backend.support.MetricasDePrueba.lasVentasDelMes;
import static com.ensambla.backend.support.MetricasDePrueba.unaReceta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class MetricaGraficoTest {

    private static final DefinicionMetrica LAS_VENTAS_POR_DIA = unaReceta("Ventas por día", FuenteMetrica.VENTAS,
            CalculoMetrica.SUMA, AgrupacionMetrica.DIA_DE_LA_SEMANA, PeriodoMetrica.MES, false);

    private Metrica laMetrica;

    private Throwable rechazo;

    @Test
    @DisplayName("Sin elegir, una métrica repartida por día se ve como barras")
    void sinElegirSeVeComoLasBarras() {
        dadaLaMetrica(LAS_VENTAS_POR_DIA);

        entoncesSeVeComo(GraficoMetrica.BARRAS);
    }

    @Test
    @DisplayName("Se ve con el gráfico que eligió la persona")
    void seVeConElGraficoElegido() {
        dadaLaMetrica(LAS_VENTAS_POR_DIA);

        cuandoSeEligeVerlaComo(GraficoMetrica.LINEA);

        entoncesSeVeComo(GraficoMetrica.LINEA);
    }

    @Test
    @DisplayName("Un número solo no se puede ver como dona: se rechaza y sigue como número")
    void unNumeroSoloNoSePuedeVerComoDona() {
        dadaLaMetrica(lasVentasDelMes());

        cuandoSeIntentaVerlaComo(GraficoMetrica.DONA);

        entoncesSeRechazaYSigueComo(GraficoMetrica.NUMERO);
    }

    private void dadaLaMetrica(DefinicionMetrica receta) {
        laMetrica = metrica(receta, 0);
    }

    private void cuandoSeEligeVerlaComo(GraficoMetrica grafico) {
        laMetrica.cambiarGrafico(grafico);
    }

    private void cuandoSeIntentaVerlaComo(GraficoMetrica grafico) {
        rechazo = catchThrowable(() -> cuandoSeEligeVerlaComo(grafico));
    }

    private void entoncesSeVeComo(GraficoMetrica grafico) {
        assertThat(laMetrica.getGrafico()).isEqualTo(grafico);
    }

    private void entoncesSeRechazaYSigueComo(GraficoMetrica grafico) {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessageContaining("Ese gráfico no sirve");
        entoncesSeVeComo(grafico);
    }
}
