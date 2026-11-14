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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        return Metrica.builder()
                .id(METRICA_ID)
                .negocioId(NEGOCIO_ID)
                .proyectoId(PROYECTO_ID)
                .definicion(definicion)
                .orden(orden)
                .autoria(AutoriaMetrica.SUGERIDA)
                .creadoEn(AHORA)
                .build();
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
