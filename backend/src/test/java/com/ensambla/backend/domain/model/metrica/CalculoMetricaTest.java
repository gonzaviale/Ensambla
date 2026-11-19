package com.ensambla.backend.domain.model.metrica;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalculoMetricaTest {

    @Test
    @DisplayName("Sumar da pesos y usa el importe de cada fila")
    void sumarDaPesosYUsaElImporte() {
        entoncesElCalculo(CalculoMetrica.SUMA, FormatoMetrica.MONEDA, true);
    }

    @Test
    @DisplayName("Promediar da pesos y usa el importe de cada fila")
    void promediarDaPesosYUsaElImporte() {
        entoncesElCalculo(CalculoMetrica.PROMEDIO, FormatoMetrica.MONEDA, true);
    }

    @Test
    @DisplayName("Contar da una cantidad y no necesita el importe")
    void contarDaUnaCantidadSinImporte() {
        entoncesElCalculo(CalculoMetrica.CONTEO, FormatoMetrica.NUMERO, false);
    }

    @Test
    @DisplayName("Las unidades dan una cantidad y no necesitan el importe")
    void lasUnidadesDanUnaCantidadSinImporte() {
        entoncesElCalculo(CalculoMetrica.UNIDADES, FormatoMetrica.NUMERO, false);
    }

    @Test
    @DisplayName("Las unidades sirven para las ventas y el inventario")
    void lasUnidadesSirvenParaVentasEInventario() {
        entoncesSirvePara(CalculoMetrica.UNIDADES, FuenteMetrica.VENTAS, FuenteMetrica.INVENTARIO);
    }

    @Test
    @DisplayName("Las unidades no sirven para los gastos")
    void lasUnidadesNoSirvenParaLosGastos() {
        entoncesNoSirvePara(CalculoMetrica.UNIDADES, FuenteMetrica.GASTOS);
    }

    @Test
    @DisplayName("Sumar sirve para cualquier tabla")
    void sumarSirveParaCualquierTabla() {
        entoncesSirvePara(CalculoMetrica.SUMA, FuenteMetrica.values());
    }

    private static void entoncesElCalculo(CalculoMetrica calculo, FormatoMetrica formato, boolean usaImporte) {
        assertThat(calculo.formato()).isEqualTo(formato);
        assertThat(calculo.usaImporte()).isEqualTo(usaImporte);
    }

    private static void entoncesSirvePara(CalculoMetrica calculo, FuenteMetrica... fuentes) {
        assertThat(fuentes).allMatch(calculo::sirvePara);
    }

    private static void entoncesNoSirvePara(CalculoMetrica calculo, FuenteMetrica fuente) {
        assertThat(calculo.sirvePara(fuente)).isFalse();
    }
}
