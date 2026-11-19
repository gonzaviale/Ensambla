package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.MetricasDePrueba.cuantasVentasHuboEnElMes;
import static com.ensambla.backend.support.MetricasDePrueba.elInventarioDeLaSemana;
import static com.ensambla.backend.support.MetricasDePrueba.elStockPorProducto;
import static com.ensambla.backend.support.MetricasDePrueba.lasUnidadesDeLosGastos;
import static com.ensambla.backend.support.MetricasDePrueba.lasVentasDeLaSemana;
import static com.ensambla.backend.support.MetricasDePrueba.lasVentasDelMes;
import static com.ensambla.backend.support.MetricasDePrueba.lasVentasDelMesLlamadas;
import static com.ensambla.backend.support.MetricasDePrueba.lasVentasFiadasDelMes;
import static com.ensambla.backend.support.MetricasDePrueba.lasVentasPorCategoria;
import static com.ensambla.backend.support.MetricasDePrueba.lasVentasPorCliente;
import static com.ensambla.backend.support.MetricasDePrueba.losGastosDelMes;
import static com.ensambla.backend.support.MetricasDePrueba.losGastosFiados;
import static com.ensambla.backend.support.MetricasDePrueba.losGastosPorCategoria;
import static com.ensambla.backend.support.MetricasDePrueba.loQueTeDebenPorCliente;
import static com.ensambla.backend.support.MetricasDePrueba.unaReceta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class DefinicionMetricaTest {

    private static final String NOMBRE_DEMASIADO_LARGO = "a".repeat(DefinicionMetrica.LARGO_MAXIMO_DEL_NOMBRE + 1);

    private DefinicionMetrica receta;

    private Throwable rechazo;

    @Test
    @DisplayName("Acepta una receta válida")
    void aceptaUnaRecetaValida() {
        dadaLaReceta(loQueTeDebenPorCliente());

        cuandoSeValida();

        entoncesEsValida();
    }

    @Test
    @DisplayName("Acepta el inventario de todo lo cargado repartido por producto")
    void aceptaElInventarioDeTodoLoCargadoPorProducto() {
        dadaLaReceta(elStockPorProducto());

        cuandoSeValida();

        entoncesEsValida();
    }

    @Test
    @DisplayName("Acepta los gastos repartidos por categoría")
    void aceptaLosGastosPorCategoria() {
        dadaLaReceta(losGastosPorCategoria());

        cuandoSeValida();

        entoncesEsValida();
    }

    @Test
    @DisplayName("Sin reparto ni período elige no repartir y mirar el mes, y recorta el nombre")
    void sinRepartoNiPeriodoNoReparteYMiraElMes() {
        dadaLaReceta(unaReceta("  Ventas ", FuenteMetrica.VENTAS, CalculoMetrica.SUMA, null, null, false));

        entoncesLaRecetaQuedaComo(lasVentasDelMesLlamadas("Ventas"));
    }

    @Test
    @DisplayName("Sin nombre la receta no es válida")
    void sinNombreLaRecetaNoEsValida() {
        dadaLaReceta(lasVentasDelMesLlamadas(null));

        cuandoSeValida();

        entoncesSeRechazaPorque("nombre");
    }

    @Test
    @DisplayName("Un nombre muy largo no es válido")
    void unNombreMuyLargoNoEsValido() {
        dadaLaReceta(lasVentasDelMesLlamadas(NOMBRE_DEMASIADO_LARGO));

        cuandoSeValida();

        entoncesSeRechazaPorque("nombre");
    }

    @Test
    @DisplayName("Sin tabla la receta no es válida")
    void sinTablaLaRecetaNoEsValida() {
        dadaLaReceta(unaReceta("Algo", null, CalculoMetrica.SUMA, AgrupacionMetrica.NINGUNA, PeriodoMetrica.MES,
                false));

        cuandoSeValida();

        entoncesSeRechazaPorque("de qué tabla");
    }

    @Test
    @DisplayName("Sin cálculo la receta no es válida")
    void sinCalculoLaRecetaNoEsValida() {
        dadaLaReceta(unaReceta("Algo", FuenteMetrica.VENTAS, null, AgrupacionMetrica.NINGUNA, PeriodoMetrica.MES,
                false));

        cuandoSeValida();

        entoncesSeRechazaPorque("de qué tabla");
    }

    @Test
    @DisplayName("Los gastos no tienen unidades")
    void losGastosNoTienenUnidades() {
        dadaLaReceta(lasUnidadesDeLosGastos());

        cuandoSeValida();

        entoncesSeRechazaPorque("unidades");
    }

    @Test
    @DisplayName("No se pueden repartir las ventas por categoría")
    void noSePuedenRepartirLasVentasPorCategoria() {
        dadaLaReceta(lasVentasPorCategoria());

        cuandoSeValida();

        entoncesSeRechazaPorque("repartir ventas");
    }

    @Test
    @DisplayName("Sólo las ventas pueden ser fiadas")
    void soloLasVentasPuedenSerFiadas() {
        dadaLaReceta(losGastosFiados());

        cuandoSeValida();

        entoncesSeRechazaPorque("fiadas");
    }

    @Test
    @DisplayName("El inventario no tiene períodos")
    void elInventarioNoTienePeriodos() {
        dadaLaReceta(elInventarioDeLaSemana());

        cuandoSeValida();

        entoncesSeRechazaPorque("períodos");
    }

    @Test
    @DisplayName("Sumar da pesos")
    void sumarDaPesos() {
        dadaLaReceta(lasVentasDelMes());

        entoncesSeMuestraEn(FormatoMetrica.MONEDA);
    }

    @Test
    @DisplayName("Contar da una cantidad")
    void contarDaUnaCantidad() {
        dadaLaReceta(cuantasVentasHuboEnElMes());

        entoncesSeMuestraEn(FormatoMetrica.NUMERO);
    }

    @Test
    @DisplayName("Dos recetas son la misma aunque se llamen distinto")
    void dosRecetasSonLaMismaAunqueSeLlamenDistinto() {
        dadaLaReceta(lasVentasDelMesLlamadas("Una"));

        entoncesPideLoMismoQue(lasVentasDelMesLlamadas("Otra"));
    }

    @Test
    @DisplayName("Dos recetas de tablas distintas no son la misma")
    void dosRecetasDeTablasDistintasNoSonLaMisma() {
        dadaLaReceta(lasVentasDelMes());

        entoncesNoPideLoMismoQue(losGastosDelMes());
    }

    @Test
    @DisplayName("Dos recetas con cálculos distintos no son la misma")
    void dosRecetasConCalculosDistintosNoSonLaMisma() {
        dadaLaReceta(lasVentasDelMes());

        entoncesNoPideLoMismoQue(cuantasVentasHuboEnElMes());
    }

    @Test
    @DisplayName("Dos recetas con repartos distintos no son la misma")
    void dosRecetasConRepartosDistintosNoSonLaMisma() {
        dadaLaReceta(lasVentasDelMes());

        entoncesNoPideLoMismoQue(lasVentasPorCliente());
    }

    @Test
    @DisplayName("Dos recetas de períodos distintos no son la misma")
    void dosRecetasDePeriodosDistintosNoSonLaMisma() {
        dadaLaReceta(lasVentasDelMes());

        entoncesNoPideLoMismoQue(lasVentasDeLaSemana());
    }

    @Test
    @DisplayName("Las ventas fiadas no son la misma receta que todas las ventas")
    void lasVentasFiadasNoSonLaMismaRecetaQueTodasLasVentas() {
        dadaLaReceta(lasVentasDelMes());

        entoncesNoPideLoMismoQue(lasVentasFiadasDelMes());
    }

    private void dadaLaReceta(DefinicionMetrica definicion) {
        receta = definicion;
    }

    private void cuandoSeValida() {
        rechazo = catchThrowable(receta::validar);
    }

    private void entoncesEsValida() {
        assertThat(rechazo).isNull();
    }

    private void entoncesSeRechazaPorque(String motivo) {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessageContaining(motivo);
    }

    private void entoncesLaRecetaQuedaComo(DefinicionMetrica esperada) {
        assertThat(receta).isEqualTo(esperada);
    }

    private void entoncesSeMuestraEn(FormatoMetrica formato) {
        assertThat(receta.formato()).isEqualTo(formato);
    }

    private void entoncesPideLoMismoQue(DefinicionMetrica otra) {
        assertThat(receta.esIgualA(otra)).isTrue();
    }

    private void entoncesNoPideLoMismoQue(DefinicionMetrica otra) {
        assertThat(receta.esIgualA(otra)).isFalse();
    }
}
