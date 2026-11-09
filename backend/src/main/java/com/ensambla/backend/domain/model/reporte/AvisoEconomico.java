package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.model.reporte.ResumenNegocio.ProductoVendido;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Cómo le fue al negocio esta semana frente a la anterior: si vendió más o menos, si ganó más o menos y qué productos
 * explican la mayor parte del cambio. Siempre hay un aviso cuando hay dos semanas para comparar: también cuando el
 * negocio mejora. Es una comparación de números, no una causa: la ganancia se calcula con el costo actual de cada
 * producto, porque el costo todavía no tiene historial.
 *
 * @param variacionVentas           cuánto cambiaron las ventas frente a la semana anterior, en %
 * @param variacionResultadoBruto   cuánto cambió la ganancia (ventas menos costo), en %; {@code null} si alguna de las
 *                                  dos semanas no tiene costos cargados o la semana anterior no dejó ganancia
 * @param productos                 los productos que más explican el cambio, hasta {@value #MAXIMO_PRODUCTOS}
 */
public record AvisoEconomico(
        Nivel prioridad,
        Tendencia tendencia,
        String categoria,
        String titulo,
        String explicacion,
        BigDecimal variacionVentas,
        BigDecimal variacionResultadoBruto,
        List<String> productos,
        Nivel confianza) {

    static final int MAXIMO_PRODUCTOS = 3;

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final long CONFIANZA_ALTA = 80;
    private static final long CONFIANZA_MEDIA = 50;

    public enum Nivel {
        ALTA, MEDIA, BAJA
    }

    public enum Tendencia {
        MEJORA, EMPEORA
    }

    /**
     * Compara la semana actual con la anterior.
     *
     * @return {@code null} si la semana anterior no tiene ventas: todavía no hay con qué comparar
     */
    public static AvisoEconomico comparar(ResumenNegocio actual, ResumenNegocio anterior) {
        BigDecimal ventasAnteriores = anterior.ventas().ingresos();
        if (ventasAnteriores.signum() <= 0) {
            return null;
        }
        BigDecimal ventas = variacion(actual.ventas().ingresos(), ventasAnteriores);
        BigDecimal resultado = variacionDeGanancia(actual.ventas().gananciaEstimada(),
                anterior.ventas().gananciaEstimada());
        boolean vendioMas = ventas.signum() >= 0;
        boolean ganoMas = resultado == null ? vendioMas : resultado.signum() >= 0;
        Tendencia tendencia = ganoMas ? Tendencia.MEJORA : Tendencia.EMPEORA;
        return new AvisoEconomico(
                prioridad(vendioMas, ganoMas),
                tendencia,
                resultado == null ? "Ventas" : "Rentabilidad",
                titulo(ventas, resultado, vendioMas, ganoMas),
                explicacion(resultado, vendioMas, ganoMas),
                ventas,
                resultado,
                productos(actual.masVendidos(), anterior.masVendidos(), tendencia),
                confianza(actual.masVendidos()));
    }

    private static BigDecimal variacion(BigDecimal actual, BigDecimal anterior) {
        return actual.subtract(anterior).multiply(CIEN).divide(anterior, 0, RoundingMode.HALF_UP);
    }

    private static BigDecimal variacionDeGanancia(BigDecimal actual, BigDecimal anterior) {
        return actual == null || anterior == null || anterior.signum() <= 0 ? null : variacion(actual, anterior);
    }

    /** Lo urgente es vender más y ganar menos: se trabaja más para llevarse menos. */
    private static Nivel prioridad(boolean vendioMas, boolean ganoMas) {
        if (ganoMas) {
            return Nivel.BAJA;
        }
        return vendioMas ? Nivel.ALTA : Nivel.MEDIA;
    }

    private static String titulo(BigDecimal ventas, BigDecimal resultado, boolean vendioMas, boolean ganoMas) {
        String ventasEnTexto = "Las ventas " + (vendioMas ? "subieron " : "bajaron ") + ventas.abs() + " %";
        if (resultado == null) {
            return ventasEnTexto + " frente a la semana pasada";
        }
        String resultadoEnTexto = "la ganancia " + (ganoMas ? "subió " : "bajó ") + resultado.abs() + " %";
        return ventasEnTexto + (vendioMas == ganoMas ? " y " : ", pero ") + resultadoEnTexto;
    }

    private static String explicacion(BigDecimal resultado, boolean vendioMas, boolean ganoMas) {
        if (resultado == null) {
            String base = ganoMas
                    ? "Tu negocio está mejorando: vendiste más que la semana pasada."
                    : "Esta semana vendiste menos que la anterior.";
            return base + " Cargá el costo de tus productos para saber también cuánto ganaste.";
        }
        if (ganoMas) {
            return vendioMas
                    ? "Tu negocio está mejorando: vendiste más y te quedó más ganancia que la semana pasada."
                    : "Vendiste menos, pero te quedó más ganancia: lo que vendiste te dejó más por cada venta.";
        }
        return vendioMas
                ? "Vendiste más, pero te quedó menos ganancia: revisá los precios de los productos de abajo."
                : "Esta semana vendiste menos y te quedó menos ganancia que la anterior.";
    }

    /**
     * Los productos cuya ganancia (o venta, si no tienen costo) más cambió en la dirección del aviso: los que más bajaron
     * cuando empeora, los que más subieron cuando mejora.
     */
    private static List<String> productos(List<ProductoVendido> actual, List<ProductoVendido> anterior,
                                          Tendencia tendencia) {
        Map<String, BigDecimal> cambio = new HashMap<>();
        actual.forEach(producto -> cambio.merge(producto.nombre(), aporte(producto), BigDecimal::add));
        anterior.forEach(producto -> cambio.merge(producto.nombre(), aporte(producto).negate(), BigDecimal::add));
        int signo = tendencia == Tendencia.MEJORA ? 1 : -1;
        Comparator<Map.Entry<String, BigDecimal>> porCambio =
                Comparator.comparing(entrada -> entrada.getValue().abs());
        return cambio.entrySet().stream()
                .filter(entrada -> entrada.getValue().signum() == signo)
                .sorted(porCambio.reversed().thenComparing(Map.Entry::getKey))
                .limit(MAXIMO_PRODUCTOS)
                .map(Map.Entry::getKey)
                .toList();
    }

    private static BigDecimal aporte(ProductoVendido producto) {
        return producto.ganancia() == null ? producto.ingresos() : producto.ganancia();
    }

    /** Cuánto se puede creer en la ganancia: depende de cuántos de los productos vendidos tienen costo cargado. */
    private static Nivel confianza(List<ProductoVendido> vendidos) {
        if (vendidos.isEmpty()) {
            return Nivel.BAJA;
        }
        long conCosto = vendidos.stream().map(ProductoVendido::ganancia).filter(Objects::nonNull).count();
        long porcentaje = conCosto * 100 / vendidos.size();
        if (porcentaje >= CONFIANZA_ALTA) {
            return Nivel.ALTA;
        }
        return porcentaje >= CONFIANZA_MEDIA ? Nivel.MEDIA : Nivel.BAJA;
    }
}
