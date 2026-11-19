package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.MetricaTestData.metrica;
import static com.ensambla.backend.support.MetricaTestData.unPanelLleno;
import static com.ensambla.backend.support.MetricasDePrueba.cuantasVentasHuboEnElMes;
import static com.ensambla.backend.support.MetricasDePrueba.lasVentasDelMes;
import static com.ensambla.backend.support.MetricasDePrueba.lasVentasDelMesLlamadas;
import static com.ensambla.backend.support.MetricasDePrueba.losGastosDelMes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class MetricasDelCuadernoTest {

    private MetricasDelCuaderno guardadas;

    private Throwable rechazo;

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

        entoncesSeRechazaPorque("Ya tenés esa métrica en el cuaderno");
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

        entoncesNoIncluye(lasVentasDelMes());
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

    private void entoncesNoIncluye(DefinicionMetrica definicion) {
        assertThat(guardadas.incluye(definicion)).isFalse();
    }
}
