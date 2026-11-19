package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.metrica.AgrupacionMetrica;
import com.ensambla.backend.domain.model.metrica.CalculoMetrica;
import com.ensambla.backend.domain.model.metrica.DefinicionMetrica;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.metrica.PeriodoMetrica;

/** Recetas de métricas con nombre de negocio, para que los tests digan qué piden y no cómo se arman. */
public final class MetricasDePrueba {

    private MetricasDePrueba() {
    }

    public static DefinicionMetrica unaReceta(String nombre, FuenteMetrica fuente, CalculoMetrica calculo,
                                              AgrupacionMetrica agrupar, PeriodoMetrica periodo, boolean soloFiado) {
        return new DefinicionMetrica(nombre, fuente, calculo, agrupar, periodo, soloFiado);
    }

    public static DefinicionMetrica lasVentasDelMesLlamadas(String nombre) {
        return unaReceta(nombre, FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.NINGUNA,
                PeriodoMetrica.MES, false);
    }

    public static DefinicionMetrica lasVentasDelMes() {
        return lasVentasDelMesLlamadas("Ventas del mes");
    }

    public static DefinicionMetrica lasVentasDeLaSemana() {
        return unaReceta("Ventas de la semana", FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.NINGUNA,
                PeriodoMetrica.SEMANA, false);
    }

    public static DefinicionMetrica cuantasVentasHuboEnElMes() {
        return unaReceta("Cantidad de ventas", FuenteMetrica.VENTAS, CalculoMetrica.CONTEO,
                AgrupacionMetrica.NINGUNA, PeriodoMetrica.MES, false);
    }

    public static DefinicionMetrica lasVentasPorCliente() {
        return unaReceta("Ventas por cliente", FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.CLIENTE,
                PeriodoMetrica.MES, false);
    }

    public static DefinicionMetrica loQueTeDebenPorCliente() {
        return unaReceta("Lo que te deben", FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.CLIENTE,
                PeriodoMetrica.MES, true);
    }

    public static DefinicionMetrica lasVentasFiadasDelMes() {
        return unaReceta("Fiado del mes", FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.NINGUNA,
                PeriodoMetrica.MES, true);
    }

    public static DefinicionMetrica lasVentasPorCategoria() {
        return unaReceta("Ventas por categoría", FuenteMetrica.VENTAS, CalculoMetrica.SUMA,
                AgrupacionMetrica.CATEGORIA, PeriodoMetrica.MES, false);
    }

    public static DefinicionMetrica losGastosDelMes() {
        return unaReceta("Gastos del mes", FuenteMetrica.GASTOS, CalculoMetrica.SUMA, AgrupacionMetrica.NINGUNA,
                PeriodoMetrica.MES, false);
    }

    public static DefinicionMetrica losGastosPorCategoria() {
        return unaReceta("Gastos por categoría", FuenteMetrica.GASTOS, CalculoMetrica.SUMA,
                AgrupacionMetrica.CATEGORIA, PeriodoMetrica.MES, false);
    }

    public static DefinicionMetrica lasUnidadesDeLosGastos() {
        return unaReceta("Unidades de gastos", FuenteMetrica.GASTOS, CalculoMetrica.UNIDADES,
                AgrupacionMetrica.NINGUNA, PeriodoMetrica.MES, false);
    }

    public static DefinicionMetrica losGastosFiados() {
        return unaReceta("Gastos fiados", FuenteMetrica.GASTOS, CalculoMetrica.SUMA, AgrupacionMetrica.NINGUNA,
                PeriodoMetrica.MES, true);
    }

    public static DefinicionMetrica elInventarioDeLaSemana() {
        return unaReceta("Inventario de la semana", FuenteMetrica.INVENTARIO, CalculoMetrica.SUMA,
                AgrupacionMetrica.NINGUNA, PeriodoMetrica.SEMANA, false);
    }

    public static DefinicionMetrica elStockPorProducto() {
        return unaReceta("Stock", FuenteMetrica.INVENTARIO, CalculoMetrica.UNIDADES, AgrupacionMetrica.PRODUCTO,
                PeriodoMetrica.TODO, false);
    }
}
