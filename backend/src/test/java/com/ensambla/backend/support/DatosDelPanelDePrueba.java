package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.BloqueEntradas;
import com.ensambla.backend.domain.model.reporte.BloqueGastos;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.ensambla.backend.support.DatosDeResumenDePrueba.ACEITE;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.FIDEOS;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.YERBA;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.unProducto;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.unaVenta;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.unaVentaFiadaA;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.ventasDe;

/** Almacenes de prueba para el panel de la home: ventas, gastos y compras de mercadería de setiembre y octubre. */
public final class DatosDelPanelDePrueba {

    public static final UUID DESCONOCIDO = UUID.fromString("d0000000-0000-4000-8000-000000000009");

    private static final BloqueId GASTOS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000004");
    private static final CampoId GASTO_FECHA_ID = CampoId.de("f0000000-0000-4000-8000-000000000022");
    private static final CampoId GASTO_TOTAL_ID = CampoId.de("f0000000-0000-4000-8000-000000000023");
    private static final CampoId GASTO_CATEGORIA_ID = CampoId.de("f0000000-0000-4000-8000-000000000024");
    private static final BloqueId ENTRADAS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final CampoId ENTRADA_PRODUCTO_ID = CampoId.de("f0000000-0000-4000-8000-000000000041");
    private static final CampoId ENTRADA_CANTIDAD_ID = CampoId.de("f0000000-0000-4000-8000-000000000042");
    private static final CampoId ENTRADA_FECHA_ID = CampoId.de("f0000000-0000-4000-8000-000000000043");
    private static final Integer SIN_PRECIO = null;

    private DatosDelPanelDePrueba() {
    }

    /** Ventas en tres semanas (una a fiado, una sin precio), gastos sin categoría y compras en la tabla de entradas. */
    public static DatosDeProyecto unAlmacenCompleto() {
        List<Registro> ventas = List.of(
                unaVenta(YERBA, 10, 3400, "2026-09-22"),
                unaVentaFiadaA(YERBA, 5, 3400, "2026-10-05", null),
                unaVenta(FIDEOS, 2, 1700, "2026-10-12"),
                unaVenta(YERBA, 2, SIN_PRECIO, "2026-10-13"),
                unaVenta(YERBA, 5, 3400, "2026-09-10"),
                unaVenta(YERBA, 100, 3400, "2026-08-01"));
        List<Registro> gastos = List.of(
                unGasto("2026-09-23", 5000), unGasto("2026-10-12", 1500), unGasto("2026-08-01", 9999));
        List<Registro> entradas = List.of(
                unaEntrada(YERBA.toString(), 5, "2026-09-24"), unaEntrada(YERBA.toString(), 5, "2026-08-01"));
        return new DatosDeProyecto("Almacén", List.of(AvisoTestData.inventarioConPrecios(yerba(), fideos(), aceite())),
                List.of(ventasDe(ventas)), List.of(gastosDe(gastos)), Map.of(), List.of(entradasDe(entradas)));
    }

    public static DatosDeProyecto unAlmacenQueVendio(Registro producto, Registro venta) {
        return new DatosDeProyecto("Almacén", List.of(AvisoTestData.inventarioConPrecios(producto)),
                List.of(ventasDe(List.of(venta))), List.of());
    }

    public static DatosDeProyecto unAlmacenConGastos(List<Registro> gastos) {
        return new DatosDeProyecto("Almacén", List.of(AvisoTestData.inventarioConPrecios(yerba())),
                List.of(ventasDe(List.of(unaVenta(YERBA, 10, 3400, "2026-09-22")))), List.of(gastosDe(gastos)));
    }

    public static DatosDeProyecto unAlmacenConGastosYEntradas(List<Registro> gastos) {
        return new DatosDeProyecto("Almacén", List.of(AvisoTestData.inventarioConPrecios(yerba(), fideos(), aceite())),
                List.of(ventasDe(List.of(unaVenta(YERBA, 10, 3400, "2026-09-22"), unaVenta(FIDEOS, 2, 1700, "2026-10-12")))),
                List.of(gastosDe(gastos)), Map.of(),
                List.of(entradasDe(List.of(unaEntrada(YERBA.toString(), 5, "2026-09-24")))));
    }

    public static DatosDeProyecto unAlmacenConEntradas(List<Registro> entradas) {
        return new DatosDeProyecto("Almacén", List.of(AvisoTestData.inventarioConPrecios(yerba(), fideos(), aceite())),
                List.of(ventasDe(List.of(unaVenta(YERBA, 10, 3400, "2026-09-22")))), List.of(), Map.of(),
                List.of(entradasDe(entradas)));
    }

    public static DatosDeProyecto unAlmacenSinTablas() {
        return new DatosDeProyecto("Almacén", List.of(), List.of(), List.of());
    }

    public static Registro yerba() {
        return unProducto(YERBA, "Yerba Nobleza Gaucha", 40, 3400, 2700, null);
    }

    public static Registro fideos() {
        return unProducto(FIDEOS, "Fideos Luchetti", 0, 1700, null, null);
    }

    /** Su costo es cero: tampoco sirve para valuar una compra. */
    public static Registro aceite() {
        return unProducto(ACEITE, "Aceite Cocinero", 3, 2800, 0, null);
    }

    public static Registro unGasto(String fecha, Integer total) {
        return unGasto(fecha, total, null);
    }

    public static Registro unGasto(String fecha, Integer total, String categoria) {
        Map<String, Object> valores = new LinkedHashMap<>();
        poner(valores, GASTO_FECHA_ID, fecha);
        poner(valores, GASTO_TOTAL_ID, total);
        poner(valores, GASTO_CATEGORIA_ID, categoria);
        return Registro.builder().id(RegistroId.nuevo()).negocioId(AvisoTestData.NEGOCIO_ID).bloqueId(GASTOS_ID)
                .valores(valores).build();
    }

    public static Registro unaEntrada(String producto, Integer cantidad, String fecha) {
        Map<String, Object> valores = new LinkedHashMap<>();
        poner(valores, ENTRADA_PRODUCTO_ID, producto);
        poner(valores, ENTRADA_CANTIDAD_ID, cantidad);
        poner(valores, ENTRADA_FECHA_ID, fecha);
        return Registro.builder().id(RegistroId.nuevo()).negocioId(AvisoTestData.NEGOCIO_ID).bloqueId(ENTRADAS_ID)
                .valores(valores).build();
    }

    public static BloqueGastos gastosDe(List<Registro> gastos) {
        Campo fecha = campo(GASTO_FECHA_ID, GASTOS_ID, "date");
        Campo total = campo(GASTO_TOTAL_ID, GASTOS_ID, "total");
        Campo categoria = campo(GASTO_CATEGORIA_ID, GASTOS_ID, "category");
        Bloque bloque = Bloque.builder().id(GASTOS_ID).negocioId(AvisoTestData.NEGOCIO_ID)
                .proyectoId(AvisoTestData.PROYECTO_ID).tipo(TipoBloque.GASTOS)
                .campos(List.of(fecha, total, categoria)).build();
        return new BloqueGastos(bloque, Map.of("date", fecha, "total", total, "category", categoria), gastos);
    }

    private static BloqueEntradas entradasDe(List<Registro> entradas) {
        Campo producto = campo(ENTRADA_PRODUCTO_ID, ENTRADAS_ID, "product");
        Campo cantidad = campo(ENTRADA_CANTIDAD_ID, ENTRADAS_ID, "quantity");
        Campo fecha = campo(ENTRADA_FECHA_ID, ENTRADAS_ID, "date");
        Bloque bloque = Bloque.builder().id(ENTRADAS_ID).negocioId(AvisoTestData.NEGOCIO_ID)
                .proyectoId(AvisoTestData.PROYECTO_ID).tipo(TipoBloque.ENTRADAS)
                .campos(List.of(producto, cantidad, fecha)).build();
        return new BloqueEntradas(bloque, Map.of("product", producto, "quantity", cantidad, "date", fecha), entradas);
    }

    private static Campo campo(CampoId id, BloqueId bloqueId, String clave) {
        return Campo.builder().id(id).negocioId(AvisoTestData.NEGOCIO_ID).bloqueId(bloqueId).clave(clave).build();
    }

    private static void poner(Map<String, Object> valores, CampoId campoId, Object valor) {
        if (valor != null) {
            valores.put(campoId.toString(), valor);
        }
    }
}
