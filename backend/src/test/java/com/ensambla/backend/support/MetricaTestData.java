package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.metrica.AgrupacionMetrica;
import com.ensambla.backend.domain.model.metrica.CalculoMetrica;
import com.ensambla.backend.domain.model.metrica.DefinicionMetrica;
import com.ensambla.backend.domain.model.metrica.AutoriaMetrica;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.metrica.Metrica;
import com.ensambla.backend.domain.model.metrica.MetricaId;
import com.ensambla.backend.domain.model.metrica.PeriodoMetrica;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.BloqueGastos;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.infrastructure.repository.entity.MetricaEntity;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

/** Un cuaderno de almacén para probar las métricas: inventario, ventas (con fiado) y gastos. */
public final class MetricaTestData {

    public static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    public static final ProyectoId PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000001");
    public static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    public static final MetricaId METRICA_ID = MetricaId.de("e0000000-0000-4000-8000-000000000001");
    public static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-11T12:00:00-03:00");
    public static final LocalDate HOY = LocalDate.of(2026, 11, 11);
    public static final RegistroId PAN_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    public static final RegistroId LECHE_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");
    public static final RegistroId SAL_ID = RegistroId.de("d0000000-0000-4000-8000-000000000003");

    private static final BloqueId INVENTARIO_ID = BloqueId.de("b0000000-0000-4000-8000-000000000011");
    private static final BloqueId VENTAS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000012");
    private static final BloqueId GASTOS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000013");

    private MetricaTestData() {
    }

    public static Negocio negocio(RubroNegocio rubro) {
        return Negocio.builder().id(NEGOCIO_ID).nombre("Almacén de Ana").rubro(rubro).build();
    }

    public static Proyecto proyecto() {
        return Proyecto.builder().id(PROYECTO_ID).negocioId(NEGOCIO_ID).nombre("Almacén").build();
    }

    /** Una métrica ya guardada, sin id nuevo: es la {@link #METRICA_ID}. */
    public static Metrica metrica(DefinicionMetrica definicion, int orden) {
        return metrica(METRICA_ID, definicion, orden);
    }

    public static Metrica metrica(MetricaId id, DefinicionMetrica definicion, int orden) {
        return Metrica.builder()
                .id(id)
                .negocioId(NEGOCIO_ID)
                .proyectoId(PROYECTO_ID)
                .definicion(definicion)
                .orden(orden)
                .autoria(AutoriaMetrica.SUGERIDA)
                .creadoEn(AHORA)
                .build();
    }

    public static final MetricaId METRICA_VENTAS_ID = MetricaId.de("e0000000-0000-4000-8000-000000000011");
    public static final MetricaId METRICA_GASTOS_ID = MetricaId.de("e0000000-0000-4000-8000-000000000012");
    public static final MetricaId METRICA_STOCK_ID = MetricaId.de("e0000000-0000-4000-8000-000000000013");

    /** Un panel de tres, en este orden: las ventas del mes, los gastos del mes y el stock. */
    public static List<Metrica> unPanelDeVentasGastosYStock() {
        return List.of(
                metrica(METRICA_VENTAS_ID, definicion(FuenteMetrica.VENTAS, CalculoMetrica.SUMA,
                        AgrupacionMetrica.NINGUNA, PeriodoMetrica.MES), 0),
                metrica(METRICA_GASTOS_ID, definicion(FuenteMetrica.GASTOS, CalculoMetrica.SUMA,
                        AgrupacionMetrica.NINGUNA, PeriodoMetrica.MES), 1),
                metrica(METRICA_STOCK_ID, definicion(FuenteMetrica.INVENTARIO, CalculoMetrica.UNIDADES,
                        AgrupacionMetrica.PRODUCTO, PeriodoMetrica.TODO), 2));
    }

    /** Un panel con el máximo de métricas por cuaderno, todas contando las ventas de hoy. */
    public static Metrica[] unPanelLleno() {
        DefinicionMetrica cuantasVentasHoy = definicion(FuenteMetrica.VENTAS, CalculoMetrica.CONTEO,
                AgrupacionMetrica.NINGUNA, PeriodoMetrica.HOY);
        return IntStream.range(0, Metrica.MAXIMO_POR_CUADERNO)
                .mapToObj(lugar -> metrica(cuantasVentasHoy, lugar))
                .toArray(Metrica[]::new);
    }

    public static DefinicionMetrica definicion(
            FuenteMetrica fuente, CalculoMetrica calculo, AgrupacionMetrica agrupar, PeriodoMetrica periodo,
            boolean soloFiado) {
        return new DefinicionMetrica("Mi métrica", fuente, calculo, agrupar, periodo, soloFiado);
    }

    public static DefinicionMetrica definicion(
            FuenteMetrica fuente, CalculoMetrica calculo, AgrupacionMetrica agrupar, PeriodoMetrica periodo) {
        return definicion(fuente, calculo, agrupar, periodo, false);
    }

    /** La fila guardada de "Lo que te deben" (ventas fiadas por cliente, todo lo cargado), en el lugar 3. */
    public static MetricaEntity laFilaDeLoQueTeDeben() {
        MetricaEntity fila = laFilaDeLoQueTeDebenEn(NEGOCIO_ID.valor(), PROYECTO_ID.valor());
        fila.setId(METRICA_ID.valor());
        return fila;
    }

    /** La fila de "Lo que te deben", todavía sin id, en el cuaderno y el negocio dados. */
    public static MetricaEntity laFilaDeLoQueTeDebenEn(UUID negocioId, UUID proyectoId) {
        return MetricaEntity.builder()
                .negocioId(negocioId)
                .proyectoId(proyectoId)
                .nombre("Lo que te deben")
                .fuente(FuenteMetrica.VENTAS)
                .calculo(CalculoMetrica.SUMA)
                .agrupar(AgrupacionMetrica.CLIENTE)
                .periodo(PeriodoMetrica.TODO)
                .soloFiado(true)
                .orden(3)
                .autoria(AutoriaMetrica.PROPIA)
                .creadoEn(AHORA)
                .build();
    }

    /** La fila guardada de las ventas del mes, con este id y en este lugar del panel. */
    public static MetricaEntity laFilaDeLasVentasDelMes(UUID id, int orden) {
        return MetricaEntity.builder()
                .id(id)
                .negocioId(NEGOCIO_ID.valor())
                .proyectoId(PROYECTO_ID.valor())
                .nombre("Ventas")
                .fuente(FuenteMetrica.VENTAS)
                .calculo(CalculoMetrica.SUMA)
                .agrupar(AgrupacionMetrica.NINGUNA)
                .periodo(PeriodoMetrica.MES)
                .orden(orden)
                .autoria(AutoriaMetrica.PROPIA)
                .creadoEn(AHORA)
                .build();
    }

    /** Una fila sugerida de las ventas del mes, todavía sin id, con este nombre y en este lugar del panel. */
    public static MetricaEntity unaFilaSugeridaEn(UUID negocioId, UUID proyectoId, String nombre, int orden) {
        return MetricaEntity.builder()
                .negocioId(negocioId)
                .proyectoId(proyectoId)
                .nombre(nombre)
                .fuente(FuenteMetrica.VENTAS)
                .calculo(CalculoMetrica.SUMA)
                .agrupar(AgrupacionMetrica.NINGUNA)
                .periodo(PeriodoMetrica.MES)
                .orden(orden)
                .autoria(AutoriaMetrica.SUGERIDA)
                .creadoEn(AHORA)
                .build();
    }

    /** Una fila de gastos del mes que todavía no tiene ids. */
    public static MetricaEntity unaFilaSinIds() {
        return MetricaEntity.builder()
                .fuente(FuenteMetrica.GASTOS)
                .calculo(CalculoMetrica.SUMA)
                .agrupar(AgrupacionMetrica.NINGUNA)
                .periodo(PeriodoMetrica.MES)
                .build();
    }

    /** Pan (10 u. a $100), Leche (5 u. a $200) y Sal (4 u. sin costo). */
    public static BloqueInventario inventario() {
        Bloque bloque = bloque(INVENTARIO_ID, TipoBloque.INVENTARIO, "name", "quantity", "cost");
        return new BloqueInventario(bloque, null, porClave(bloque), List.of(
                registro(INVENTARIO_ID, PAN_ID, bloque, "Pan", 10, 100),
                registro(INVENTARIO_ID, LECHE_ID, bloque, "Leche", 5, 200),
                registro(INVENTARIO_ID, SAL_ID, bloque, "Sal", 4, null)));
    }

    /** Un inventario cuyos productos no tienen cantidad cargada. */
    public static BloqueInventario inventarioSinCantidad() {
        Bloque bloque = bloque(INVENTARIO_ID, TipoBloque.INVENTARIO, "name", "quantity", "cost");
        return new BloqueInventario(bloque, null, porClave(bloque), List.of(
                registro(INVENTARIO_ID, PAN_ID, bloque, "Pan", null, 100)));
    }

    public static BloqueVentas ventas(Registro... registros) {
        Bloque bloque = bloqueDeVentas();
        return new BloqueVentas(bloque, porClave(bloque), List.of(registros));
    }

    /** Una venta; los valores nulos no se cargan. */
    public static Registro venta(
            RegistroId producto, Object cantidad, Object precio, String fecha, String cliente, Boolean fiado) {
        Bloque bloque = bloqueDeVentas();
        Map<String, Object> valores = new LinkedHashMap<>();
        poner(valores, bloque, "product", producto == null ? null : producto.toString());
        poner(valores, bloque, "quantity", cantidad);
        poner(valores, bloque, "price", precio);
        poner(valores, bloque, "date", fecha);
        poner(valores, bloque, "customer", cliente);
        poner(valores, bloque, "credit", fiado);
        return Registro.builder().id(RegistroId.nuevo()).negocioId(NEGOCIO_ID).bloqueId(VENTAS_ID).valores(valores)
                .build();
    }

    public static BloqueGastos gastos(Registro... registros) {
        Bloque bloque = bloqueDeGastos();
        return new BloqueGastos(bloque, porClave(bloque), List.of(registros));
    }

    public static Registro gasto(Object total, Object fecha, String categoria) {
        Bloque bloque = bloqueDeGastos();
        Map<String, Object> valores = new LinkedHashMap<>();
        poner(valores, bloque, "total", total);
        poner(valores, bloque, "date", fecha);
        poner(valores, bloque, "category", categoria);
        return Registro.builder().id(RegistroId.nuevo()).negocioId(NEGOCIO_ID).bloqueId(GASTOS_ID).valores(valores)
                .build();
    }

    /** Una tabla de gastos a la que le falta el importe. */
    public static BloqueGastos gastosSinImporte() {
        Bloque bloque = bloque(GASTOS_ID, TipoBloque.GASTOS, "name", "date");
        return new BloqueGastos(bloque, porClave(bloque), List.of());
    }

    public static DatosDeProyecto datos(
            List<BloqueInventario> inventarios, List<BloqueVentas> ventas, List<BloqueGastos> gastos,
            Map<String, String> clientes) {
        return new DatosDeProyecto("Almacén", inventarios, ventas, gastos, clientes);
    }

    public static DatosDeProyecto datos(
            List<BloqueInventario> inventarios, List<BloqueVentas> ventas, List<BloqueGastos> gastos) {
        return datos(inventarios, ventas, gastos, Map.of());
    }

    /**
     * Un almacén con todo: ventas de Pan $300 hoy (fiado, Ana), Leche $250 ayer (Beto), Pan $450 el 4/11 (sin
     * cliente), Sal sin precio hoy, una venta ilegible y Pan $150 de septiembre (fiado, Ana); gastos con importes y
     * fechas dudosas; y el inventario de {@link #inventario()}.
     */
    public static DatosDeProyecto unAlmacenConVentasYGastos() {
        BloqueVentas ventas = ventas(
                venta(PAN_ID, 2, 150, "2026-11-11", "Ana", true),
                venta(LECHE_ID, 1, 250, "2026-11-10", "Beto", false),
                venta(PAN_ID, 3, 150, "2026-11-04", null, false),
                venta(SAL_ID, 1, null, "2026-11-11", "Ana", null),
                venta(null, 1, 100, "2026-11-11", "Ana", false),
                venta(PAN_ID, 1, 150, "2026-09-01", "Ana", true));
        BloqueGastos gastos = gastos(
                gasto(1000, "2026-11-10", "Alquiler"),
                gasto(200, "2026-11-11", " "),
                gasto(null, "2026-11-09", "Luz"),
                gasto("abc", "2026-11-08", "Luz"),
                gasto(50, "mal", "Luz"),
                gasto(25, 7, "Luz"));
        return datos(List.of(inventario()), List.of(ventas), List.of(gastos));
    }

    /** Un cuaderno sin ninguna tabla. */
    public static DatosDeProyecto unCuadernoVacio() {
        return datos(List.of(), List.of(), List.of());
    }

    /** Un cuaderno con el inventario de {@link #inventario()} y estas ventas. */
    public static DatosDeProyecto unAlmacenQueVendio(Registro... ventas) {
        return datos(List.of(inventario()), List.of(ventas(ventas)), List.of());
    }

    /** Doce ventas de Pan del 10/11, cada una a un cliente distinto: "cliente 1" paga $10 y "cliente 12", $120. */
    public static DatosDeProyecto unAlmacenConDoceClientes() {
        Registro[] ventas = IntStream.rangeClosed(1, 12)
                .mapToObj(numero -> venta(PAN_ID, 1, numero * 10, "2026-11-10", "cliente " + numero, false))
                .toArray(Registro[]::new);
        return unAlmacenQueVendio(ventas);
    }

    /** Un cuaderno con estas tablas de gastos y nada más. */
    public static DatosDeProyecto unCuadernoConGastos(BloqueGastos... gastos) {
        return datos(List.of(), List.of(), List.of(gastos));
    }

    /** Un inventario con un solo producto, la Sal, que no tiene costo. */
    public static DatosDeProyecto unInventarioSinCostos() {
        BloqueInventario completo = inventario();
        BloqueInventario soloLaSal = new BloqueInventario(completo.bloque(), null, completo.camposPorClave(),
                completo.registros().subList(2, 3));
        return datos(List.of(soloLaSal), List.of(), List.of());
    }

    /** Un inventario cuyos productos no tienen la cantidad cargada. */
    public static DatosDeProyecto unInventarioSinCantidades() {
        return datos(List.of(inventarioSinCantidad()), List.of(), List.of());
    }

    private static Bloque bloqueDeVentas() {
        return bloque(VENTAS_ID, TipoBloque.VENTAS, "product", "quantity", "price", "date", "customer", "credit");
    }

    private static Bloque bloqueDeGastos() {
        return bloque(GASTOS_ID, TipoBloque.GASTOS, "name", "date", "total", "category");
    }

    private static Bloque bloque(BloqueId id, TipoBloque tipo, String... claves) {
        List<Campo> campos = Arrays.stream(claves)
                .map(clave -> Campo.builder().id(campoId(id, clave)).negocioId(NEGOCIO_ID).bloqueId(id).clave(clave)
                        .build())
                .toList();
        return Bloque.builder().id(id).negocioId(NEGOCIO_ID).proyectoId(PROYECTO_ID).tipo(tipo).campos(campos)
                .build();
    }

    private static CampoId campoId(BloqueId bloqueId, String clave) {
        return CampoId.de(UUID.nameUUIDFromBytes((bloqueId + clave).getBytes(StandardCharsets.UTF_8)));
    }

    private static Map<String, Campo> porClave(Bloque bloque) {
        Map<String, Campo> porClave = new LinkedHashMap<>();
        bloque.getCampos().forEach(campo -> porClave.put(campo.getClave(), campo));
        return porClave;
    }

    private static Registro registro(
            BloqueId bloqueId, RegistroId id, Bloque bloque, String nombre, Object cantidad, Object costo) {
        Map<String, Object> valores = new LinkedHashMap<>();
        poner(valores, bloque, "name", nombre);
        poner(valores, bloque, "quantity", cantidad);
        poner(valores, bloque, "cost", costo);
        return Registro.builder().id(id).negocioId(NEGOCIO_ID).bloqueId(bloqueId).valores(valores).build();
    }

    private static void poner(Map<String, Object> valores, Bloque bloque, String clave, Object valor) {
        if (valor != null) {
            Campo campo = bloque.getCampos().stream().filter(c -> c.getClave().equals(clave)).findFirst().orElseThrow();
            valores.put(campo.getId().toString(), valor);
        }
    }
}
