package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.aviso.MetricaProducto;
import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.objetivo.PasoObjetivo.PrecioZona;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Selecciona hasta tres acciones concretas con productos reales del negocio. */
public final class PasosObjetivo {

    private static final BigDecimal MARGEN_BAJO = new BigDecimal("0.30");
    private static final BigDecimal PRECIO_PASO_GRANDE = new BigDecimal("1500");
    private static final BigDecimal PASO_GRANDE = new BigDecimal("50");
    private static final BigDecimal PASO_CHICO = new BigDecimal("10");

    private PasosObjetivo() {
    }

    public static List<PasoObjetivo> elegir(
            List<DatosDeProyecto> datos,
            MetricasVentas metricasVentas,
            LocalDate hoy,
            AnalisisZona zona) {
        List<BloqueVentas> ventas = datos.stream().flatMap(dato -> dato.ventas().stream()).toList();
        Map<RegistroId, MetricaProducto> metricas = metricasVentas.calcular(ventas, hoy);
        List<Producto> productos = productosDe(datos, metricas);
        List<PasoObjetivo> pasos = new ArrayList<>();
        Set<RegistroId> usados = new HashSet<>();

        productos.stream()
                .filter(Producto::tieneVentas)
                .filter(Producto::tieneMargenBajo)
                .min(Comparator.comparing(Producto::vendidas).reversed().thenComparing(Producto::orden))
                .ifPresent(producto -> agregar(pasos, usados, TipoPasoObjetivo.SUBIR_PRECIO, producto, zona));

        productos.stream()
                .filter(Producto::estaParado)
                .filter(Producto::admiteRebaja)
                .min(Comparator.comparing(Producto::capital).reversed().thenComparing(Producto::orden))
                .ifPresent(producto -> agregar(pasos, usados, TipoPasoObjetivo.MOVER_PARADO, producto, zona));

        productos.stream()
                .filter(Producto::tieneVentas)
                .filter(producto -> !usados.contains(producto.id()))
                .min(Comparator.comparing(Producto::ganancia).reversed().thenComparing(Producto::orden))
                .ifPresent(producto -> agregar(pasos, usados, TipoPasoObjetivo.PONER_A_LA_VISTA, producto, zona));

        return List.copyOf(pasos);
    }

    private static List<Producto> productosDe(
            List<DatosDeProyecto> datos,
            Map<RegistroId, MetricaProducto> metricas) {
        Map<RegistroId, Producto> productos = new LinkedHashMap<>();
        datos.stream()
                .flatMap(dato -> dato.inventarios().stream())
                .forEach(inventario -> inventario.registros().stream()
                        .map(registro -> productoDe(inventario, registro, metricas.get(registro.getId())))
                        .filter(Objects::nonNull)
                        .forEach(producto -> productos.put(producto.id(), producto)));
        return List.copyOf(productos.values());
    }

    private static Producto productoDe(
            BloqueInventario inventario,
            Registro registro,
            MetricaProducto metrica) {
        Object valorNombre = inventario.valorDe(registro, "name");
        String nombre = valorNombre instanceof String texto ? texto.trim() : "";
        BigDecimal precio = positivo(inventario.valorDe(registro, "price"));
        BigDecimal costo = positivo(inventario.valorDe(registro, "cost"));
        if (nombre.isBlank() || precio == null || costo == null) {
            return null;
        }
        BigDecimal stock = numero(inventario.valorDe(registro, "quantity"));
        BigDecimal vendidas = metrica == null ? BigDecimal.ZERO : metrica.unidadesPeriodo();
        return new Producto(registro.getId(), nombre, precio, costo,
                stock == null ? BigDecimal.ZERO : stock, vendidas);
    }

    private static void agregar(
            List<PasoObjetivo> pasos,
            Set<RegistroId> usados,
            TipoPasoObjetivo tipo,
            Producto producto,
            AnalisisZona zona) {
        usados.add(producto.id());
        pasos.add(new PasoObjetivo(
                tipo,
                producto.nombre(),
                producto.precio(),
                producto.costo(),
                producto.stock(),
                producto.vendidas(),
                precioZona(producto.nombre(), zona)));
    }

    private static PrecioZona precioZona(String nombre, AnalisisZona zona) {
        AgregadoZonaProducto producto = zona.productoConRango(nombre).orElse(null);
        return producto == null
                ? null
                : new PrecioZona(producto.precioMinimo(), producto.precioPromedio(), producto.precioMaximo());
    }

    private static BigDecimal numero(Object valor) {
        return valor instanceof Number numero ? new BigDecimal(numero.toString()) : null;
    }

    private static BigDecimal positivo(Object valor) {
        BigDecimal numero = numero(valor);
        return numero != null && numero.signum() > 0 ? numero : null;
    }

    private record Producto(
            RegistroId id,
            String nombre,
            BigDecimal precio,
            BigDecimal costo,
            BigDecimal stock,
            BigDecimal vendidas) {

        boolean tieneVentas() {
            return vendidas.signum() > 0;
        }

        boolean tieneMargenBajo() {
            return ganancia().compareTo(precio.multiply(MARGEN_BAJO)) < 0;
        }

        boolean estaParado() {
            return vendidas.signum() == 0 && stock.signum() > 0;
        }

        boolean admiteRebaja() {
            return ganancia().compareTo(pasoDePrecio().multiply(BigDecimal.valueOf(2))) >= 0;
        }

        BigDecimal capital() {
            return costo.multiply(stock);
        }

        BigDecimal ganancia() {
            return precio.subtract(costo);
        }

        BigDecimal pasoDePrecio() {
            return precio.compareTo(PRECIO_PASO_GRANDE) >= 0 ? PASO_GRANDE : PASO_CHICO;
        }

        String orden() {
            return nombre.toLowerCase(Locale.ROOT) + "|" + id;
        }
    }
}
