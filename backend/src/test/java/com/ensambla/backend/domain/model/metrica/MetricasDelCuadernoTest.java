package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.MetricaTestData.METRICA_GASTOS_ID;
import static com.ensambla.backend.support.MetricaTestData.METRICA_ID;
import static com.ensambla.backend.support.MetricaTestData.METRICA_STOCK_ID;
import static com.ensambla.backend.support.MetricaTestData.METRICA_VENTAS_ID;
import static com.ensambla.backend.support.MetricaTestData.metrica;
import static com.ensambla.backend.support.MetricaTestData.unPanelDeVentasGastosYStock;
import static com.ensambla.backend.support.MetricaTestData.unPanelLleno;
import static com.ensambla.backend.support.MetricasDePrueba.cuantasVentasHuboEnElMes;
import static com.ensambla.backend.support.MetricasDePrueba.lasVentasDeLaSemana;
import static com.ensambla.backend.support.MetricasDePrueba.lasVentasDelMes;
import static com.ensambla.backend.support.MetricasDePrueba.lasVentasDelMesLlamadas;
import static com.ensambla.backend.support.MetricasDePrueba.losGastosDelMes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class MetricasDelCuadernoTest {

    private MetricasDelCuaderno guardadas;

    private Throwable rechazo;

    private List<Metrica> reordenado;

    @Test
    @DisplayName("Un panel vacío arranca en el lugar cero")
    void unPanelVacioArrancaEnElLugarCero() {
        dadoUnPanelCon();

        entoncesLaQueSigueVaEnElLugar(0);
    }

    @Test
    @DisplayName("La que sigue va después de la última, aunque haya lugares salteados")
    void laQueSigueVaDespuesDeLaUltima() {
        dadoUnPanelCon(metrica(lasVentasDelMes(), 0), metrica(losGastosDelMes(), 2));

        entoncesLaQueSigueVaEnElLugar(3);
    }

    @Test
    @DisplayName("Hay lugar para una receta nueva en un panel que no está lleno")
    void hayLugarParaUnaRecetaNueva() {
        dadoUnPanelCon(metrica(lasVentasDelMes(), 0));

        cuandoSePideLugarPara(losGastosDelMes());

        entoncesHayLugar();
    }

    @Test
    @DisplayName("No hay lugar para una receta que ya está, aunque se llame distinto")
    void noHayLugarParaUnaRecetaQueYaEsta() {
        dadoUnPanelCon(metrica(lasVentasDelMes(), 0));

        cuandoSePideLugarPara(lasVentasDelMesLlamadas("Mis ventas"));

        entoncesSeRechazaPorque(MetricasDelCuaderno.YA_LA_TENES);
    }

    @Test
    @DisplayName("Lo que ya muestra el panel de ventas del inicio cuenta como que ya está, sin guardarlo")
    void loDelPanelDeVentasCuentaComoQueYaEsta() {
        dadoUnPanelSinMetricas();

        entoncesIncluye(lasVentasDeLaSemana());
    }

    @Test
    @DisplayName("No hay lugar en un panel lleno")
    void noHayLugarEnUnPanelLleno() {
        dadoUnPanelCon(unPanelLleno());

        cuandoSePideLugarPara(cuantasVentasHuboEnElMes());

        entoncesSeRechazaPorque("quitá alguna para agregar otra");
    }

    @Test
    @DisplayName("Sin métricas no incluye ninguna receta")
    void sinMetricasNoIncluyeNingunaReceta() {
        dadoUnPanelSinMetricas();

        entoncesNoIncluye(losGastosDelMes());
    }

    @Test
    @DisplayName("Subir la última al primer lugar corre las demás un lugar para abajo")
    void subirLaUltimaAlPrimerLugarCorreLasDemas() {
        dadoUnPanelDeVentasGastosYStock();

        cuandoSeMueve(METRICA_STOCK_ID, 0);

        entoncesElPanelQuedaEnEsteOrden(METRICA_STOCK_ID, METRICA_VENTAS_ID, METRICA_GASTOS_ID);
    }

    @Test
    @DisplayName("Bajar la primera un lugar la cambia con la que estaba abajo")
    void bajarLaPrimeraUnLugarLaCambiaConLaDeAbajo() {
        dadoUnPanelDeVentasGastosYStock();

        cuandoSeMueve(METRICA_VENTAS_ID, 1);

        entoncesElPanelQuedaEnEsteOrden(METRICA_GASTOS_ID, METRICA_VENTAS_ID, METRICA_STOCK_ID);
    }

    @Test
    @DisplayName("Dejarla en su lugar no cambia el panel")
    void dejarlaEnSuLugarNoCambiaElPanel() {
        dadoUnPanelDeVentasGastosYStock();

        cuandoSeMueve(METRICA_GASTOS_ID, 1);

        entoncesElPanelQuedaEnEsteOrden(METRICA_VENTAS_ID, METRICA_GASTOS_ID, METRICA_STOCK_ID);
    }

    @Test
    @DisplayName("No se puede mover a un lugar después del último")
    void noSePuedeMoverDespuesDelUltimo() {
        dadoUnPanelDeVentasGastosYStock();

        cuandoSeIntentaMover(METRICA_VENTAS_ID, 3);

        entoncesSeRechazaPorque("Ese lugar no está en el panel");
    }

    @Test
    @DisplayName("No se puede mover a un lugar negativo")
    void noSePuedeMoverAUnLugarNegativo() {
        dadoUnPanelDeVentasGastosYStock();

        cuandoSeIntentaMover(METRICA_VENTAS_ID, -1);

        entoncesSeRechazaPorque("Ese lugar no está en el panel");
    }

    @Test
    @DisplayName("No se puede mover una métrica que no está en el panel")
    void noSePuedeMoverUnaMetricaQueNoEstaEnElPanel() {
        dadoUnPanelDeVentasGastosYStock();

        cuandoSeIntentaMover(METRICA_ID, 0);

        entoncesNoSeEncuentraLaMetrica();
    }

    private void dadoUnPanelDeVentasGastosYStock() {
        guardadas = new MetricasDelCuaderno(unPanelDeVentasGastosYStock());
    }

    private void cuandoSeMueve(MetricaId metricaId, int lugar) {
        reordenado = guardadas.conLaMetricaEnElLugar(metricaId, lugar);
    }

    private void cuandoSeIntentaMover(MetricaId metricaId, int lugar) {
        rechazo = catchThrowable(() -> cuandoSeMueve(metricaId, lugar));
    }

    private void entoncesElPanelQuedaEnEsteOrden(MetricaId... ids) {
        assertThat(reordenado).extracting(Metrica::getId).containsExactly(ids);
        assertThat(reordenado).extracting(Metrica::getOrden).containsExactly(0, 1, 2);
    }

    private void entoncesNoSeEncuentraLaMetrica() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void dadoUnPanelCon(Metrica... metricas) {
        guardadas = new MetricasDelCuaderno(List.of(metricas));
    }

    private void dadoUnPanelSinMetricas() {
        guardadas = new MetricasDelCuaderno(null);
    }

    private void cuandoSePideLugarPara(DefinicionMetrica definicion) {
        rechazo = catchThrowable(() -> guardadas.exigirLugarPara(definicion));
    }

    private void entoncesLaQueSigueVaEnElLugar(int orden) {
        assertThat(guardadas.siguienteOrden()).isEqualTo(orden);
    }

    private void entoncesHayLugar() {
        assertThat(rechazo).isNull();
    }

    private void entoncesSeRechazaPorque(String motivo) {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessageContaining(motivo);
    }

    private void entoncesIncluye(DefinicionMetrica definicion) {
        assertThat(guardadas.incluye(definicion)).isTrue();
    }

    private void entoncesNoIncluye(DefinicionMetrica definicion) {
        assertThat(guardadas.incluye(definicion)).isFalse();
    }
}
