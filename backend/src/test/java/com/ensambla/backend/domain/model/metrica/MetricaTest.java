package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.MetricaTestData.AHORA;
import static com.ensambla.backend.support.MetricaTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.MetricaTestData.PROYECTO_ID;
import static com.ensambla.backend.support.MetricasDePrueba.lasVentasDelMes;
import static com.ensambla.backend.support.MetricasDePrueba.lasVentasDelMesLlamadas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class MetricaTest {

    private Metrica metrica;

    private Throwable rechazo;

    @Test
    @DisplayName("Crea la métrica con su receta, su lugar y quién la eligió")
    void creaLaMetricaConSuRecetaSuLugarYQuienLaEligio() {
        cuandoSeCreaEnElLugar(lasVentasDelMes(), 2);

        entoncesLaMetricaEsNuevaYTiene(lasVentasDelMes(), 2);
    }

    @Test
    @DisplayName("No crea una métrica con una receta inválida")
    void noCreaUnaMetricaConUnaRecetaInvalida() {
        cuandoSeIntentaCrear(lasVentasDelMesLlamadas(" "));

        entoncesSeRechaza();
    }

    @Test
    @DisplayName("Renombrarla cambia sólo el nombre, no lo que calcula")
    void renombrarlaCambiaSoloElNombre() {
        dadaUnaMetricaDeLasVentasDelMes();

        cuandoSeRenombra("  Lo vendido ");

        entoncesSeLlamaYCalculaLoMismo("Lo vendido");
    }

    @Test
    @DisplayName("No se puede dejar sin nombre y conserva el anterior")
    void noSePuedeDejarSinNombre() {
        dadaUnaMetricaDeLasVentasDelMes();

        cuandoSeIntentaRenombrar(" ");

        entoncesSeRechaza();
        entoncesSeLlamaYCalculaLoMismo("Ventas del mes");
    }

    private void dadaUnaMetricaDeLasVentasDelMes() {
        cuandoSeCreaEnElLugar(lasVentasDelMes(), 0);
    }

    private void cuandoSeRenombra(String nombre) {
        metrica.renombrar(nombre);
    }

    private void cuandoSeIntentaRenombrar(String nombre) {
        rechazo = catchThrowable(() -> cuandoSeRenombra(nombre));
    }

    private void entoncesSeLlamaYCalculaLoMismo(String nombre) {
        assertThat(metrica.getDefinicion()).isEqualTo(lasVentasDelMesLlamadas(nombre));
    }

    private void cuandoSeCreaEnElLugar(DefinicionMetrica definicion, int orden) {
        metrica = Metrica.nueva(NEGOCIO_ID, PROYECTO_ID, definicion, orden, AutoriaMetrica.SUGERIDA, AHORA);
    }

    private void cuandoSeIntentaCrear(DefinicionMetrica definicion) {
        rechazo = catchThrowable(() -> cuandoSeCreaEnElLugar(definicion, 0));
    }

    private void entoncesLaMetricaEsNuevaYTiene(DefinicionMetrica definicion, int orden) {
        assertThat(metrica.getId()).isNull();
        assertThat(metrica.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(metrica.getProyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(metrica.getDefinicion()).isEqualTo(definicion);
        assertThat(metrica.getOrden()).isEqualTo(orden);
        assertThat(metrica.getAutoria()).isEqualTo(AutoriaMetrica.SUGERIDA);
        assertThat(metrica.getCreadoEn()).isEqualTo(AHORA);
    }

    private void entoncesSeRechaza() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class);
    }
}
