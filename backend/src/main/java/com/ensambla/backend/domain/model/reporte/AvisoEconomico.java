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
 * @param cambios                   lo mismo, con cuánto cambió cada uno
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
        List<Cambio> cambios,
        Nivel confianza) {

    static final int MAXIMO_PRODUCTOS = 3;

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final String PEDIDO_DE_COSTOS =
            " Cargá el costo de tus productos para saber también cuánto ganaste.";
    private static final long CONFIANZA_ALTA = 80;
    private static final long CONFIANZA_MEDIA = 50;

    /**
     * Cuánto cambió un producto frente a la semana anterior, en pesos: su ganancia o, si no tiene costo cargado, lo que
     * vendió. Negativo si bajó.
     */
    public record Cambio(String nombre, BigDecimal monto) {
    }

    public enum Nivel {
        ALTA, MEDIA, BAJA
    }

    public enum Tendencia {
        MEJORA(1), EMPEORA(-1);

        private final int signo;

        Tendencia(int signo) {
            this.signo = signo;
        }
    }

    /** Cuánto cambiaron las ventas y la ganancia frente a la semana anterior; la ganancia es null si no hay costos. */
    private record Variaciones(BigDecimal ventas, BigDecimal resultado) {

        boolean vendioMas() {
            return ventas.signum() >= 0;
        }

        boolean ganoMas() {
            return resultado == null ? vendioMas() : resultado.signum() >= 0;
        }

        Tendencia tendencia() {
            return ganoMas() ? Tendencia.MEJORA : Tendencia.EMPEORA;
        }

        boolean tieneCostos() {
            return resultado != null;
        }
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
        Variaciones variaciones = new Variaciones(
                variacion(actual.ventas().ingresos(), ventasAnteriores),
                variacionDeGanancia(actual.ventas().gananciaEstimada(), anterior.ventas().gananciaEstimada()));
        return armar(variaciones, actual.masVendidos(), anterior.masVendidos());
    }

    private static AvisoEconomico armar(Variaciones variaciones, List<ProductoVendido> actuales,
                                        List<ProductoVendido> anteriores) {
        List<Cambio> cambios = cambios(actuales, anteriores, variaciones.tendencia());
        return new AvisoEconomico(prioridad(variaciones), variaciones.tendencia(), categoria(variaciones),
                titulo(variaciones), explicacion(variaciones), variaciones.ventas(), variaciones.resultado(),
                cambios.stream().map(Cambio::nombre).toList(), cambios, confianza(actuales));
    }

    private static BigDecimal variacion(BigDecimal actual, BigDecimal anterior) {
        return actual.subtract(anterior).multiply(CIEN).divide(anterior, 0, RoundingMode.HALF_UP);
    }

    private static BigDecimal variacionDeGanancia(BigDecimal actual, BigDecimal anterior) {
        return actual == null || anterior == null || anterior.signum() <= 0 ? null : variacion(actual, anterior);
    }

    private static String categoria(Variaciones variaciones) {
        return variaciones.tieneCostos() ? "Rentabilidad" : "Ventas";
    }

    /** Lo urgente es vender más y ganar menos: se trabaja más para llevarse menos. */
    private static Nivel prioridad(Variaciones variaciones) {
        if (variaciones.ganoMas()) {
            return Nivel.BAJA;
        }
        return variaciones.vendioMas() ? Nivel.ALTA : Nivel.MEDIA;
    }

    private static String titulo(Variaciones variaciones) {
        String ventas = "Las ventas " + (variaciones.vendioMas() ? "subieron " : "bajaron ")
                + variaciones.ventas().abs() + " %";
        if (!variaciones.tieneCostos()) {
            return ventas + " frente a la semana pasada";
        }
        String ganancia = "la ganancia " + (variaciones.ganoMas() ? "subió " : "bajó ")
                + variaciones.resultado().abs() + " %";
        return ventas + (variaciones.vendioMas() == variaciones.ganoMas() ? " y " : ", pero ") + ganancia;
    }

    private static String explicacion(Variaciones variaciones) {
        if (!variaciones.tieneCostos()) {
            return explicacionSinCostos(variaciones) + PEDIDO_DE_COSTOS;
        }
        return explicacionConCostos(variaciones);
    }

    private static String explicacionSinCostos(Variaciones variaciones) {
        return variaciones.ganoMas()
                ? "Tu negocio está mejorando: vendiste más que la semana pasada."
                : "Esta semana vendiste menos que la anterior.";
    }

    private static String explicacionConCostos(Variaciones variaciones) {
        if (variaciones.ganoMas()) {
            return variaciones.vendioMas()
                    ? "Tu negocio está mejorando: vendiste más y te quedó más ganancia que la semana pasada."
                    : "Vendiste menos, pero te quedó más ganancia: lo que vendiste te dejó más por cada venta.";
        }
        return variaciones.vendioMas()
                ? "Vendiste más, pero te quedó menos ganancia: revisá los precios de los productos involucrados."
                : "Esta semana vendiste menos y te quedó menos ganancia que la anterior.";
    }

    /**
     * Los productos cuya ganancia (o venta, si no tienen costo) más cambió en la dirección del aviso: los que más bajaron
     * cuando empeora, los que más subieron cuando mejora.
     */
    private static List<Cambio> cambios(List<ProductoVendido> actual, List<ProductoVendido> anterior,
                                        Tendencia tendencia) {
        Comparator<Map.Entry<String, BigDecimal>> porCambio =
                Comparator.comparing(entrada -> entrada.getValue().abs());
        return cambioPorProducto(actual, anterior).entrySet().stream()
                .filter(entrada -> entrada.getValue().signum() == tendencia.signo)
                .sorted(porCambio.reversed().thenComparing(Map.Entry::getKey))
                .limit(MAXIMO_PRODUCTOS)
                .map(entrada -> new Cambio(entrada.getKey(), entrada.getValue()))
                .toList();
    }

    private static Map<String, BigDecimal> cambioPorProducto(List<ProductoVendido> actual,
                                                             List<ProductoVendido> anterior) {
        Map<String, BigDecimal> cambio = new HashMap<>();
        actual.forEach(producto -> cambio.merge(producto.nombre(), aporte(producto), BigDecimal::add));
        anterior.forEach(producto -> cambio.merge(producto.nombre(), aporte(producto).negate(), BigDecimal::add));
        return cambio;
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
