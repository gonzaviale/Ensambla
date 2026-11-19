package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.ImpactoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.BloqueGastos;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

/** Productos, ventas, gastos, rangos de zona y avisos de un almacén de prueba para armar resúmenes del negocio. */
public final class DatosDeResumenDePrueba {

    public static final UUID YERBA = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    public static final UUID FIDEOS = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    public static final UUID MERMELADA = UUID.fromString("d0000000-0000-4000-8000-000000000003");
    public static final UUID ACEITE = UUID.fromString("d0000000-0000-4000-8000-000000000004");
    public static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T15:00:00Z");

    private static final CampoId CAMPO_FIADO_ID = CampoId.de("f0000000-0000-4000-8000-000000000016");
    private static final BloqueId GASTOS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000004");
    private static final CampoId GASTO_NOMBRE_ID = CampoId.de("f0000000-0000-4000-8000-000000000021");
    private static final CampoId GASTO_FECHA_ID = CampoId.de("f0000000-0000-4000-8000-000000000022");
    private static final CampoId GASTO_TOTAL_ID = CampoId.de("f0000000-0000-4000-8000-000000000023");
    private static final CampoId GASTO_CATEGORIA_ID = CampoId.de("f0000000-0000-4000-8000-000000000024");

    private DatosDeResumenDePrueba() {
    }

    public static Registro yerba() {
        return unProducto(YERBA, "Yerba Nobleza Gaucha", 40, 3400, 2700, 10);
    }

    public static Registro fideos() {
        return unProducto(FIDEOS, "Fideos Luchetti", 0, 1700, null, 6);
    }

    public static Registro mermelada() {
        return unProducto(MERMELADA, "Mermelada BC", 20, 2600, 1800, 4);
    }

    public static Registro aceite() {
        return unProducto(ACEITE, "Aceite Cocinero", 3, 2800, null, 6);
    }

    public static Registro unProducto(String nombre, Object cantidad, Object precio, Object costo, Object minimo) {
        return unProducto(UUID.randomUUID(), nombre, cantidad, precio, costo, minimo);
    }

    public static Registro unProducto(UUID id, String nombre, Object cantidad, Object precio, Object costo,
                                      Object minimo) {
        Map<String, Object> valores = new LinkedHashMap<>();
        poner(valores, AvisoTestData.CAMPO_NOMBRE_ID, nombre);
        poner(valores, AvisoTestData.CAMPO_CANTIDAD_ID, cantidad);
        poner(valores, AvisoTestData.CAMPO_PRECIO_ID, precio);
        poner(valores, AvisoTestData.CAMPO_COSTO_ID, costo);
        poner(valores, AvisoTestData.CAMPO_MINIMO_ID, minimo);
        return Registro.builder().id(RegistroId.de(id)).negocioId(AvisoTestData.NEGOCIO_ID)
                .bloqueId(AvisoTestData.INVENTARIO_ID).valores(valores).build();
    }

    /** Productos numerados del 0 en adelante, todos con stock, precio y costo. */
    public static List<Registro> productosNumerados(int cantidad, Object stock, Object minimo) {
        return IntStream.range(0, cantidad)
                .mapToObj(i -> unProducto("Producto " + i, stock, 100, 50, minimo))
                .toList();
    }

    /** Productos numerados con precios que suben de a 100 desde 1000, y un rango de zona para cada uno. */
    public static List<Registro> productosCarosNumerados(int cantidad) {
        return IntStream.range(0, cantidad)
                .mapToObj(i -> unProducto("Producto " + i, 1, 1000 + i * 100, null, null))
                .toList();
    }

    public static List<AgregadoZonaProducto> rangosBaratosNumerados(int cantidad) {
        return IntStream.range(0, cantidad).mapToObj(i -> unRango("Producto " + i, "100", "200", 5)).toList();
    }

    /** Una venta de una unidad por producto: la del producto i vende i + 1 unidades. */
    public static List<Registro> ventasCrecientesDe(List<Registro> productos) {
        return IntStream.range(0, productos.size())
                .mapToObj(i -> unaVenta(productos.get(i).getId().valor(), i + 1, 100, "2026-10-10"))
                .toList();
    }

    public static List<Registro> ventasAClientesNumerados(int cantidad) {
        return IntStream.range(0, cantidad)
                .mapToObj(i -> unaVentaA(YERBA, 1, 3400, "2026-10-10", "Cliente " + i))
                .toList();
    }

    public static Registro unaVenta(UUID producto, int cantidad, Integer precio, String fecha) {
        return unaVentaCompleta(producto, cantidad, precio, fecha, null, false);
    }

    public static Registro unaVentaA(UUID producto, int cantidad, Integer precio, String fecha, String cliente) {
        return unaVentaCompleta(producto, cantidad, precio, fecha, cliente, false);
    }

    public static Registro unaVentaFiadaA(UUID producto, int cantidad, Integer precio, String fecha, String cliente) {
        return unaVentaCompleta(producto, cantidad, precio, fecha, cliente, true);
    }

    private static Registro unaVentaCompleta(UUID producto, int cantidad, Integer precio, String fecha, String cliente,
                                             boolean fiado) {
        Map<String, Object> valores = new LinkedHashMap<>();
        poner(valores, AvisoTestData.CAMPO_PRODUCTO_ID, producto.toString());
        poner(valores, AvisoTestData.CAMPO_VENTA_CANTIDAD_ID, cantidad);
        poner(valores, AvisoTestData.CAMPO_VENTA_PRECIO_ID, precio);
        poner(valores, AvisoTestData.CAMPO_FECHA_ID, fecha);
        poner(valores, AvisoTestData.CAMPO_CLIENTE_ID, cliente);
        poner(valores, CAMPO_FIADO_ID, fiado);
        return Registro.builder().id(RegistroId.nuevo()).negocioId(AvisoTestData.NEGOCIO_ID)
                .bloqueId(AvisoTestData.VENTAS_ID).valores(valores).build();
    }

    /** La tabla de ventas de prueba con el campo de fiado. */
    public static BloqueVentas ventasDe(List<Registro> ventas) {
        Bloque base = AvisoTestData.bloqueVentas();
        List<Campo> campos = new ArrayList<>(base.getCampos());
        campos.add(campo(CAMPO_FIADO_ID, AvisoTestData.VENTAS_ID, "credit"));
        Bloque bloque = Bloque.builder().id(base.getId()).negocioId(base.getNegocioId())
                .proyectoId(base.getProyectoId()).tipo(base.getTipo()).campos(campos).build();
        return new BloqueVentas(bloque, AvisoTestData.camposPorClave(bloque), ventas);
    }

    public static Registro unGasto(String nombre, String fecha, Integer total, String categoria) {
        Map<String, Object> valores = new LinkedHashMap<>();
        poner(valores, GASTO_NOMBRE_ID, nombre);
        poner(valores, GASTO_FECHA_ID, fecha);
        poner(valores, GASTO_TOTAL_ID, total);
        poner(valores, GASTO_CATEGORIA_ID, categoria);
        return Registro.builder().id(RegistroId.nuevo()).negocioId(AvisoTestData.NEGOCIO_ID).bloqueId(GASTOS_ID)
                .valores(valores).build();
    }

    public static BloqueGastos gastosDe(List<Registro> gastos) {
        Campo nombre = campo(GASTO_NOMBRE_ID, GASTOS_ID, "name");
        Campo fecha = campo(GASTO_FECHA_ID, GASTOS_ID, "date");
        Campo total = campo(GASTO_TOTAL_ID, GASTOS_ID, "total");
        Campo categoria = campo(GASTO_CATEGORIA_ID, GASTOS_ID, "category");
        return new BloqueGastos(bloqueDeGastos(List.of(nombre, fecha, total, categoria)),
                Map.of("name", nombre, "date", fecha, "total", total, "category", categoria), gastos);
    }

    public static BloqueGastos gastosSinImporte() {
        return new BloqueGastos(bloqueDeGastos(List.of()), Map.of(), List.of());
    }

    public static AgregadoZonaProducto unRango(String nombre, String minimo, String maximo, int comercios) {
        return new AgregadoZonaProducto(nombre, comercios, comercios, new BigDecimal(minimo), new BigDecimal(minimo),
                new BigDecimal(maximo), BigDecimal.TEN);
    }

    public static Aviso unAvisoPendiente(TipoAviso tipo) {
        return unAviso(tipo, EstadoAviso.PENDIENTE, null);
    }

    public static Aviso unAviso(TipoAviso tipo, EstadoAviso estado, OffsetDateTime resueltoEn) {
        return Aviso.builder().tipo(tipo).estado(estado).resueltoEn(resueltoEn).prioridad((short) 1)
                .datos(Map.of()).acciones(List.of()).build();
    }

    /** Un aviso aplicado hace tres días cuyo ingreso semanal subió 3500. */
    public static Aviso unAvisoAplicadoYMedido() {
        Aviso aviso = unAviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO, AHORA.minusDays(3));
        aviso.registrarImpacto(new ImpactoAviso(14, BigDecimal.ONE, BigDecimal.TWO, new BigDecimal("1000"),
                new BigDecimal("1500")), AHORA);
        return aviso;
    }

    private static Bloque bloqueDeGastos(List<Campo> campos) {
        return Bloque.builder().id(GASTOS_ID).negocioId(AvisoTestData.NEGOCIO_ID).proyectoId(AvisoTestData.PROYECTO_ID)
                .tipo(TipoBloque.GASTOS).campos(campos).build();
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
